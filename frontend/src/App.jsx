import { useEffect, useMemo, useRef, useState } from "react";
import { apiFetch, apiUrl } from "./api/apiClient";
import { getDashboardStats } from "./api/dashboardApi";

import {
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
} from "recharts";

import Attendance from "./Attendance";
import Leave from "./Leave";
import Performance from "./Performance";
import Payroll from "./Payroll";
import "./App.css";
import "./ProfessionalUI.css";
import Recruitment from "./Recruitment";
import JobApplications from "./JobApplications";
import WorkforceIntelligence from "./WorkforceIntelligence";
import Reports from "./Reports";
import Notifications from "./Notifications";
import EmployeeWorkspace from "./EmployeeWorkspace";
import ManagerAttendance from "./ManagerAttendance";

import Login from "./Login";
import { clearAuthSession, loadAuthSession, saveAuthSession } from "./api/authSession";
import PublicCareers from "./PublicCareers";
const HEALTH_API = apiUrl("/health");

const emptyEmployee = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  department: "",
  jobTitle: "",
  salary: "",
  joiningDate: "",
  employmentStatus: "ACTIVE",
  role: "EMPLOYEE",
};

const STATUS_META = [
  {
    key: "ACTIVE",
    label: "Active",
    hex: "#2563eb",
    dotClass: "blue",
  },
  {
    key: "INACTIVE",
    label: "Inactive",
    hex: "#f59e0b",
    dotClass: "orange",
  },
  {
    key: "ON LEAVE",
    label: "On Leave",
    hex: "#8b5cf6",
    dotClass: "purple",
  },
];

const DEPARTMENT_PALETTE = [
  "#2563eb",
  "#0891b2",
  "#7c3aed",
  "#db2777",
  "#059669",
  "#d97706",
  "#4f46e5",
  "#dc2626",
];

const AVATAR_PALETTE = [
  {
    bg: "rgba(37, 99, 235, 0.14)",
    color: "#2563eb",
  },
  {
    bg: "rgba(22, 163, 74, 0.14)",
    color: "#16a34a",
  },
  {
    bg: "rgba(124, 58, 237, 0.14)",
    color: "#7c3aed",
  },
  {
    bg: "rgba(234, 88, 12, 0.14)",
    color: "#ea580c",
  },
  {
    bg: "rgba(219, 39, 119, 0.14)",
    color: "#db2777",
  },
  {
    bg: "rgba(8, 145, 178, 0.14)",
    color: "#0891b2",
  },
];

function normalizeStatus(status) {
  return String(status || "INACTIVE")
    .trim()
    .toUpperCase();
}

function formatSalary(value) {
  if (value === null || value === undefined || value === "") {
    return "?";
  }

  const numericValue = Number(value);

  if (Number.isNaN(numericValue)) {
    return String(value);
  }

  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(numericValue);
}

function formatDate(date) {
  if (!date) {
    return "?";
  }

  const parsed = new Date(date);

  if (Number.isNaN(parsed.getTime())) {
    return "?";
  }

  return parsed.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function tenureMonthsSince(dateStr) {
  if (!dateStr) {
    return null;
  }

  const joined = new Date(dateStr);

  if (Number.isNaN(joined.getTime())) {
    return null;
  }

  const now = new Date();

  let months =
    (now.getFullYear() - joined.getFullYear()) * 12 +
    (now.getMonth() - joined.getMonth());

  if (now.getDate() < joined.getDate()) {
    months -= 1;
  }

  return Math.max(0, months);
}

function formatTenure(months) {
  if (
    months === null ||
    months === undefined ||
    Number.isNaN(months)
  ) {
    return "-";
  }

  const total = Math.max(0, Math.round(months));

  if (total < 1) {
    return "Less than 1 mo";
  }

  if (total < 12) {
    return `${total} mo`;
  }

  const years = Math.floor(total / 12);
  const remainder = total % 12;

  return remainder > 0
    ? `${years}y ${remainder}m`
    : `${years}y`;
}

function initialsOf(first, last) {
  const value = `${first || ""}${last || ""}`
    .substring(0, 2)
    .toUpperCase();

  return value || "U";
}

function avatarStyle(seed) {
  const str = seed || "U";

  let hash = 0;

  for (let i = 0; i < str.length; i += 1) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }

  const palette =
    AVATAR_PALETTE[
      Math.abs(hash) % AVATAR_PALETTE.length
    ];

  return {
    background: palette.bg,
    color: palette.color,
  };
}

function colorForIndex(index) {
  return DEPARTMENT_PALETTE[
    index % DEPARTMENT_PALETTE.length
  ];
}

function validateForm(values) {
  const errors = {};

  if (!values.firstName.trim()) {
    errors.firstName = "First name is required.";
  }

  if (!values.lastName.trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!values.email.trim()) {
    errors.email = "Email is required.";
  } else if (
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
      values.email.trim()
    )
  ) {
    errors.email = "Enter a valid email address.";
  }

  if (
    values.salary !== "" &&
    Number(values.salary) < 0
  ) {
    errors.salary = "Salary can't be negative.";
  }

  return errors;
}

function useTheme() {
  const [theme, setTheme] = useState(() => {
    try {
      const saved = localStorage.getItem(
        "nexushr-theme"
      );

      if (saved === "light" || saved === "dark") {
        return saved;
      }
    } catch {}

    if (
      typeof window !== "undefined" &&
      window.matchMedia
    ) {
      return window.matchMedia(
        "(prefers-color-scheme: dark)"
      ).matches
        ? "dark"
        : "light";
    }

    return "light";
  });

  useEffect(() => {
    document.documentElement.setAttribute(
      "data-theme",
      theme
    );

    try {
      localStorage.setItem(
        "nexushr-theme",
        theme
      );
    } catch {}
  }, [theme]);

  return [theme, setTheme];
}

function useClickOutside(ref, handler) {
  useEffect(() => {
    function listener(event) {
      if (
        !ref.current ||
        ref.current.contains(event.target)
      ) {
        return;
      }

      handler();
    }

    document.addEventListener(
      "mousedown",
      listener
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        listener
      );
    };
  }, [ref, handler]);
}

function App() {
  const [theme, setTheme] = useTheme();

  const [user, setUser] = useState(() => loadAuthSession());
  const [authChecking, setAuthChecking] = useState(() => !loadAuthSession());

  useEffect(() => {
    if (user) {
      setAuthChecking(false);
      return undefined;
    }

    let active = true;

    // Restore a valid HttpOnly-cookie session when localStorage is missing.
    // This also makes refreshes resilient to older/stale frontend sessions.
    apiFetch("/auth/me")
      .then((profile) => {
        if (active && profile?.username) {
          setUser({
            ...profile,
            role: String(profile.role || "").startsWith("ROLE_")
              ? profile.role
              : `ROLE_${String(profile.role || "").toUpperCase()}`,
            cookieSession: true,
          });
        }
      })
      .catch(() => {
        // No valid server session: the login page is the correct next state.
      })
      .finally(() => {
        if (active) setAuthChecking(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const handleLogin = (loginData) => {
    const session = saveAuthSession(loginData);
    setUser(session);
  };

  const handleLogout = () => {
    fetch(apiUrl("/auth/logout"), {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: null }),
    }).catch(() => {});

    clearAuthSession();
    setUser(null);
  };

  if (typeof window !== "undefined" && window.location.pathname.startsWith("/careers")) {
    return <PublicCareers />;
  }

  if (authChecking) {
    return (
      <div className="auth-boot-screen" role="status" aria-live="polite">
        <div className="auth-boot-mark">NX</div>
        <strong>Restoring secure session…</strong>
      </div>
    );
  }

  if (!user) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <Dashboard
      user={user}
      onLogout={handleLogout}
      theme={theme}
      setTheme={setTheme}
    />
  );
}

function Dashboard({

  user,
  onLogout,
  theme,
  setTheme,
}) {

  const normalizedRole = String(
    user?.role || ""
  )
    .replace(/^ROLE_/, "")
    .toUpperCase();

  const isAdmin =
    normalizedRole === "ADMIN";

  const isHR =
    normalizedRole === "HR";

  const isManager =
    normalizedRole === "MANAGER";

  const isEmployee =
    normalizedRole === "EMPLOYEE";

  const canEditEmployees =
    isAdmin || isHR || isManager;

  // Permanent employee deletion remains an Admin-only action.
  const canDeleteEmployees =
    isAdmin;

  // Managers may edit existing employee records, but only Admin/HR can add employees.
  const canManageEmployees =
    isAdmin || isHR;

  const canViewEmployeeDirectory =
    isAdmin ||
    isHR ||
    isManager;

  const canManageAttendance =
    isAdmin ||
    isHR ||
    isManager;

  const canManageLeave =
    isAdmin ||
    isHR ||
    isManager;

  const canManagePayroll =
    isAdmin ||
    isHR;

  const canViewPayroll =
    isAdmin ||
    isHR ||
    isManager;

  const canViewPerformance =
    isAdmin ||
    isHR ||
    isManager;

  const canViewRecruitment =
    isAdmin ||
    isHR ||
    isManager;

  const canViewWorkforceIntelligence =
    isAdmin ||
    isHR ||
    isManager;

  const canViewReports =
    isAdmin ||
    isHR ||
    isManager;

  const canManageNotifications =
    isAdmin ||
    isHR ||
    isManager;

  const [currentPage, setCurrentPage] =
    useState(() =>
      isEmployee
        ? "employee-access"
        : "dashboard"
    );

  const [employees, setEmployees] =
    useState([]);

  const [dashboardStats, setDashboardStats] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [loadError, setLoadError] =
    useState(false);

  const [toasts, setToasts] =
    useState([]);

  const [search, setSearch] =
    useState("");

  const [debouncedSearch, setDebouncedSearch] =
    useState("");

  const [departmentFilter, setDepartmentFilter] =
    useState("ALL");

  const [statusFilter, setStatusFilter] =
    useState("ALL");

  const [sortConfig, setSortConfig] =
    useState({
      key: "name",
      direction: "asc",
    });

  const [page, setPage] = useState(1);

  const pageSize = 8;

  const [modalOpen, setModalOpen] =
    useState(false);

  const [viewModalOpen, setViewModalOpen] =
    useState(false);

  const [confirmTarget, setConfirmTarget] =
    useState(null);

  const [deletingId, setDeletingId] =
    useState(null);

  const [editingEmployee, setEditingEmployee] =
    useState(null);

  const [selectedEmployee, setSelectedEmployee] =
    useState(null);

  const [form, setForm] =
    useState(emptyEmployee);

  const [formErrors, setFormErrors] =
    useState({});

  const [saving, setSaving] = useState(false);

  const [mobileMenu, setMobileMenu] =
    useState(false);

  const [sidebarCollapsed, setSidebarCollapsed] =
    useState(false);

  const [backendStatus, setBackendStatus] =
    useState("Checking");

  const [lastChecked, setLastChecked] =
    useState(null);

  const [chartRange, setChartRange] =
    useState(6);

  const [topSearch, setTopSearch] =
    useState("");

  const [topSearchOpen, setTopSearchOpen] =
    useState(false);

  const [notifOpen, setNotifOpen] =
    useState(false);

  const [userMenuOpen, setUserMenuOpen] =
    useState(false);

  const firstFieldRef = useRef(null);
  const employeesSectionRef = useRef(null);
  const notifRef = useRef(null);
  const userMenuRef = useRef(null);
  const topSearchRef = useRef(null);

  useClickOutside(notifRef, () =>
    setNotifOpen(false)
  );

  useClickOutside(userMenuRef, () =>
    setUserMenuOpen(false)
  );

  useClickOutside(topSearchRef, () =>
    setTopSearchOpen(false)
  );

  useEffect(() => {
    if (!isEmployee) {
      loadEmployees();
      loadDashboardStats();
    } else {
      setLoading(false);
    }

    checkBackend();

    const interval = setInterval(
      checkBackend,
      30000
    );

    return () => clearInterval(interval);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 200);

    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    setPage(1);
  }, [
    debouncedSearch,
    departmentFilter,
    statusFilter,
  ]);

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key !== "Escape") {
        return;
      }

      if (confirmTarget) {
        setConfirmTarget(null);
      } else if (modalOpen) {
        setModalOpen(false);
      } else if (viewModalOpen) {
        setViewModalOpen(false);
      } else if (mobileMenu) {
        setMobileMenu(false);
      } else if (notifOpen) {
        setNotifOpen(false);
      } else if (userMenuOpen) {
        setUserMenuOpen(false);
      }
    }

    document.addEventListener(
      "keydown",
      handleKeyDown
    );

    return () =>
      document.removeEventListener(
        "keydown",
        handleKeyDown
      );
  }, [
    confirmTarget,
    modalOpen,
    viewModalOpen,
    mobileMenu,
    notifOpen,
    userMenuOpen,
  ]);

  useEffect(() => {
    if (!modalOpen) {
      return undefined;
    }

    const timer = setTimeout(() => {
      firstFieldRef.current?.focus();
    }, 60);

    return () => clearTimeout(timer);
  }, [modalOpen]);

  function pushToast(type, message) {
    const id = Date.now();

    setToasts((current) => [
      ...current,
      {
        id,
        type,
        message,
      },
    ]);

    setTimeout(() => {
      setToasts((current) =>
        current.filter(
          (toast) => toast.id !== id
        )
      );
    }, 4000);
  }

  function dismissToast(id) {
    setToasts((current) =>
      current.filter(
        (toast) => toast.id !== id
      )
    );
  }

  async function checkBackend() {
    try {
      const response = await fetch(HEALTH_API);

      setBackendStatus(
        response.ok ? "Online" : "Offline"
      );
    } catch {
      setBackendStatus("Offline");
    } finally {
      setLastChecked(new Date());
    }
  }

async function loadEmployees() {
  try {
    setLoading(true);
    setLoadError(false);

    const data = await apiFetch("/employees");

    setEmployees(
      Array.isArray(data) ? data : []
    );
  } catch (err) {
    console.error("Employee load error:", err);

    setLoadError(true);

    pushToast(
      "error",
      err.message || "Unable to load employees."
    );
  } finally {
    setLoading(false);
  }
}

async function loadDashboardStats() {
  try {
    const data = await getDashboardStats();
    setDashboardStats(data || null);
  } catch (err) {
    console.error("Dashboard stats load error:", err);
  }
}

function openAddEmployee() {
  setEditingEmployee(null);
  setForm(emptyEmployee);
  setFormErrors({});
  setModalOpen(true);
}

function openEditEmployee(employee) {
  setEditingEmployee(employee);
    setForm({
      firstName: employee.firstName || "",
      lastName: employee.lastName || "",
      email: employee.email || "",
      phone: employee.phone || "",
      department: employee.department || "-",
      jobTitle: employee.jobTitle || "-",
      salary:
        employee.salary === null ||
        employee.salary === undefined
          ? ""
          : employee.salary,
      joiningDate:
        employee.joiningDate || "",
      employmentStatus:
        employee.employmentStatus ||
        "ACTIVE",
      role: employee.role || "EMPLOYEE",
    });

    setFormErrors({});
    setModalOpen(true);
  }

  function openViewEmployee(employee) {
    setSelectedEmployee(employee);
    setViewModalOpen(true);
  }

  function handleFormChange(event) {
    const {
      name,
      value,
    } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setFormErrors((current) => ({
      ...current,
      [name]: "",
    }));
  }

  async function saveEmployee(event) {
    event.preventDefault();

    const errors = validateForm(form);

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setSaving(true);

      const payload = {
        ...form,
        salary:
          form.salary === ""
            ? null
            : Number(form.salary),
      };

      const endpoint = editingEmployee
        ? `/employees/${editingEmployee.id}`
        : "/employees";

      const method = editingEmployee
        ? "PUT"
        : "POST";

      await apiFetch(endpoint, {
        method,
        body: JSON.stringify(payload),
      });

      await loadEmployees();

      setModalOpen(false);

      pushToast(
        "success",
        editingEmployee
          ? "Employee updated successfully."
          : "Employee added successfully."
      );
    } catch (error) {
      pushToast(
        "error",
        error.message ||
          "Unable to save employee."
      );
    } finally {
      setSaving(false);
    }
  }

  function requestDelete(employee) {
    setConfirmTarget(employee);
  }

  async function handleConfirmDelete() {
    if (!confirmTarget) {
      return;
    }

    const employee = confirmTarget;

    try {
      setDeletingId(employee.id);

      await apiFetch(
        `/employees/${employee.id}`,
        {
          method: "DELETE",
        }
      );

      setConfirmTarget(null);

      await loadEmployees();

      pushToast(
        "success",
        `${employee.firstName} ${employee.lastName} was removed.`
      );
    } catch (error) {
      pushToast(
        "error",
        error.message ||
          "Unable to delete employee."
      );
    } finally {
      setDeletingId(null);
    }
  }

  function handleLogout() {
    clearAuthSession();
    onLogout();
  }

  function canAccessPage(pageName) {
    switch (pageName) {
      case "dashboard":
        return !isEmployee;

      case "employees":
        return canViewEmployeeDirectory;

      case "attendance":
        return canManageAttendance;

      case "manager-attendance":
        return isManager;

      case "leave":
        return canManageLeave;

      case "payroll":
        return canViewPayroll;

      case "performance":
        return canViewPerformance;

      case "applications":
      case "recruitment":
        return canViewRecruitment;

      case "workforce-intelligence":
        return canViewWorkforceIntelligence;

      case "reports":
        return canViewReports;

      case "notifications":
        return canManageNotifications;

      case "employee-access":
      case "employee-profile":
      case "employee-attendance":
        return isEmployee;

      case "employee-leave":
        return isEmployee || isManager;

      case "employee-payroll":
      case "employee-documents":
      case "employee-performance":
      case "employee-notifications":
        return isEmployee;

      default:
        return false;
    }
  }

  function navigate(pageName) {
    if (!canAccessPage(pageName)) {
      pushToast(
        "error",
        "You do not have permission to access this module."
      );
      return;
    }

    setCurrentPage(pageName);
    setMobileMenu(false);
    setNotifOpen(false);
    setUserMenuOpen(false);
  }

  function jumpToEmployee(employee) {
    setSearch(
      `${employee.firstName || ""} ${
        employee.lastName || ""
      }`.trim()
    );

    setCurrentPage("dashboard");

    setTimeout(() => {
      employeesSectionRef.current?.scrollIntoView(
        {
          behavior: "smooth",
          block: "start",
        }
      );
    }, 100);
  }

  function handleSort(key) {
    setSortConfig((current) => ({
      key,
      direction:
        current.key === key &&
        current.direction === "asc"
          ? "desc"
          : "asc",
    }));
  }

  function applyDepartmentFilter(
    department
  ) {
    setDepartmentFilter(department);
    setCurrentPage("dashboard");

    setTimeout(() => {
      employeesSectionRef.current?.scrollIntoView(
        {
          behavior: "smooth",
          block: "start",
        }
      );
    }, 100);
  }

  function applyStatusFilter(status) {
    setStatusFilter(status);
    setCurrentPage("dashboard");

    setTimeout(() => {
      employeesSectionRef.current?.scrollIntoView(
        {
          behavior: "smooth",
          block: "start",
        }
      );
    }, 100);
  }

  function handleBarClick(data) {
    if (data?.activePayload?.[0]?.payload?.name) {
      applyDepartmentFilter(
        data.activePayload[0].payload.name
      );
    }
  }

  function handlePieClick(data) {
    if (data?.name) {
      applyStatusFilter(
        normalizeStatus(data.name)
      );
    }
  }

  function clearAllFilters() {
    setSearch("");
    setDepartmentFilter("ALL");
    setStatusFilter("ALL");
    setPage(1);
  }

  function exportCsv() {
    if (filteredEmployees.length === 0) {
      pushToast(
        "info",
        "There are no employees to export."
      );
      return;
    }

    const headers = [
      "First Name",
      "Last Name",
      "Email",
      "Phone",
      "Department",
      "Job Title",
      "Salary",
      "Joining Date",
      "Status",
    ];

    const rows = filteredEmployees.map(
      (employee) => [
        employee.firstName || "",
        employee.lastName || "",
        employee.email || "",
        employee.phone || "",
        employee.department || "-",
        employee.jobTitle || "-",
        employee.salary ?? "",
        employee.joiningDate || "",
        employee.employmentStatus || "",
      ]
    );

    const csv = [
      headers,
      ...rows,
    ]
      .map((row) =>
        row
          .map((value) => {
            const text = String(value ?? "");

            return `"${text.replace(
              /"/g,
              '""'
            )}"`;
          })
          .join(",")
      )
      .join("\n");

    const blob = new Blob(
      [csv],
      {
        type: "text/csv;charset=utf-8;",
      }
    );

    const url =
      URL.createObjectURL(blob);

    const anchor =
      document.createElement("a");

    anchor.href = url;
    anchor.download =
      "nexushr-employees.csv";

    anchor.click();

    URL.revokeObjectURL(url);

    pushToast(
      "success",
      "Employee CSV exported successfully."
    );
  }

  const departments = useMemo(() => {
    return [
      ...new Set(
        employees
          .map(
            (employee) =>
              employee.department
          )
          .filter(Boolean)
      ),
    ].sort();
  }, [employees]);

  const totalEmployees =
    employees.length;

  const activeEmployees =
    employees.filter(
      (employee) =>
        normalizeStatus(
          employee.employmentStatus
        ) === "ACTIVE"
    ).length;

  const inactiveEmployees =
    employees.filter(
      (employee) =>
        normalizeStatus(
          employee.employmentStatus
        ) === "INACTIVE"
    ).length;

  const onLeaveEmployees =
    employees.filter(
      (employee) =>
        normalizeStatus(
          employee.employmentStatus
        ) === "ON LEAVE"
    ).length;

  const departmentsCount =
    departments.length;

  const activePercent =
    totalEmployees
      ? Math.round(
          (activeEmployees /
            totalEmployees) *
            100
        )
      : 0;

  const newHires = useMemo(() => {
    const current = new Date();

    return employees.filter((employee) => {
      if (!employee.joiningDate) {
        return false;
      }

      const joined = new Date(
        employee.joiningDate
      );

      if (Number.isNaN(joined.getTime())) {
        return false;
      }

      const months =
        (current.getFullYear() -
          joined.getFullYear()) *
          12 +
        (current.getMonth() -
          joined.getMonth());

      return months >= 0 && months < 6;
    }).length;
  }, [employees]);

  const averageTenure = useMemo(() => {
    const values = employees
      .map((employee) =>
        tenureMonthsSince(
          employee.joiningDate
        )
      )
      .filter(
        (value) =>
          value !== null &&
          !Number.isNaN(value)
      );

    if (!values.length) {
      return 0;
    }

    return Math.round(
      values.reduce(
        (sum, value) =>
          sum + value,
        0
      ) / values.length
    );
  }, [employees]);

  const departmentChartData =
    useMemo(() => {
      if (dashboardStats?.departmentStats) {
        return Object.entries(
          dashboardStats.departmentStats
        ).map(([department, count]) => ({
          name: department,
          employees: count,
        }));
      }

      return departments.map(
        (department) => ({
          name: department,
          employees:
            employees.filter(
              (employee) =>
                employee.department ===
                department
            ).length,
        })
      );
    }, [dashboardStats, employees, departments]);

  const statusChartData =
    useMemo(() => {
      return STATUS_META.map(
        (status) => ({
          name: status.label,
          value:
            employees.filter(
              (employee) =>
                normalizeStatus(
                  employee.employmentStatus
                ) === status.key
            ).length,
          color: status.hex,
        })
      ).filter(
        (item) => item.value > 0
      );
    }, [employees]);

  const trendData = useMemo(() => {
    const now = new Date();

    return Array.from(
      { length: chartRange },
      (_, index) => {
        const date = new Date(
          now.getFullYear(),
          now.getMonth() -
            (chartRange - 1 - index),
          1
        );

        const month =
          date.toLocaleDateString(
            "en-IN",
            {
              month: "short",
            }
          );

        const endOfMonth =
          new Date(
            date.getFullYear(),
            date.getMonth() + 1,
            0
          );

        const active =
          employees.filter((employee) => {
            const joined =
              employee.joiningDate
                ? new Date(
                    employee.joiningDate
                  )
                : null;

            if (
              !joined ||
              Number.isNaN(
                joined.getTime()
              )
            ) {
              return false;
            }

            return (
              joined <= endOfMonth &&
              normalizeStatus(
                employee.employmentStatus
              ) === "ACTIVE"
            );
          }).length;

        return {
          month,
          employees: active,
        };
      }
    );
  }, [employees, chartRange]);

  const filteredEmployees =
    useMemo(() => {
      const query =
        debouncedSearch
          .trim()
          .toLowerCase();

      const filtered =
        employees.filter(
          (employee) => {
            const name =
              `${employee.firstName || ""} ${
                employee.lastName || ""
              }`.toLowerCase();

            const department =
              String(
                employee.department || "-"
              ).toLowerCase();

            const email =
              String(
                employee.email || ""
              ).toLowerCase();

            const jobTitle =
              String(
                employee.jobTitle || "-"
              ).toLowerCase();

            const status =
              normalizeStatus(
                employee.employmentStatus
              );

            const matchesSearch =
              !query ||
              name.includes(query) ||
              department.includes(query) ||
              email.includes(query) ||
              jobTitle.includes(query);

            const matchesDepartment =
              departmentFilter === "ALL" ||
              employee.department ===
                departmentFilter;

            const matchesStatus =
              statusFilter === "ALL" ||
              status === statusFilter;

            return (
              matchesSearch &&
              matchesDepartment &&
              matchesStatus
            );
          }
        );

      return [...filtered].sort(
        (a, b) => {
          let first;
          let second;

          switch (sortConfig.key) {
            case "name":
              first =
                `${a.firstName || ""} ${
                  a.lastName || ""
                }`.toLowerCase();

              second =
                `${b.firstName || ""} ${
                  b.lastName || ""
                }`.toLowerCase();
              break;

            case "department":
              first =
                String(
                  a.department || ""
                ).toLowerCase();

              second =
                String(
                  b.department || ""
                ).toLowerCase();
              break;

            case "salary":
              first =
                Number(a.salary) || 0;

              second =
                Number(b.salary) || 0;
              break;

            case "joiningDate":
              first =
                a.joiningDate || "";

              second =
                b.joiningDate || "";
              break;

            case "status":
              first =
                normalizeStatus(
                  a.employmentStatus
                );

              second =
                normalizeStatus(
                  b.employmentStatus
                );
              break;

            default:
              first = "";
              second = "";
          }

          if (first < second) {
            return sortConfig.direction ===
              "asc"
              ? -1
              : 1;
          }

          if (first > second) {
            return sortConfig.direction ===
              "asc"
              ? 1
              : -1;
          }

          return 0;
        }
      );
    }, [
      employees,
      debouncedSearch,
      departmentFilter,
      statusFilter,
      sortConfig,
    ]);

  const totalPages =
    Math.max(
      1,
      Math.ceil(
        filteredEmployees.length /
          pageSize
      )
    );

  const currentEmployees =
    filteredEmployees.slice(
      (page - 1) * pageSize,
      page * pageSize
    );

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const pageTitle =
    currentPage === "employee-access"
      ? "Employee Workspace"
      : currentPage === "employee-profile"
      ? "My Profile"
      : currentPage === "employee-attendance"
      ? "My Attendance"
      : currentPage === "employee-leave"
      ? "My Leave"
      : currentPage === "employee-payroll"
      ? "My Payroll"
      : currentPage === "employee-documents"
      ? "My Documents"
      : currentPage === "employee-performance"
      ? "My Performance"
      : currentPage === "employee-notifications"
      ? "My Notifications"
      : currentPage === "manager-attendance"
      ? "My Attendance"
      : currentPage === "attendance"
      ? "Attendance"
      : currentPage === "leave"
      ? "Leave Management"
      : currentPage === "payroll"
      ? "Payroll Management"
      : currentPage === "performance"
      ? "Performance Management"
      : currentPage === "applications"
      ? "Job Applications"
      : currentPage === "recruitment"
      ? "Recruitment"
      : currentPage === "workforce-intelligence"
      ? "Workforce Intelligence"
      : currentPage === "reports"
      ? "Reports & Export"
      : currentPage === "notifications"
      ? "Notifications"
      : currentPage === "employees"
      ? "Employees"
      : "Dashboard";

  const pageDescription =
    currentPage === "employee-access"
      ? "Access your attendance, leave, payroll, performance and notifications securely from one workspace."
      : currentPage === "employee-profile"
      ? "View your employee profile and employment information."
      : currentPage === "employee-attendance"
      ? "Review date-wise attendance, check-in, check-out and working hours."
      : currentPage === "employee-leave"
      ? "View balances, request leave and track approval status."
      : currentPage === "employee-payroll"
      ? "Review salary details and download secure payslips."
      : currentPage === "employee-documents"
      ? "Upload, download and manage documents linked securely to your account."
      : currentPage === "employee-performance"
      ? "Review ratings, performance history and manager feedback."
      : currentPage === "employee-notifications"
      ? "Review alerts, reminders and HR announcements."
      : currentPage === "manager-attendance"
      ? "Check in, check out and review your own manager attendance history."
      : currentPage === "attendance"
      ? "Track employee attendance and daily workforce presence."
      : currentPage === "leave"
      ? "Manage employee leave requests and approvals."
      : currentPage === "payroll"
      ? "Manage salary processing, payroll records, and payslips."
      : currentPage === "performance"
      ? "Manage reviews, goals, ratings, 360° feedback, and performance trends."
      : currentPage === "applications"
      ? "Manage candidate applications and hiring workflow."
      : currentPage === "recruitment"
      ? "Manage recruitment and workforce hiring activities."
      : currentPage === "workforce-intelligence"
      ? "Analyze attrition risk, engagement, skill gaps, and workforce recommendations."
      : currentPage === "reports"
      ? "Review workforce summaries and export PDF and Excel reports."
      : currentPage === "notifications"
      ? "Manage employee notifications, reminders, approvals, and announcements."
      : currentPage === "employees"
      ? "Manage your organization's employee records."
      : "Welcome back. Here's what's happening across your workforce.";

  const displayName =
    user?.name ||
    user?.username ||
    user?.email ||
    "Admin";

  const signedInUsername =
    user?.username ||
    user?.email ||
    displayName;

  const notificationCount =
    loadError ||
    backendStatus === "Offline"
      ? 1
      : 0;

  return (
    <div
      className={`app-shell ${
        sidebarCollapsed
          ? "sidebar-is-collapsed"
          : ""
      }`}
    >
      <aside
        className={`sidebar ${
          mobileMenu
            ? "mobile-open"
            : ""
        }`}
      >
        <div className="sidebar-brand">
          <div className="brand-mark">
            <span>NX</span>
          </div>

          {!sidebarCollapsed && (
            <div className="brand-copy">
              <strong>NexusHR</strong>
              <span>Workforce Intelligence</span>
            </div>
          )}
        </div>

        <div className="sidebar-section">
          {!sidebarCollapsed && (
            <span className="sidebar-label">
              MAIN MENU
            </span>
          )}

          {isEmployee && (
            <>
              <button
                className={`nav-item ${currentPage === "employee-access" ? "active" : ""}`}
                onClick={() => navigate("employee-access")}
                title="Overview"
              >
                <IconDashboard width="19" height="19" />
                {!sidebarCollapsed && <span>Overview</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-profile" ? "active" : ""}`}
                onClick={() => navigate("employee-profile")}
                title="My Profile"
              >
                <IconUsers width="19" height="19" />
                {!sidebarCollapsed && <span>My Profile</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-attendance" ? "active" : ""}`}
                onClick={() => navigate("employee-attendance")}
                title="My Attendance"
              >
                <IconClock width="19" height="19" />
                {!sidebarCollapsed && <span>My Attendance</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-leave" ? "active" : ""}`}
                onClick={() => navigate("employee-leave")}
                title="My Leave"
              >
                <IconCalendar width="19" height="19" />
                {!sidebarCollapsed && <span>My Leave</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-payroll" ? "active" : ""}`}
                onClick={() => navigate("employee-payroll")}
                title="My Payroll"
              >
                <IconDownload width="19" height="19" />
                {!sidebarCollapsed && <span>My Payroll</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-documents" ? "active" : ""}`}
                onClick={() => navigate("employee-documents")}
                title="My Documents"
              >
                <IconDownload width="19" height="19" />
                {!sidebarCollapsed && <span>My Documents</span>}
              </button>
            </>
          )}

          {!isEmployee && (
          <button
            className={`nav-item ${
              currentPage === "dashboard"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("dashboard")
            }
            title="Dashboard"
          >
            <IconDashboard
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Dashboard</span>
            )}
          </button>
          )}

          {canViewEmployeeDirectory && (
          <button
            className={`nav-item ${
              currentPage === "employees"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("employees")
            }
            title="Employees"
          >
            <IconUsers
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Employees</span>
            )}
          </button>
          )}

          {isManager && (
          <button
            className={`nav-item ${currentPage === "manager-attendance" ? "active" : ""}`}
            onClick={() => navigate("manager-attendance")}
            title="My Attendance"
          >
            <IconClock width="19" height="19" />
            {!sidebarCollapsed && <span>My Attendance</span>}
          </button>
          )}

          {isManager && (
          <button
            className={`nav-item ${currentPage === "employee-leave" ? "active" : ""}`}
            onClick={() => navigate("employee-leave")}
            title="My Leave Request"
          >
            <IconCalendar width="19" height="19" />
            {!sidebarCollapsed && <span>My Leave</span>}
          </button>
          )}

          {canManageAttendance && (
          <button
            className={`nav-item ${
              currentPage === "attendance"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("attendance")
            }
            title="Attendance"
          >
            <IconClock
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Attendance</span>
            )}
          </button>
          )}

          {canManageLeave && (
          <button
            className={`nav-item ${
              currentPage === "leave"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("leave")
            }
            title="Leave Management"
          >
            <IconCalendar
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Leave Management</span>
            )}
          </button>
          )}

{canViewPayroll && (
  <button
    className={`nav-item ${
      currentPage === "payroll" ? "active" : ""
    }`}
    onClick={() => navigate("payroll")}
    title="Payroll Management"
  >
    <IconCalendar
      width="19"
      height="19"
    />

    {!sidebarCollapsed && (
      <span>Payroll Management</span>
    )}
  </button>
)}

{canViewPerformance && (
  <button
    className={`nav-item ${
      currentPage === "performance" ? "active" : ""
    }`}
    onClick={() => navigate("performance")}
    title="Performance Management"
  >
    <IconCalendar
      width="19"
      height="19"
    />

    {!sidebarCollapsed && (
      <span>Performance Management</span>
    )}
  </button>
)}

{canViewRecruitment && (
<button
  className={`nav-item ${
    currentPage === "applications" ? "active" : ""
  }`}
  onClick={() => navigate("applications")}
  title="Job Applications"
>
  <IconCalendar
    width="19"
    height="19"
  />

  {!sidebarCollapsed && (
    <span>Job Applications</span>
  )}
</button>
)}

{canViewRecruitment && (
<button
  className={`nav-item ${
    currentPage === "recruitment" ? "active" : ""
  }`}
  onClick={() => navigate("recruitment")}
  title="Recruitment"
>
  <IconCalendar
    width="19"
    height="19"
  />

  {!sidebarCollapsed && (
    <span>Recruitment</span>
  )}
</button>
)}


        </div>

        <div className="sidebar-section">
          {!sidebarCollapsed && (
            <span className="sidebar-label">
              INSIGHTS
            </span>
          )}

          {isEmployee && (
            <>
              <button
                className={`nav-item ${currentPage === "employee-performance" ? "active" : ""}`}
                onClick={() => navigate("employee-performance")}
                title="My Performance"
              >
                <IconChart width="19" height="19" />
                {!sidebarCollapsed && <span>My Performance</span>}
              </button>

              <button
                className={`nav-item ${currentPage === "employee-notifications" ? "active" : ""}`}
                onClick={() => navigate("employee-notifications")}
                title="My Notifications"
              >
                <IconBell width="19" height="19" />
                {!sidebarCollapsed && <span>My Notifications</span>}
              </button>
            </>
          )}

          {canViewWorkforceIntelligence && (
          <button
            className={`nav-item ${
              currentPage === "workforce-intelligence"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("workforce-intelligence")
            }
            title="Workforce Intelligence"
          >
            <IconSparkles
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Workforce Intelligence</span>
            )}
          </button>
          )}

          {canViewReports && (
          <button
            className={`nav-item ${
              currentPage === "reports"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("reports")
            }
            title="Reports & Export"
          >
            <IconDownload
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Reports & Export</span>
            )}
          </button>
          )}

          {canManageNotifications && (
          <button
            className={`nav-item ${
              currentPage === "notifications"
                ? "active"
                : ""
            }`}
            onClick={() =>
              navigate("notifications")
            }
            title="Notifications"
          >
            <IconBell
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Notifications</span>
            )}
          </button>
          )}

          {!isEmployee && (
          <button
            className="nav-item"
            onClick={() => {
              navigate("dashboard");

              setTimeout(() => {
                document
                  .getElementById(
                    "analytics-section"
                  )
                  ?.scrollIntoView({
                    behavior: "smooth",
                  });
              }, 100);
            }}
            title="Analytics"
          >
            <IconChart
              width="19"
              height="19"
            />

            {!sidebarCollapsed && (
              <span>Analytics</span>
            )}
          </button>
          )}
        </div>

        <div className="sidebar-bottom">
          <div
            className={`system-status ${
              backendStatus ===
              "Online"
                ? "online"
                : backendStatus ===
                  "Offline"
                ? "offline"
                : "checking"
            }`}
          >
            <span className="status-pulse" />

            {!sidebarCollapsed && (
              <div>
                <strong>
                  System {backendStatus}
                </strong>

                <small>
                  {lastChecked
                    ? `Checked ${lastChecked.toLocaleTimeString(
                        "en-IN",
                        {
                          hour: "2-digit",
                          minute: "2-digit",
                        }
                      )}`
                    : "Checking server..."}
                </small>
              </div>
            )}
          </div>

          <button
            className="collapse-button"
            onClick={() =>
              setSidebarCollapsed(
                (current) =>
                  !current
              )
            }
            title={
              sidebarCollapsed
                ? "Expand sidebar"
                : "Collapse sidebar"
            }
          >
            {sidebarCollapsed ? (
              <IconChevronRight
                width="17"
                height="17"
              />
            ) : (
              <IconChevronLeft
                width="17"
                height="17"
              />
            )}
          </button>
        </div>
      </aside>

      {mobileMenu && (
        <div
          className="mobile-overlay"
          onClick={() =>
            setMobileMenu(false)
          }
        />
      )}

      <main className="main-content">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="mobile-menu-button"
              onClick={() =>
                setMobileMenu(
                  (current) =>
                    !current
                )
              }
            >
              <IconMenu
                width="20"
                height="20"
              />
            </button>

            <div>
              <h1>{pageTitle}</h1>
              <p>{pageDescription}</p>
            </div>
          </div>

          <div className="topbar-actions">
            <div
              className={`top-search ${
                topSearchOpen
                  ? "open"
                  : ""
              }`}
              ref={topSearchRef}
            >
              <button
                className="icon-button"
                onClick={() =>
                  setTopSearchOpen(
                    (current) =>
                      !current
                  )
                }
                title="Search"
              >
                <IconSearch
                  width="18"
                  height="18"
                />
              </button>

              {topSearchOpen && (
                <div className="top-search-box">
                  <IconSearch
                    width="16"
                    height="16"
                  />

                  <input
                    autoFocus
                    value={topSearch}
                    onChange={(event) =>
                      setTopSearch(
                        event.target.value
                      )
                    }
                    onKeyDown={(event) => {
                      if (
                        event.key ===
                        "Enter"
                      ) {
                        setSearch(
                          topSearch
                        );

                        navigate(
                          "dashboard"
                        );

                        setTopSearchOpen(
                          false
                        );
                      }
                    }}
                    placeholder="Search employees..."
                  />
                </div>
              )}
            </div>

            <button
              className="icon-button"
              onClick={() =>
                setTheme(
                  theme === "dark"
                    ? "light"
                    : "dark"
                )
              }
              title="Toggle theme"
            >
              {theme === "dark" ? (
                <IconSun
                  width="18"
                  height="18"
                />
              ) : (
                <IconMoon
                  width="18"
                  height="18"
                />
              )}
            </button>

            <div
              className="notification-wrapper"
              ref={notifRef}
            >
              <button
                className="icon-button notification-button"
                onClick={() =>
                  setNotifOpen(
                    (current) =>
                      !current
                  )
                }
                title="Notifications"
              >
                <IconBell
                  width="18"
                  height="18"
                />

                {notificationCount >
                  0 && (
                  <span className="notification-dot">
                    {notificationCount}
                  </span>
                )}
              </button>

              {notifOpen && (
                <div className="dropdown notification-dropdown">
                  <div className="dropdown-heading">
                    <strong>
                      Notifications
                    </strong>
                  </div>

                  {backendStatus ===
                  "Offline" ? (
                    <div className="notification-item warning">
                      <div className="notification-icon">
                        <IconAlert
                          width="17"
                          height="17"
                        />
                      </div>

                      <div>
                        <strong>
                          Backend offline
                        </strong>

                        <p>
                          Unable to connect
                          to the server.
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="notification-item">
                      <div className="notification-icon success">
                        <IconCheck
                          width="17"
                          height="17"
                        />
                      </div>

                      <div>
                        <strong>
                          All systems normal
                        </strong>

                        <p>
                          NexusHR is
                          connected to the
                          backend.
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            <div
              className="user-wrapper"
              ref={userMenuRef}
            >
              <button
                className="user-button"
                onClick={() =>
                  setUserMenuOpen(
                    (current) =>
                      !current
                  )
                }
              >
                <div
                  className="user-avatar"
                  style={avatarStyle(
                    displayName
                  )}
                >
                  {initialsOf(
                    displayName,
                    ""
                  )}
                </div>

                <div className="user-info">
                  <strong>
                    {displayName}
                  </strong>
                  <span>
                    @{signedInUsername} · {isAdmin
                      ? "Administrator"
                      : isHR
                      ? "HR Administrator"
                      : isManager
                      ? "Manager"
                      : "Employee"}
                  </span>
                </div>

                <IconChevronDown
                  width="16"
                  height="16"
                />
              </button>

              {userMenuOpen && (
                <div className="dropdown user-dropdown">
                  <div className="user-dropdown-header">
                    <div
                      className="user-avatar large"
                      style={avatarStyle(
                        displayName
                      )}
                    >
                      {initialsOf(
                        displayName,
                        ""
                      )}
                    </div>

                    <div>
                      <strong>
                        {displayName}
                      </strong>

                      <span>
                        @{signedInUsername}
                        {user?.email && user?.email !== signedInUsername
                          ? ` · ${user.email}`
                          : ""}
                      </span>
                    </div>
                  </div>

                  <button
                    className="dropdown-action"
                    onClick={() =>
                      setTheme(
                        theme ===
                          "dark"
                          ? "light"
                          : "dark"
                      )
                    }
                  >
                    {theme === "dark" ? (
                      <IconSun
                        width="16"
                        height="16"
                      />
                    ) : (
                      <IconMoon
                        width="16"
                        height="16"
                      />
                    )}

                    {theme === "dark"
                      ? "Light mode"
                      : "Dark mode"}
                  </button>

                  <button
                    className="dropdown-action danger"
                    onClick={
                      handleLogout
                    }
                  >
                    <IconLogout
                      width="16"
                      height="16"
                    />
                    Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {[
          "employee-access",
          "employee-profile",
          "employee-attendance",
          "employee-leave",
          "employee-payroll",
          "employee-documents",
          "employee-performance",
          "employee-notifications",
        ].includes(currentPage) && (
          <div className="page-container">
            <EmployeeWorkspace
              initialTab={
                currentPage === "employee-profile"
                  ? "profile"
                  : currentPage === "employee-attendance"
                  ? "attendance"
                  : currentPage === "employee-leave"
                  ? "leave"
                  : currentPage === "employee-payroll"
                  ? "payroll"
                  : currentPage === "employee-documents"
                  ? "documents"
                  : currentPage === "employee-performance"
                  ? "performance"
                  : currentPage === "employee-notifications"
                  ? "notifications"
                  : "overview"
              }
            />
          </div>
        )}

        {currentPage === "manager-attendance" && (
          <div className="page-container">
            <ManagerAttendance />
          </div>
        )}

        {currentPage ===
          "attendance" && (
          <div className="page-container">
            <Attendance
              employees={employees}
            />
          </div>
        )}

{currentPage === "leave" && (
  <div className="page-container">
    <Leave
      employees={employees}
    />
  </div>
)}

{currentPage === "payroll" && (
  <div className="page-container">
    <Payroll />
  </div>
)}

{currentPage === "performance" && (
  <div className="page-container">
    <Performance />
  </div>
)}

{currentPage === "notifications" && (
  <div className="page-container">
    <Notifications />
  </div>
)}

{currentPage === "reports" && (
  <div className="page-container">
    <Reports />
  </div>
)}

{currentPage === "workforce-intelligence" && (
  <div className="page-container">
    <WorkforceIntelligence />
  </div>
)}

{currentPage === "recruitment" && (
  <div className="page-container">
    <Recruitment />
  </div>
)}

{currentPage === "applications" && (
  <div className="page-container">
    <JobApplications />
  </div>
)}



        {currentPage === "employees" && (
          <div className="page-container">
            <section
              className="page-hero compact"
            >
              <div>
                <span className="eyebrow">
                  EMPLOYEE DIRECTORY
                </span>

                <h2>
                  Manage your workforce
                </h2>

                <p>
                  Add, update, search and
                  manage employee records
                  from one place.
                </p>
              </div>

              {canManageEmployees && (
              <button
                className="primary-button"
                onClick={
                  openAddEmployee
                }
              >
                <IconPlus
                  width="16"
                  height="16"
                />
                Add Employee
              </button>
              )}
            </section>


<EmployeeTable
  canEditEmployees={
    canEditEmployees
  }
  canDeleteEmployees={
    canDeleteEmployees
  }
  employees={
    currentEmployees
  }
  loading={
    loading
  }
  sortConfig={
    sortConfig
  }
  handleSort={
    handleSort
  }
  openViewEmployee={
    openViewEmployee
  }
  openEditEmployee={
    openEditEmployee
  }
  requestDelete={
    requestDelete
  }
  page={
    page
  }
  setPage={
    setPage
  }
  totalPages={
    totalPages
  }
  totalCount={
    filteredEmployees.length
  }
/>
          </div>
        )}

        {currentPage ===
          "dashboard" && (
          <div className="page-container">
            <section className="hero-card">
              <div className="hero-content">
                <div className="hero-eyebrow">
                  <span className="live-dot" />
                  LIVE WORKFORCE OVERVIEW
                </div>

                <h2>
                  Smarter HR starts here.
                </h2>

                <p>
                  Monitor your people,
                  understand workforce
                  trends, and manage
                  employee operations
                  from one intelligent
                  workspace.
                </p>

                <div className="hero-actions">
                  {canManageEmployees && (
                  <button
                    className="primary-button"
                    onClick={
                      openAddEmployee
                    }
                  >
                    <IconPlus
                      width="16"
                      height="16"
                    />
                    Add Employee
                  </button>
                  )}

                  <button
                    className="secondary-button"
                    onClick={() =>
                      navigate(
                        "employees"
                      )
                    }
                  >
                    <IconUsers
                      width="16"
                      height="16"
                    />
                    View Employees
                  </button>
                </div>
              </div>

              <div className="hero-visual">
                <div className="hero-orbit orbit-one" />
                <div className="hero-orbit orbit-two" />
                <div className="hero-orbit orbit-three" />

                <div className="hero-center">
                  <IconUsers
                    width="42"
                    height="42"
                  />

                  <strong>
                    {totalEmployees}
                  </strong>

                  <span>
                    Employees
                  </span>
                </div>
              </div>
            </section>

            <section className="stats-grid">
              <StatCard
                icon={
                  <IconUsers
                    width="20"
                    height="20"
                  />
                }
                title="Total Employees"
                value={
                  dashboardStats?.totalEmployees ?? totalEmployees
                }
                subtitle="Across all departments"
                accent="blue"
              />

              <StatCard
                icon={
                  <IconSparkles
                    width="20"
                    height="20"
                  />
                }
                title="New Hires"
                value={newHires}
                subtitle="Joined in last 6 months"
                accent="purple"
              />

              <StatCard
                icon={
                  <IconUserCheck
                    width="20"
                    height="20"
                  />
                }
                title="Active Employees"
                value={
                  dashboardStats?.activeEmployees ?? activeEmployees
                }
                subtitle={`${activePercent}% of workforce`}
                accent="green"
              />

              <StatCard
                icon={
                  <IconCalendar
                    width="20"
                    height="20"
                  />
                }
                title="On Leave"
                value={dashboardStats?.approvedLeaves ?? onLeaveEmployees}
                subtitle="Currently unavailable"
                accent="orange"
              />

              <StatCard
                icon={
                  <IconBuilding
                    width="20"
                    height="20"
                  />
                }
                title="Departments"
                value={
                  dashboardStats?.departmentStats ? Object.keys(dashboardStats.departmentStats).length : departmentsCount
                }
                subtitle="Active business units"
                accent="teal"
              />

              <StatCard
                icon={
                  <IconClock
                    width="20"
                    height="20"
                  />
                }
                title="Avg. Tenure"
                value={
                  formatTenure(
                    averageTenure
                  )
                }
                subtitle="Across all employees"
                accent="pink"
              />
            </section>

            <section
              className="analytics-grid"
              id="analytics-section"
            >
              <div className="panel chart-panel large-chart">
                <div className="panel-header">
                  <div>
                    <h3>
                      Workforce Trend
                    </h3>

                    <p>
                      Active workforce
                      over time
                    </p>
                  </div>

                  <select
                    className="chart-select"
                    value={chartRange}
                    onChange={(event) =>
                      setChartRange(
                        Number(
                          event.target
                            .value
                        )
                      )
                    }
                  >
                    <option value={6}>
                      Last 6 months
                    </option>

                    <option value={9}>
                      Last 9 months
                    </option>

                    <option value={12}>
                      Last 12 months
                    </option>
                  </select>
                </div>

                <div className="chart-wrapper">
                  <ResponsiveContainer
                    width="100%"
                    height="100%"
                  >
                    <AreaChart
                      data={trendData}
                      margin={{
                        top: 10,
                        right: 10,
                        left: -20,
                        bottom: 0,
                      }}
                    >
                      <defs>
                        <linearGradient
                          id="employeeGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="0%"
                            stopColor="#2563eb"
                            stopOpacity={
                              0.28
                            }
                          />

                          <stop
                            offset="100%"
                            stopColor="#2563eb"
                            stopOpacity={
                              0.02
                            }
                          />
                        </linearGradient>
                      </defs>

                      <CartesianGrid
                        strokeDasharray="3 3"
                        vertical={false}
                      />

                      <XAxis
                        dataKey="month"
                        axisLine={false}
                        tickLine={false}
                      />

                      <YAxis
                        axisLine={false}
                        tickLine={false}
                        allowDecimals={false}
                      />

                      <Tooltip />

                      <Area
                        type="monotone"
                        dataKey="employees"
                        stroke="#2563eb"
                        strokeWidth={3}
                        fill="url(#employeeGradient)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="panel chart-panel">
                <div className="panel-header">
                  <div>
                    <h3>
                      Workforce Status
                    </h3>

                    <p>
                      Current employee
                      status
                    </p>
                  </div>
                </div>

                <div className="donut-wrapper">
                  {statusChartData.length >
                  0 ? (
                    <ResponsiveContainer
                      width="100%"
                      height="100%"
                    >
                      <PieChart>
                        <Pie
                          data={
                            statusChartData
                          }
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          innerRadius={60}
                          outerRadius={88}
                          paddingAngle={4}
                          onClick={
                            handlePieClick
                          }
                        >
                          {statusChartData.map(
                            (
                              entry,
                              index
                            ) => (
                              <Cell
                                key={`cell-${index}`}
                                fill={
                                  entry.color
                                }
                              />
                            )
                          )}
                        </Pie>

                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="empty-chart">
                      No workforce data
                    </div>
                  )}

                  <div className="donut-center">
                    <strong>
                      {totalEmployees}
                    </strong>

                    <span>
                      Total
                    </span>
                  </div>
                </div>

                <div className="status-legend">
                  {STATUS_META.map(
                    (status) => {
                      const count =
                        status.key === "ACTIVE"
                          ? (dashboardStats?.activeEmployees ?? employees.filter(
                              (employee) =>
                                normalizeStatus(
                                  employee.employmentStatus
                                ) === status.key
                            ).length)
                          : status.key === "INACTIVE"
                            ? (dashboardStats?.inactiveEmployees ?? employees.filter(
                                (employee) =>
                                  normalizeStatus(
                                    employee.employmentStatus
                                  ) === status.key
                              ).length)
                            : employees.filter(
                                (employee) =>
                                  normalizeStatus(
                                    employee.employmentStatus
                                  ) === status.key
                              ).length;

                      return (
                        <button
                          key={
                            status.key
                          }
                          className="legend-item"
                          onClick={() =>
                            applyStatusFilter(
                              status.key
                            )
                          }
                        >
                          <span
                            className={`legend-dot ${status.dotClass}`}
                          />

                          <span>
                            {status.label}
                          </span>

                          <strong>
                            {count}
                          </strong>
                        </button>
                      );
                    }
                  )}
                </div>
              </div>

              <div className="panel chart-panel full-width">
                <div className="panel-header">
                  <div>
                    <h3>
                      Employees by
                      Department
                    </h3>

                    <p>
                      Workforce
                      distribution
                    </p>
                  </div>
                </div>

                <div className="department-chart">
                  {departmentChartData.length >
                  0 ? (
                    <ResponsiveContainer
                      width="100%"
                      height="100%"
                    >
                      <BarChart
                        data={
                          departmentChartData
                        }
                        margin={{
                          top: 10,
                          right: 10,
                          left: -20,
                          bottom: 0,
                        }}
                        onClick={
                          handleBarClick
                        }
                      >
                        <CartesianGrid
                          strokeDasharray="3 3"
                          vertical={false}
                        />

                        <XAxis
                          dataKey="name"
                          axisLine={false}
                          tickLine={false}
                        />

                        <YAxis
                          axisLine={false}
                          tickLine={false}
                          allowDecimals={
                            false
                          }
                        />

                        <Tooltip />

                        <Bar
                          dataKey="employees"
                          radius={[
                            7,
                            7,
                            0,
                            0,
                          ]}
                        >
                          {departmentChartData.map(
                            (
                              entry,
                              index
                            ) => (
                              <Cell
                                key={`bar-${entry.name}`}
                                fill={colorForIndex(
                                  index
                                )}
                              />
                            )
                          )}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="empty-chart">
                      No department data
                    </div>
                  )}
                </div>
              </div>
            </section>

            <section
              className="panel employee-panel"
              ref={
                employeesSectionRef
              }
            >
              <div className="panel-header employee-panel-header">
                <div>
                  <h3>
                    Employee Directory
                  </h3>

                  <p>
                    Manage and monitor
                    your workforce
                  </p>
                </div>

                <div className="employee-header-actions">
                  <button
                    className="secondary-button"
                    onClick={
                      exportCsv
                    }
                  >
                    <IconDownload
                      width="15"
                      height="15"
                    />
                    Export CSV
                  </button>

                  {canManageEmployees && (
                    <button
                      className="primary-button"
                      onClick={openAddEmployee}
                    >
                      <IconPlus
                        width="15"
                        height="15"
                      />
                      Add Employee
                    </button>
                  )}
                </div>
              </div>

              <div className="filter-bar">
                <div className="search-field">
                  <IconSearch
                    width="17"
                    height="17"
                  />

                  <input
                    value={search}
                    onChange={(event) =>
                      setSearch(
                        event.target.value
                      )
                    }
                    placeholder="Search employees, departments..."
                  />
                </div>

                <select
                  value={
                    departmentFilter
                  }
                  onChange={(event) =>
                    setDepartmentFilter(
                      event.target
                        .value
                    )
                  }
                >
                  <option value="ALL">
                    All Departments
                  </option>

                  {departments.map(
                    (department) => (
                      <option
                        key={
                          department
                        }
                        value={
                          department
                        }
                      >
                        {department}
                      </option>
                    )
                  )}
                </select>

                <select
                  value={
                    statusFilter
                  }
                  onChange={(event) =>
                    setStatusFilter(
                      event.target
                        .value
                    )
                  }
                >
                  <option value="ALL">
                    All Status
                  </option>

                  <option value="ACTIVE">
                    Active
                  </option>

                  <option value="INACTIVE">
                    Inactive
                  </option>

                  <option value="ON LEAVE">
                    On Leave
                  </option>
                </select>

                {(search ||
                  departmentFilter !==
                    "ALL" ||
                  statusFilter !==
                    "ALL") && (
                  <button
                    className="clear-filter-button"
                    onClick={
                      clearAllFilters
                    }
                  >
                    Clear
                  </button>
                )}
              </div>

              {loadError ? (
                <div className="error-state">
                  <div className="empty-icon">
                    <IconAlert
                      width="24"
                      height="24"
                    />
                  </div>

                  <h4>
                    Unable to load
                    employees
                  </h4>

                  <p>
                    Make sure the
                    NexusHR backend is
                    running on port
                    8080.
                  </p>

                  <button
                    className="primary-button"
                    onClick={
                      loadEmployees
                    }
                  >
                    <IconRefresh
                      width="15"
                      height="15"
                    />
                    Try Again
                  </button>
                </div>
              ) : (
                <EmployeeTable
                  canEditEmployees={
                    canEditEmployees
                  }
                  canDeleteEmployees={
                    canDeleteEmployees
                  }
                  employees={
                    currentEmployees
                  }
                  loading={loading}
                  search={search}
                  setSearch={
                    setSearch
                  }
                  departmentFilter={
                    departmentFilter
                  }
                  setDepartmentFilter={
                    setDepartmentFilter
                  }
                  statusFilter={
                    statusFilter
                  }
                  setStatusFilter={
                    setStatusFilter
                  }
                  departments={
                    departments
                  }
                  sortConfig={
                    sortConfig
                  }
                  handleSort={
                    handleSort
                  }
                  openViewEmployee={
                    openViewEmployee
                  }
                  openEditEmployee={
                    openEditEmployee
                  }
                  requestDelete={
                    requestDelete
                  }
                  exportCsv={
                    exportCsv
                  }
                  clearAllFilters={
                    clearAllFilters
                  }
                  page={page}
                  setPage={setPage}
                  totalPages={
                    totalPages
                  }
                  totalCount={
                    filteredEmployees.length
                  }
                  embedded
                />
              )}
            </section>
          </div>
        )}
      </main>

      {modalOpen && (
        <div
          className="modal-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              setModalOpen(false);
            }
          }}
        >
          <div className="modal employee-modal">
            <div className="modal-header">
              <div>
                <span className="modal-eyebrow">
                  EMPLOYEE PROFILE
                </span>

                <h3>
                  {editingEmployee
                    ? "Edit Employee"
                    : "Add New Employee"}
                </h3>

                <p>
                  Enter the employee
                  information below.
                </p>
              </div>

              <button
                className="modal-close"
                onClick={() =>
                  setModalOpen(false)
                }
              >
                <IconX
                  width="18"
                  height="18"
                />
              </button>
            </div>

            <form
              onSubmit={
                saveEmployee
              }
            >
              <div className="form-grid">
                <FormField
                  label="First Name"
                  name="firstName"
                  value={
                    form.firstName
                  }
                  onChange={
                    handleFormChange
                  }
                  error={
                    formErrors.firstName
                  }
                  required
                  inputRef={
                    firstFieldRef
                  }
                />

                <FormField
                  label="Last Name"
                  name="lastName"
                  value={
                    form.lastName
                  }
                  onChange={
                    handleFormChange
                  }
                  error={
                    formErrors.lastName
                  }
                  required
                />

                <FormField
                  label="Email Address"
                  name="email"
                  type="email"
                  value={
                    form.email
                  }
                  onChange={
                    handleFormChange
                  }
                  error={
                    formErrors.email
                  }
                  required
                />

                <FormField
                  label="Phone Number"
                  name="phone"
                  value={
                    form.phone
                  }
                  onChange={
                    handleFormChange
                  }
                />

                <FormField
                  label="Department"
                  name="department"
                  value={
                    form.department
                  }
                  onChange={
                    handleFormChange
                  }
                />

                <FormField
                  label="Job Title"
                  name="jobTitle"
                  value={
                    form.jobTitle
                  }
                  onChange={
                    handleFormChange
                  }
                />

                {!editingEmployee && (
                  <div className="form-field">
                    <label htmlFor="role">
                      Account Role
                      <span className="required">*</span>
                    </label>

                    <select
                      id="role"
                      name="role"
                      value={form.role || "EMPLOYEE"}
                      onChange={handleFormChange}
                    >
                      <option value="EMPLOYEE">
                        Employee
                      </option>

                      <option value="MANAGER">
                        Manager
                      </option>

                      <option value="HR">
                        HR
                      </option>
                    </select>
                  </div>
                )}

                <FormField
                  label="Salary"
                  name="salary"
                  type="number"
                  value={
                    form.salary
                  }
                  onChange={
                    handleFormChange
                  }
                  error={
                    formErrors.salary
                  }
                />

                <FormField
                  label="Joining Date"
                  name="joiningDate"
                  type="date"
                  value={
                    form.joiningDate
                  }
                  onChange={
                    handleFormChange
                  }
                />

                <div className="form-field">
                  <label htmlFor="employmentStatus">
                    Employment Status
                  </label>

                  <select
                    id="employmentStatus"
                    name="employmentStatus"
                    value={
                      form.employmentStatus
                    }
                    onChange={
                      handleFormChange
                    }
                  >
                    <option value="ACTIVE">
                      Active
                    </option>

                    <option value="INACTIVE">
                      Inactive
                    </option>

                    <option value="ON LEAVE">
                      On Leave
                    </option>
                  </select>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() =>
                    setModalOpen(false)
                  }
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <span className="button-spinner" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <IconCheck
                        width="16"
                        height="16"
                      />

                      {editingEmployee
                        ? "Save Changes"
                        : "Add Employee"}
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {viewModalOpen &&
        selectedEmployee && (
          <div
            className="modal-overlay"
            onMouseDown={(event) => {
              if (
                event.target ===
                event.currentTarget
              ) {
                setViewModalOpen(
                  false
                );
              }
            }}
          >
            <div className="modal profile-modal">
              <div className="modal-header">
                <div>
                  <span className="modal-eyebrow">
                    EMPLOYEE DETAILS
                  </span>

                  <h3>
                    Employee Profile
                  </h3>
                </div>

                <button
                  className="modal-close"
                  onClick={() =>
                    setViewModalOpen(
                      false
                    )
                  }
                >
                  <IconX
                    width="18"
                    height="18"
                  />
                </button>
              </div>

              <div className="profile-hero">
                <div
                  className="profile-avatar"
                  style={avatarStyle(
                    `${selectedEmployee.firstName}${selectedEmployee.lastName}`
                  )}
                >
                  {initialsOf(
                    selectedEmployee.firstName,
                    selectedEmployee.lastName
                  )}
                </div>

                <div>
                  <h4>
                    {
                      selectedEmployee.firstName
                    }{" "}
                    {
                      selectedEmployee.lastName
                    }
                  </h4>

                  <p>
                    {selectedEmployee.jobTitle ||
                      "Employee"}
                  </p>

                  <span
                    className={`status-badge ${normalizeStatus(
                      selectedEmployee.employmentStatus
                    )
                      .toLowerCase()
                      .replace(
                        " ",
                        "-"
                      )}`}
                  >
                    <span />
                    {
                      selectedEmployee.employmentStatus
                    }
                  </span>
                </div>
              </div>

              <div className="profile-grid">
                <ProfileItem
                  icon={
                    <IconMail
                      width="17"
                      height="17"
                    />
                  }
                  label="Email"
                  value={
                    selectedEmployee.email
                  }
                />

                <ProfileItem
                  icon={
                    <IconPhone
                      width="17"
                      height="17"
                    />
                  }
                  label="Phone"
                  value={
                    selectedEmployee.phone
                  }
                />

                <ProfileItem
                  icon={
                    <IconBuilding
                      width="17"
                      height="17"
                    />
                  }
                  label="Department"
                  value={
                    selectedEmployee.department
                  }
                />

                <ProfileItem
                  icon={
                    <IconCalendar
                      width="17"
                      height="17"
                    />
                  }
                  label="Joining Date"
                  value={formatDate(
                    selectedEmployee.joiningDate
                  )}
                />

                <ProfileItem
                  icon={
                    <IconIndianRupee
                      width="17"
                      height="17"
                    />
                  }
                  label="Salary"
                  value={formatSalary(
                    selectedEmployee.salary
                  )}
                />

                <ProfileItem
                  icon={
                    <IconClock
                      width="17"
                      height="17"
                    />
                  }
                  label="Tenure"
                  value={formatTenure(
                    tenureMonthsSince(
                      selectedEmployee.joiningDate
                    )
                  )}
                />
              </div>

              <div className="modal-footer">
                <button
                  className="secondary-button"
                  onClick={() =>
                    setViewModalOpen(
                      false
                    )
                  }
                >
                  Close
                </button>

                {canManageEmployees && (
                  <button
                    className="primary-button"
                    onClick={() => {
                      setViewModalOpen(false);
                      openEditEmployee(selectedEmployee);
                    }}
                  >
                    <IconEdit
                      width="15"
                      height="15"
                    />
                    Edit Employee
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

      {confirmTarget && (
        <div
          className="modal-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              setConfirmTarget(null);
            }
          }}
        >
          <div className="modal confirm-modal">
            <div className="confirm-icon">
              <IconTrash
                width="24"
                height="24"
              />
            </div>

            <h3>
              Permanently Delete Employee?
            </h3>

            <p>
              Are you sure you want to permanently delete{" "}
              <strong>
                {confirmTarget.firstName}{" "}
                {confirmTarget.lastName}
              </strong>
              ? This will permanently remove the employee profile,
              login account, attendance, leave, payroll, performance
              records, documents and other linked employee data.
              This action cannot be undone.
            </p>

            <div className="modal-footer">
              <button
                className="secondary-button"
                onClick={() =>
                  setConfirmTarget(
                    null
                  )
                }
                disabled={
                  deletingId !== null
                }
              >
                Cancel
              </button>

              <button
                className="danger-button"
                onClick={
                  handleConfirmDelete
                }
                disabled={
                  deletingId !== null
                }
              >
                {deletingId !== null ? (
                  <>
                    <span className="button-spinner" />
                    Deleting...
                  </>
                ) : (
                  <>
                    <IconTrash
                      width="15"
                      height="15"
                    />
                    Delete Permanently
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      <ToastStack
        toasts={toasts}
        dismissToast={
          dismissToast
        }
      />
    </div>
  );
}
function EmployeeTable({
  canEditEmployees,
  canDeleteEmployees,
  employees,
  loading,
  sortConfig,
  handleSort,
  openViewEmployee,
  openEditEmployee,
  requestDelete,
  page,
  setPage,
  totalPages,
  totalCount,
}) {
  return (
    <>
      <div className="table-wrapper">
        <table className="employee-table">
          <thead>
            <tr>
              <SortableHeader
                label="Employee"
                sortKey="name"
                sortConfig={
                  sortConfig
                }
                onSort={
                  handleSort
                }
              />

              <SortableHeader
                label="Department"
                sortKey="department"
                sortConfig={
                  sortConfig
                }
                onSort={
                  handleSort
                }
              />

              <th>Job Title</th>

              <SortableHeader
                label="Salary"
                sortKey="salary"
                sortConfig={
                  sortConfig
                }
                onSort={
                  handleSort
                }
              />

              <SortableHeader
                label="Joining Date"
                sortKey="joiningDate"
                sortConfig={
                  sortConfig
                }
                onSort={
                  handleSort
                }
              />

              <SortableHeader
                label="Status"
                sortKey="status"
                sortConfig={
                  sortConfig
                }
                onSort={
                  handleSort
                }
              />

              <th style={{ minWidth: "250px" }}>Actions</th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <SkeletonRows
                count={5}
              />
            ) : employees.length ===
              0 ? (
              <tr>
                <td
                  colSpan="7"
                  className="table-empty"
                >
                  <div className="empty-state">
                    <div className="empty-icon">
                      <IconUsers
                        width="24"
                        height="24"
                      />
                    </div>

                    <h4>
                      No employees found
                    </h4>

                    <p>
                      Try changing your
                      filters or add a
                      new employee.
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              employees.map(
                (employee) => (
                  <tr
                    key={
                      employee.id
                    }
                  >
                    <td>
                      <div className="employee-cell">
                        <div
                          className="employee-avatar"
                          style={avatarStyle(
                            `${employee.firstName}${employee.lastName}`
                          )}
                        >
                          {initialsOf(
                            employee.firstName,
                            employee.lastName
                          )}
                        </div>

                        <div>
                          <strong>
                            {
                              employee.firstName
                            }{" "}
                            {
                              employee.lastName
                            }
                          </strong>

                          <span>
                            {
                              employee.email
                            }
                          </span>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="department-text">
                        {
                          employee.department || "-"
                        }
                      </span>
                    </td>

                    <td>
                      <span className="job-title">
                        {
                          employee.jobTitle || "-"
                        }
                      </span>
                    </td>

                    <td>
                      <strong className="salary-text">
                        {formatSalary(
                          employee.salary
                        )}
                      </strong>
                    </td>

                    <td>
                      {formatDate(
                        employee.joiningDate
                      )}
                    </td>

                    <td>
                      <StatusRow
                        status={
                          employee.employmentStatus
                        }
                      />
                    </td>

                    <td style={{ minWidth: "250px" }}>
  <div
    className="table-actions"
    style={{
      display: "flex",
      alignItems: "center",
      gap: "8px",
      flexWrap: "nowrap",
      minWidth: "235px",
      visibility: "visible",
      opacity: 1,
    }}
  >
    <button
      type="button"
      title="View employee"
      onClick={() => openViewEmployee(employee)}
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "6px",
        minWidth: "70px",
        height: "36px",
        padding: "7px 11px",
        borderRadius: "8px",
        border: "1px solid #93c5fd",
        background: "#eff6ff",
        color: "#1d4ed8",
        fontSize: "12px",
        fontWeight: "700",
        cursor: "pointer",
      }}
    >
      <IconEye width="15" height="15" />
      <span>View</span>
    </button>

    {canEditEmployees && (
      <button
        type="button"
        title="Edit employee"
        onClick={() => openEditEmployee(employee)}
        style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          gap: "6px",
          minWidth: "66px",
          height: "36px",
          padding: "7px 11px",
          borderRadius: "8px",
          border: "1px solid #fdba74",
          background: "#fff7ed",
          color: "#c2410c",
          fontSize: "12px",
          fontWeight: "700",
          cursor: "pointer",
        }}
      >
        <IconEdit width="15" height="15" />
        <span>Edit</span>
      </button>
    )}

    {canDeleteEmployees && (
      <button
        type="button"
        title="Delete permanently"
        onClick={() => requestDelete(employee)}
        style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          gap: "6px",
          minWidth: "76px",
          height: "36px",
          padding: "7px 11px",
          borderRadius: "8px",
          border: "1px solid #fca5a5",
          background: "#fef2f2",
          color: "#b91c1c",
          fontSize: "12px",
          fontWeight: "700",
          cursor: "pointer",
        }}
      >
        <IconTrash width="15" height="15" />
        <span>Delete</span>
      </button>
    )}
  </div>
</td>
                  </tr>
                )
              )
            )}
          </tbody>
        </table>
      </div>

      {!loading &&
        totalCount > 0 && (
          <div className="pagination">
            <span>
              Showing{" "}
              <strong>
                {Math.min(
                  (page - 1) * 8 +
                    1,
                  totalCount
                )}
              </strong>{" "}
              -{" "}
              <strong>
                {Math.min(
                  page * 8,
                  totalCount
                )}
              </strong>{" "}
              of{" "}
              <strong>
                {totalCount}
              </strong>
            </span>

            <div className="pagination-buttons">
              <button
                disabled={page <= 1}
                onClick={() =>
                  setPage(
                    (current) =>
                      Math.max(
                        1,
                        current - 1
                      )
                  )
                }
              >
                <IconChevronLeft
                  width="15"
                  height="15"
                />
              </button>

              {Array.from(
                {
                  length: totalPages,
                },
                (_, index) =>
                  index + 1
              )
                .slice(
                  Math.max(0, page - 3),
                  Math.min(
                    totalPages,
                    page + 2
                  )
                )
                .map(
                  (pageNumber) => (
                    <button
                      key={
                        pageNumber
                      }
                      className={
                        page ===
                        pageNumber
                          ? "active"
                          : ""
                      }
                      onClick={() =>
                        setPage(
                          pageNumber
                        )
                      }
                    >
                      {pageNumber}
                    </button>
                  )
                )}

              <button
                disabled={
                  page >=
                  totalPages
                }
                onClick={() =>
                  setPage(
                    (current) =>
                      Math.min(
                        totalPages,
                        current + 1
                      )
                  )
                }
              >
                <IconChevronRight
                  width="15"
                  height="15"
                />
              </button>
            </div>
          </div>
        )}
    </>
  );
}

function StatCard({
  icon,
  title,
  value,
  subtitle,
  accent,
}) {
  return (
    <div
      className={`stat-card accent-${accent}`}
    >
      <div className="stat-card-top">
        <div className="stat-icon">
          {icon}
        </div>

        <span className="stat-accent-dot" />
      </div>

      <div className="stat-value">
        {value}
      </div>

      <div className="stat-title">
        {title}
      </div>

      <div className="stat-subtitle">
        {subtitle}
      </div>
    </div>
  );
}

function SortableHeader({
  label,
  sortKey,
  sortConfig,
  onSort,
}) {
  const active =
    sortConfig.key === sortKey;

  return (
    <th>
      <button
        className={`sortable-header ${
          active ? "active" : ""
        }`}
        onClick={() =>
          onSort(sortKey)
        }
      >
        {label}

        <span>
          {active &&
          sortConfig.direction ===
            "desc" ? (
            <IconChevronDown
              width="14"
              height="14"
            />
          ) : (
            <IconChevronUp
              width="14"
              height="14"
            />
          )}
        </span>
      </button>
    </th>
  );
}

function StatusRow({ status }) {
  const normalized =
    normalizeStatus(status);

  const className =
    normalized
      .toLowerCase()
      .replace(" ", "-");

  return (
    <span
      className={`status-badge ${className}`}
    >
      <span />
      {status || "Inactive"}
    </span>
  );
}

function FormField({
  label,
  name,
  type = "text",
  value,
  onChange,
  error,
  required,
  inputRef,
}) {
  return (
    <div className="form-field">
      <label htmlFor={name}>
        {label}

        {required && (
          <span className="required">
            *
          </span>
        )}
      </label>

      <input
        ref={inputRef}
        id={name}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        className={
          error ? "has-error" : ""
        }
      />

      {error && (
        <span className="field-error">
          {error}
        </span>
      )}
    </div>
  );
}

function ProfileItem({
  icon,
  label,
  value,
}) {
  return (
    <div className="profile-item">
      <div className="profile-item-icon">
        {icon}
      </div>

      <div>
        <span>{label}</span>
        <strong>
          {value || "-"}
        </strong>
      </div>
    </div>
  );
}

function SkeletonRows({
  count = 5,
}) {
  return Array.from(
    { length: count },
    (_, index) => (
      <tr
        key={`skeleton-${index}`}
        className="skeleton-row"
      >
        <td>
          <div className="skeleton-employee">
            <span />
            <div>
              <i />
              <i />
            </div>
          </div>
        </td>

        <td>
          <i />
        </td>

        <td>
          <i />
        </td>

        <td>
          <i />
        </td>

        <td>
          <i />
        </td>

        <td>
          <i />
        </td>

        <td>
          <i />
        </td>
      </tr>
    )
  );
}

function ToastStack({
  toasts,
  dismissToast,
}) {
  return (
    <div className="toast-stack">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast ${toast.type}`}
        >
          <div className="toast-icon">
            {toast.type ===
            "success" ? (
              <IconCheck
                width="17"
                height="17"
              />
            ) : toast.type ===
              "error" ? (
              <IconAlert
                width="17"
                height="17"
              />
            ) : (
              <IconInfo
                width="17"
                height="17"
              />
            )}
          </div>

          <span>
            {toast.message}
          </span>

          <button
            onClick={() =>
              dismissToast(
                toast.id
              )
            }
          >
            <IconX
              width="14"
              height="14"
            />
          </button>
        </div>
      ))}
    </div>
  );
}

/* ---------------- ICONS ---------------- */

function IconDashboard(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect
        x="3"
        y="3"
        width="7"
        height="7"
        rx="1"
      />
      <rect
        x="14"
        y="3"
        width="7"
        height="7"
        rx="1"
      />
      <rect
        x="3"
        y="14"
        width="7"
        height="7"
        rx="1"
      />
      <rect
        x="14"
        y="14"
        width="7"
        height="7"
        rx="1"
      />
    </svg>
  );
}

function IconUsers(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}

function IconClock(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle
        cx="12"
        cy="12"
        r="9"
      />
      <polyline points="12 7 12 12 15 14" />
    </svg>
  );
}

function IconCalendar(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect
        x="3"
        y="4"
        width="18"
        height="17"
        rx="2"
      />
      <line
        x1="16"
        y1="2"
        x2="16"
        y2="6"
      />
      <line
        x1="8"
        y1="2"
        x2="8"
        y2="6"
      />
      <line
        x1="3"
        y1="10"
        x2="21"
        y2="10"
      />
    </svg>
  );
}

function IconChart(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <line
        x1="18"
        y1="20"
        x2="18"
        y2="10"
      />
      <line
        x1="12"
        y1="20"
        x2="12"
        y2="4"
      />
      <line
        x1="6"
        y1="20"
        x2="6"
        y2="14"
      />
    </svg>
  );
}

function IconBuilding(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect
        x="4"
        y="3"
        width="16"
        height="18"
        rx="2"
      />
      <path d="M9 21v-4h6v4" />
      <path d="M8 7h2" />
      <path d="M14 7h2" />
      <path d="M8 11h2" />
      <path d="M14 11h2" />
    </svg>
  );
}

function IconSparkles(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="m12 3-1.5 4.5L6 9l4.5 1.5L12 15l1.5-4.5L18 9l-4.5-1.5L12 3Z" />
      <path d="m19 15-.75 2.25L16 18l2.25.75L19 21l.75-2.25L22 18l-2.25-.75L19 15Z" />
      <path d="m5 14-.75 2.25L2 17l2.25.75L5 20l.75-2.25L8 17l-2.25-.75L5 14Z" />
    </svg>
  );
}

function IconUserCheck(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <polyline points="16 11 18 13 22 9" />
    </svg>
  );
}

function IconIndianRupee(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M6 3h12" />
      <path d="M6 8h12" />
      <path d="M6 3c6 0 8 2 8 5s-2 5-8 5h7l4 8" />
    </svg>
  );
}

function IconSearch(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle
        cx="11"
        cy="11"
        r="7"
      />
      <line
        x1="20"
        y1="20"
        x2="16.65"
        y2="16.65"
      />
    </svg>
  );
}

function IconBell(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
      <path d="M10 21h4" />
    </svg>
  );
}

function IconSun(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle
        cx="12"
        cy="12"
        r="4"
      />
      <path d="M12 2v2" />
      <path d="M12 20v2" />
      <path d="m4.93 4.93 1.41 1.41" />
      <path d="m17.66 17.66 1.41 1.41" />
      <path d="M2 12h2" />
      <path d="M20 12h2" />
      <path d="m6.34 17.66-1.41 1.41" />
      <path d="m19.07 4.93-1.41 1.41" />
    </svg>
  );
}

function IconMoon(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
    </svg>
  );
}

function IconMenu(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <line
        x1="3"
        y1="6"
        x2="21"
        y2="6"
      />
      <line
        x1="3"
        y1="12"
        x2="21"
        y2="12"
      />
      <line
        x1="3"
        y1="18"
        x2="21"
        y2="18"
      />
    </svg>
  );
}

function IconChevronDown(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

function IconChevronUp(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="18 15 12 9 6 15" />
    </svg>
  );
}

function IconChevronLeft(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}

function IconChevronRight(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="9 18 15 12 9 6" />
    </svg>
  );
}

function IconPlus(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <line
        x1="12"
        y1="5"
        x2="12"
        y2="19"
      />
      <line
        x1="5"
        y1="12"
        x2="19"
        y2="12"
      />
    </svg>
  );
}

function IconDownload(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 3v12" />
      <path d="m7 10 5 5 5-5" />
      <path d="M5 21h14" />
    </svg>
  );
}

function IconRefresh(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10" />
      <path d="M20.49 15A9 9 0 0 1 5.64 18.36L1 14" />
    </svg>
  );
}

function IconEye(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
      <circle
        cx="12"
        cy="12"
        r="3"
      />
    </svg>
  );
}

function IconEdit(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  );
}

function IconTrash(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14H6L5 6" />
      <path d="M10 11v5" />
      <path d="M14 11v5" />
      <path d="M9 6V4h6v2" />
    </svg>
  );
}

function IconCheck(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

function IconX(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <line
        x1="18"
        y1="6"
        x2="6"
        y2="18"
      />
      <line
        x1="6"
        y1="6"
        x2="18"
        y2="18"
      />
    </svg>
  );
}

function IconMail(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect
        x="3"
        y="5"
        width="18"
        height="14"
        rx="2"
      />
      <polyline points="3 7 12 13 21 7" />
    </svg>
  );
}

function IconPhone(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.12.9.33 1.78.62 2.63a2 2 0 0 1-.45 2.11L8 9.73a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.85.29 1.73.5 2.63.62A2 2 0 0 1 22 16.92Z" />
    </svg>
  );
}

function IconLogout(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line
        x1="21"
        y1="12"
        x2="9"
        y2="12"
      />
    </svg>
  );
}

function IconAlert(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M10.3 3.9 2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
      <line
        x1="12"
        y1="9"
        x2="12"
        y2="13"
      />
      <line
        x1="12"
        y1="17"
        x2="12.01"
        y2="17"
      />
    </svg>
  );
}

function IconInfo(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle
        cx="12"
        cy="12"
        r="9"
      />
      <line
        x1="12"
        y1="11"
        x2="12"
        y2="16"
      />
      <line
        x1="12"
        y1="8"
        x2="12.01"
        y2="8"
      />
    </svg>
  );
}

export default App;
