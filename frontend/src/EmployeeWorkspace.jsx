import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Clock3,
  CreditCard,
  FileText,
  FolderOpen,
  History,
  IdCard,
  LayoutDashboard,
  Loader2,
  Mail,
  Phone,
  RefreshCw,
  Star,
  Target,
  TrendingUp,
  Trash2,
  UploadCloud,
  Download,
  User,
  WalletCards,
} from "lucide-react";

import { apiFetch, apiUrl } from "./api/apiClient";

/* =========================================================
   HELPERS
========================================================= */

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function safeText(value, fallback = "-") {
  if (value === null || value === undefined || String(value).trim() === "") {
    return fallback;
  }
  return String(value);
}

function humanizeStatus(value, fallback = "-") {
  const raw = safeText(value, fallback);
  if (raw === fallback) return fallback;
  return String(raw)
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function upper(value) {
  return safeText(value, "").toUpperCase();
}

function numberValue(...values) {
  for (const value of values) {
    const number = Number(value);
    if (!Number.isNaN(number) && value !== null && value !== undefined && value !== "") {
      return number;
    }
  }
  return 0;
}

function getEmployeeName(employee) {
  if (!employee) return "Employee";

  const direct =
    employee.fullName ||
    employee.name ||
    employee.employeeName;

  if (direct) return direct;

  const firstName = employee.firstName || "";
  const lastName = employee.lastName || "";
  return `${firstName} ${lastName}`.trim() || "Employee";
}

function parseDateValue(value) {
  if (!value) return null;

  const text = String(value).trim();

  // Safe parsing for backend LocalDate values such as 2026-10-03.
  const localDate = text.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (localDate) {
    const date = new Date(
      Number(localDate[1]),
      Number(localDate[2]) - 1,
      Number(localDate[3])
    );
    return Number.isNaN(date.getTime()) ? null : date;
  }

  const date = new Date(text);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDate(value) {
  const date = parseDateValue(value);
  if (!date) return safeText(value);

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function formatShortDate(value) {
  const date = parseDateValue(value);
  if (!date) return safeText(value);

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
  });
}

function formatMoney(value) {
  const number = Number(value);
  if (Number.isNaN(number)) return value ?? "-";

  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(number);
}

/*
 * Backend LocalTime values should not be timezone-converted again.
 * Examples:
 * 09:48
 * 09:48:00
 */
function extractClockParts(value) {
  if (!value) return null;

  const text = String(value).trim();
  const match = text.match(
    /(?:^|T|\s)(\d{1,2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?(?:Z|[+-]\d{2}:?\d{2})?$/
  );

  if (!match) return null;

  return {
    hour: Number(match[1]),
    minute: Number(match[2]),
    second: Number(match[3] || 0),
  };
}

function formatTime(value) {
  const parts = extractClockParts(value);
  if (!parts) return value ? String(value) : "-";

  let hour = parts.hour;
  const period = hour >= 12 ? "pm" : "am";
  hour %= 12;
  if (hour === 0) hour = 12;

  return `${String(hour).padStart(2, "0")}:${String(parts.minute).padStart(
    2,
    "0"
  )} ${period}`;
}

function timeToMinutes(value) {
  const parts = extractClockParts(value);
  if (!parts) return null;
  return parts.hour * 60 + parts.minute + parts.second / 60;
}

function formatDurationMinutes(value) {
  const totalMinutes = Number(value);
  if (!Number.isFinite(totalMinutes) || totalMinutes < 0) return "-";

  const roundedMinutes = Math.round(totalMinutes);
  const hours = Math.floor(roundedMinutes / 60);
  const minutes = roundedMinutes % 60;

  return `${hours}h ${String(minutes).padStart(2, "0")}m`;
}

function formatWorkingDuration(checkIn, checkOut) {
  const start = timeToMinutes(checkIn);
  const end = timeToMinutes(checkOut);

  if (start === null || end === null) return "-";

  let total = end - start;
  if (total < 0) total += 24 * 60;

  return formatDurationMinutes(total);
}

function recordWorkingMinutes(record) {
  if (!record) return null;

  const backendMinutes = Number(record.workingMinutes);
  if (Number.isFinite(backendMinutes) && backendMinutes >= 0 && (record.checkOutTime || record.checkOut)) {
    return Math.round(backendMinutes);
  }

  const backendHours = Number(record.workingHours);
  if (Number.isFinite(backendHours) && backendHours > 0) {
    return Math.round(backendHours * 60);
  }

  const start = timeToMinutes(record.checkInTime || record.checkIn);
  const end = timeToMinutes(record.checkOutTime || record.checkOut);
  if (start === null || end === null) return null;

  let total = end - start;
  if (total < 0) total += 24 * 60;
  return Math.round(total);
}

function extractAttendanceDate(record) {
  return record?.attendanceDate || record?.date || record?.workDate || null;
}

function isToday(value) {
  const date = parseDateValue(value);
  if (!date) return false;

  const today = new Date();

  return (
    date.getFullYear() === today.getFullYear() &&
    date.getMonth() === today.getMonth() &&
    date.getDate() === today.getDate()
  );
}

function isCurrentMonth(value) {
  const date = parseDateValue(value);
  if (!date) return false;

  const today = new Date();
  return (
    date.getFullYear() === today.getFullYear() &&
    date.getMonth() === today.getMonth()
  );
}


function toLocalDateKey(value) {
  const date = value instanceof Date ? value : parseDateValue(value);
  if (!date) return "";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function isDateInsideApprovedLeave(dateKey, leaveRows = []) {
  return leaveRows.some((leave) => {
    if (upper(leave?.status) !== "APPROVED") return false;

    const start = toLocalDateKey(leave?.startDate);
    const end = toLocalDateKey(leave?.endDate);

    return Boolean(start && end && dateKey >= start && dateKey <= end);
  });
}

function statusClass(status) {
  const value = upper(status);

  if (
    ["ACTIVE", "APPROVED", "PAID", "PRESENT", "COMPLETED", "READ"].includes(value)
  ) {
    return "ews-status-success";
  }

  if (
    ["REJECTED", "CANCELLED", "ABSENT", "FAILED", "INACTIVE"].includes(value)
  ) {
    return "ews-status-danger";
  }

  if (["PENDING", "PROCESSING", "ON_LEAVE", "LEAVE"].includes(value)) {
    return "ews-status-warning";
  }

  return "ews-status-neutral";
}

function greetingText() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function daysBetween(startDate, endDate) {
  const start = parseDateValue(startDate);
  const end = parseDateValue(endDate);
  if (!start || !end) return "-";

  const ms = end.setHours(0, 0, 0, 0) - start.setHours(0, 0, 0, 0);
  if (ms < 0) return "-";

  return Math.floor(ms / 86400000) + 1;
}

function notificationIsUnread(notification) {
  return (
    notification?.read === false ||
    notification?.isRead === false ||
    upper(notification?.status) === "UNREAD"
  );
}

/* =========================================================
   MAIN COMPONENT
========================================================= */

export default function EmployeeWorkspace({ initialTab = "overview" }) {
  const [activeTab, setActiveTab] = useState(initialTab);

  const [profile, setProfile] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [leaveBalances, setLeaveBalances] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [payroll, setPayroll] = useState([]);
  const [performance, setPerformance] = useState([]);
  const [performanceGoals, setPerformanceGoals] = useState([]);
  const [performanceFeedback, setPerformanceFeedback] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [documents, setDocuments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [attendanceError, setAttendanceError] = useState("");

  const [notificationFilter, setNotificationFilter] = useState("ALL");
  const [attendanceStartDate, setAttendanceStartDate] = useState("");
  const [attendanceEndDate, setAttendanceEndDate] = useState("");
  const [attendanceStatusFilter, setAttendanceStatusFilter] = useState("ALL");
  const [selectedPayslip, setSelectedPayslip] = useState(null);
  const [documentUploading, setDocumentUploading] = useState(false);
  const [documentForm, setDocumentForm] = useState({
    documentName: "",
    documentType: "OTHER",
    file: null,
  });

  const [leaveForm, setLeaveForm] = useState({
    leaveType: "CASUAL",
    startDate: "",
    endDate: "",
    reason: "",
  });

  const [goalProgressDrafts, setGoalProgressDrafts] = useState({});
  const [selfFeedbackForm, setSelfFeedbackForm] = useState({
    reviewId: "",
    rating: "3",
    comments: "",
    strengths: "",
    areasForImprovement: "",
  });

  /* =======================================================
     LOAD DATA
  ======================================================= */

  const loadWorkspace = useCallback(async (showMainLoader = true) => {
    if (showMainLoader) setLoading(true);

    setError("");
    setAttendanceError("");

    try {
      const results = await Promise.allSettled([
        apiFetch("/me"),
        apiFetch("/me/attendance"),
        apiFetch("/me/leaves"),
        apiFetch("/me/leaves/balance"),
        apiFetch("/me/payroll"),
        apiFetch("/me/performance"),
        apiFetch("/me/performance/goals"),
        apiFetch("/me/performance/feedback"),
        apiFetch("/me/notifications"),
        apiFetch("/me/documents"),
      ]);

      const [
        profileResult,
        attendanceResult,
        leavesResult,
        balanceResult,
        payrollResult,
        performanceResult,
        performanceGoalsResult,
        performanceFeedbackResult,
        notificationsResult,
        documentsResult,
      ] = results;

      if (profileResult.status === "fulfilled") {
        setProfile(profileResult.value);
      } else {
        throw profileResult.reason;
      }

      if (attendanceResult.status === "fulfilled") {
        setAttendance(safeArray(attendanceResult.value));
        setAttendanceError("");
      } else {
        setAttendanceError(
          attendanceResult.reason?.message ||
            "Unable to load your attendance history. Please refresh or sign in again."
        );
      }

      if (leavesResult.status === "fulfilled") {
        setLeaves(safeArray(leavesResult.value));
      }

      if (balanceResult.status === "fulfilled") {
        const value = balanceResult.value;

        if (Array.isArray(value)) {
          setLeaveBalances(value);
        } else if (value && typeof value === "object") {
          setLeaveBalances(
            Object.entries(value).map(([key, item]) => {
              if (item && typeof item === "object") {
                return {
                  leaveType: item.leaveType || key,
                  ...item,
                };
              }

              return {
                leaveType: key,
                remainingDays: item,
              };
            })
          );
        } else {
          setLeaveBalances([]);
        }
      }

      if (payrollResult.status === "fulfilled") {
        setPayroll(safeArray(payrollResult.value));
      }

      if (performanceResult.status === "fulfilled") {
        setPerformance(safeArray(performanceResult.value));
      }

      if (performanceGoalsResult.status === "fulfilled") {
        const goalRows = safeArray(performanceGoalsResult.value);
        setPerformanceGoals(goalRows);
        setGoalProgressDrafts((current) => {
          const next = { ...current };
          goalRows.forEach((goal) => {
            if (next[goal.id] == null) next[goal.id] = numberValue(goal.progress);
          });
          return next;
        });
      }

      if (performanceFeedbackResult.status === "fulfilled") {
        setPerformanceFeedback(safeArray(performanceFeedbackResult.value));
      }

      if (notificationsResult.status === "fulfilled") {
        setNotifications(safeArray(notificationsResult.value));
      }

      if (documentsResult.status === "fulfilled") {
        setDocuments(safeArray(documentsResult.value));
      }
    } catch (requestError) {
      console.error("Employee workspace load failed:", requestError);
      setError(
        requestError?.message || "Unable to load employee workspace."
      );
    } finally {
      if (showMainLoader) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadWorkspace();
  }, [loadWorkspace]);

  useEffect(() => {
    setActiveTab(initialTab || "overview");
  }, [initialTab]);

  /* =======================================================
     DERIVED DATA
  ======================================================= */

  const sortedAttendance = useMemo(() => {
    return [...attendance].sort((a, b) => {
      const aDate = parseDateValue(extractAttendanceDate(a));
      const bDate = parseDateValue(extractAttendanceDate(b));
      return (bDate?.getTime() || 0) - (aDate?.getTime() || 0);
    });
  }, [attendance]);

  /*
   * Build a complete date-wise attendance calendar.
   *
   * The backend only stores days that have an attendance/leave record.
   * For the employee screen we also show missing dates:
   *   - real attendance record -> keep backend status
   *   - approved leave        -> ON_LEAVE
   *   - no record             -> ABSENT
   *
   * By default the table shows every date from the first day of the
   * current month through today. When the user chooses From/To dates,
   * that selected range is used instead.
   */
  const completeAttendance = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const attendanceByDate = new Map();

    sortedAttendance.forEach((record) => {
      const dateKey = toLocalDateKey(extractAttendanceDate(record));
      if (dateKey) {
        attendanceByDate.set(dateKey, record);
      }
    });

    let rangeStart = attendanceStartDate
      ? parseDateValue(attendanceStartDate)
      : new Date(today.getFullYear(), today.getMonth(), 1);

    let rangeEnd = attendanceEndDate
      ? parseDateValue(attendanceEndDate)
      : today;

    if (!rangeStart) {
      rangeStart = new Date(today.getFullYear(), today.getMonth(), 1);
    }

    if (!rangeEnd || rangeEnd > today) {
      rangeEnd = today;
    }

    rangeStart.setHours(0, 0, 0, 0);
    rangeEnd.setHours(0, 0, 0, 0);

    // Do not generate rows before the employee joined the organisation.
    const joiningDate = parseDateValue(profile?.joiningDate);
    if (joiningDate) {
      joiningDate.setHours(0, 0, 0, 0);
      if (joiningDate > rangeStart && joiningDate <= rangeEnd) {
        rangeStart = joiningDate;
      }
    }

    if (rangeStart > rangeEnd) {
      return [];
    }

    const rows = [];
    const cursor = new Date(rangeStart);

    while (cursor <= rangeEnd) {
      const dateKey = toLocalDateKey(cursor);
      const realRecord = attendanceByDate.get(dateKey);

      if (realRecord) {
        rows.push({
          ...realRecord,
          attendanceDate:
            extractAttendanceDate(realRecord) || dateKey,
          generated: false,
        });
      } else {
        const onApprovedLeave = isDateInsideApprovedLeave(
          dateKey,
          leaves
        );

        rows.push({
          id: `generated-${dateKey}`,
          attendanceDate: dateKey,
          date: dateKey,
          checkInTime: null,
          checkOutTime: null,
          checkIn: null,
          checkOut: null,
          workingMinutes: 0,
          workingHours: 0,
          status: onApprovedLeave ? "ON_LEAVE" : "ABSENT",
          remarks: onApprovedLeave
            ? "Approved leave"
            : "Attendance not marked",
          generated: true,
        });
      }

      cursor.setDate(cursor.getDate() + 1);
    }

    return rows.sort((a, b) =>
      String(extractAttendanceDate(b)).localeCompare(
        String(extractAttendanceDate(a))
      )
    );
  }, [
    sortedAttendance,
    leaves,
    attendanceStartDate,
    attendanceEndDate,
    profile?.joiningDate,
  ]);

  const filteredAttendance = useMemo(() => {
    return completeAttendance.filter((record) => {
      if (
        attendanceStatusFilter !== "ALL" &&
        upper(record.status) !== attendanceStatusFilter
      ) {
        return false;
      }

      return true;
    });
  }, [completeAttendance, attendanceStatusFilter]);

  const todayAttendance = useMemo(() => {
    return (
      sortedAttendance.find((record) =>
        isToday(extractAttendanceDate(record))
      ) || null
    );
  }, [sortedAttendance]);

  const currentMonthAttendance = useMemo(
    () =>
      completeAttendance.filter((record) =>
        isCurrentMonth(extractAttendanceDate(record))
      ),
    [completeAttendance]
  );

  const presentDays = useMemo(
    () =>
      currentMonthAttendance.filter(
        (record) => upper(record.status) === "PRESENT"
      ).length,
    [currentMonthAttendance]
  );

  const absentDays = useMemo(
    () =>
      currentMonthAttendance.filter(
        (record) => upper(record.status) === "ABSENT"
      ).length,
    [currentMonthAttendance]
  );

  const leaveDaysThisMonth = useMemo(
    () =>
      currentMonthAttendance.filter((record) =>
        ["LEAVE", "ON_LEAVE"].includes(upper(record.status))
      ).length,
    [currentMonthAttendance]
  );

  const attendancePercentage = useMemo(() => {
    const tracked = currentMonthAttendance.length;
    if (tracked === 0) return 0;
    return Math.round((presentDays / tracked) * 100);
  }, [currentMonthAttendance.length, presentDays]);

  const totalLeaveRemaining = useMemo(
    () =>
      leaveBalances.reduce(
        (sum, balance) =>
          sum +
          numberValue(
            balance.remainingDays,
            balance.remaining,
            balance.balance
          ),
        0
      ),
    [leaveBalances]
  );

  const pendingLeaves = useMemo(
    () =>
      leaves.filter((leave) => upper(leave.status) === "PENDING")
        .length,
    [leaves]
  );

  const approvedUpcomingLeaves = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return leaves
      .filter((leave) => {
        if (upper(leave.status) !== "APPROVED") return false;
        const start = parseDateValue(leave.startDate);
        return start && start >= today;
      })
      .sort((a, b) => {
        const aDate = parseDateValue(a.startDate);
        const bDate = parseDateValue(b.startDate);
        return (aDate?.getTime() || 0) - (bDate?.getTime() || 0);
      });
  }, [leaves]);

  const unreadCount = useMemo(
    () => notifications.filter(notificationIsUnread).length,
    [notifications]
  );

  const latestPayroll = useMemo(() => {
    if (!payroll.length) return null;

    return [...payroll].sort((a, b) => {
      const aValue = String(
        a.payrollMonth || a.month || a.period || a.id || ""
      );
      const bValue = String(
        b.payrollMonth || b.month || b.period || b.id || ""
      );
      return bValue.localeCompare(aValue);
    })[0];
  }, [payroll]);

  const latestPerformance = useMemo(() => {
    if (!performance.length) return null;

    return [...performance].sort((a, b) => {
      const aDate = parseDateValue(
        a.reviewDate || a.createdAt || a.updatedAt
      );
      const bDate = parseDateValue(
        b.reviewDate || b.createdAt || b.updatedAt
      );
      return (bDate?.getTime() || 0) - (aDate?.getTime() || 0);
    })[0];
  }, [performance]);

  const performanceRating = numberValue(
    latestPerformance?.rating,
    latestPerformance?.overallRating,
    latestPerformance?.performanceScore,
    latestPerformance?.score
  );


  const performanceHistory = useMemo(() => {
    return [...performance].sort((a, b) => {
      const aDate = parseDateValue(
        a.reviewPeriodEnd ||
          a.reviewDate ||
          a.updatedAt ||
          a.createdAt
      );
      const bDate = parseDateValue(
        b.reviewPeriodEnd ||
          b.reviewDate ||
          b.updatedAt ||
          b.createdAt
      );

      return (aDate?.getTime() || 0) - (bDate?.getTime() || 0);
    });
  }, [performance]);

  const completedPerformanceReviews = useMemo(
    () =>
      performance.filter(
        (review) => upper(review.status) === "COMPLETED"
      ).length,
    [performance]
  );

  const averagePerformanceRating = useMemo(() => {
    const ratings = performance
      .map((review) =>
        numberValue(
          review.rating,
          review.overallRating,
          review.performanceScore,
          review.score
        )
      )
      .filter((value) => value > 0);

    if (!ratings.length) return 0;

    return ratings.reduce((sum, value) => sum + value, 0) / ratings.length;
  }, [performance]);

  const activePerformanceGoals = useMemo(
    () =>
      performanceGoals.filter(
        (goal) => upper(goal.status) === "ACTIVE"
      ),
    [performanceGoals]
  );

  const completedPerformanceGoals = useMemo(
    () =>
      performanceGoals.filter(
        (goal) => upper(goal.status) === "COMPLETED"
      ),
    [performanceGoals]
  );

  const averageGoalProgress = useMemo(() => {
    if (!performanceGoals.length) return 0;

    return Math.round(
      performanceGoals.reduce(
        (sum, goal) => sum + numberValue(goal.progress),
        0
      ) / performanceGoals.length
    );
  }, [performanceGoals]);

  const averageFeedbackRating = useMemo(() => {
    const ratings = performanceFeedback
      .map((item) => numberValue(item.rating))
      .filter((value) => value > 0);

    if (!ratings.length) return 0;

    return ratings.reduce((sum, value) => sum + value, 0) / ratings.length;
  }, [performanceFeedback]);

  const performanceTrend = useMemo(() => {
    return performanceHistory.slice(-6).map((review, index) => {
      const rating = numberValue(
        review.rating,
        review.overallRating,
        review.performanceScore,
        review.score
      );

      const score = numberValue(
        review.finalScore,
        rating > 0 && rating <= 5 ? rating * 20 : rating
      );

      return {
        id: review.id || index,
        label: safeText(
          review.reviewCycle,
          review.reviewPeriodEnd
            ? formatShortDate(review.reviewPeriodEnd)
            : `Review ${index + 1}`
        ),
        rating,
        score: Math.max(0, Math.min(100, score)),
        status: review.status || "DRAFT",
      };
    });
  }, [performanceHistory]);

  const latestPerformanceTrendDelta = useMemo(() => {
    if (performanceTrend.length < 2) return null;

    const current = performanceTrend[performanceTrend.length - 1].score;
    const previous = performanceTrend[performanceTrend.length - 2].score;

    return Math.round((current - previous) * 10) / 10;
  }, [performanceTrend]);

  const checkedIn = Boolean(
    todayAttendance?.checkInTime || todayAttendance?.checkIn
  );

  const checkedOut = Boolean(
    todayAttendance?.checkOutTime || todayAttendance?.checkOut
  );

  const workingHours = checkedIn && !checkedOut
    ? "In progress"
    : formatDurationMinutes(recordWorkingMinutes(todayAttendance));

  const completedAttendanceDays = useMemo(
    () =>
      currentMonthAttendance.filter(
        (record) =>
          Boolean(record.checkInTime || record.checkIn) &&
          Boolean(record.checkOutTime || record.checkOut)
      ).length,
    [currentMonthAttendance]
  );

  const currentMonthWorkingMinutes = useMemo(
    () =>
      currentMonthAttendance.reduce((total, record) => {
        const minutes = recordWorkingMinutes(record);
        return total + (minutes === null ? 0 : minutes);
      }, 0),
    [currentMonthAttendance]
  );

  const averageWorkingMinutes =
    completedAttendanceDays > 0
      ? Math.round(currentMonthWorkingMinutes / completedAttendanceDays)
      : 0;

  const incompleteAttendanceDays = useMemo(
    () =>
      currentMonthAttendance.filter(
        (record) =>
          Boolean(record.checkInTime || record.checkIn) &&
          !Boolean(record.checkOutTime || record.checkOut)
      ).length,
    [currentMonthAttendance]
  );

  const monthHoursLabel = formatDurationMinutes(currentMonthWorkingMinutes);
  const averageHoursLabel = formatDurationMinutes(averageWorkingMinutes);

  const pendingLeaveCount = useMemo(
    () => leaves.filter((leave) => upper(leave.status) === "PENDING").length,
    [leaves]
  );

  const approvedLeaveCount = useMemo(
    () => leaves.filter((leave) => upper(leave.status) === "APPROVED").length,
    [leaves]
  );

  const rejectedLeaveCount = useMemo(
    () => leaves.filter((leave) => ["REJECTED", "CANCELLED"].includes(upper(leave.status))).length,
    [leaves]
  );

  const totalRemainingLeave = useMemo(
    () =>
      leaveBalances.reduce(
        (total, balance) =>
          total +
          numberValue(
            balance.remainingDays,
            balance.remaining,
            balance.balance
          ),
        0
      ),
    [leaveBalances]
  );

  const filteredNotifications = useMemo(() => {
    if (notificationFilter === "UNREAD") {
      return notifications.filter(notificationIsUnread);
    }

    if (notificationFilter === "READ") {
      return notifications.filter((item) => !notificationIsUnread(item));
    }

    return notifications;
  }, [notificationFilter, notifications]);

  const recentActivity = useMemo(() => {
    const items = [];

    sortedAttendance.slice(0, 2).forEach((record) => {
      items.push({
        id: `attendance-${record.id || extractAttendanceDate(record)}`,
        title: `Attendance ${safeText(record.status, "updated")}`,
        description: `${formatShortDate(
          extractAttendanceDate(record)
        )} · In ${formatTime(record.checkInTime || record.checkIn)} · Out ${formatTime(
          record.checkOutTime || record.checkOut
        )}`,
        type: "attendance",
      });
    });

    leaves.slice(0, 2).forEach((leave) => {
      items.push({
        id: `leave-${leave.id}`,
        title: `${safeText(leave.leaveType || leave.type)} leave`,
        description: `${formatShortDate(leave.startDate)} - ${formatShortDate(
          leave.endDate
        )} · ${safeText(leave.status)}`,
        type: "leave",
      });
    });

    notifications.slice(0, 2).forEach((notification) => {
      items.push({
        id: `notification-${notification.id}`,
        title: safeText(notification.title, "Notification"),
        description: safeText(notification.message, ""),
        type: "notification",
      });
    });

    return items.slice(0, 6);
  }, [sortedAttendance, leaves, notifications]);

  /* =======================================================
     ACTIONS
  ======================================================= */

  function mergeAttendanceRecord(record) {
    if (!record) return;

    setAttendance((current) => {
      const recordDate = extractAttendanceDate(record);
      const remaining = current.filter((item) => {
        if (record.id != null && item?.id === record.id) return false;
        return extractAttendanceDate(item) !== recordDate;
      });
      return [record, ...remaining];
    });
  }

  async function handleCheckIn() {
    setActionLoading(true);
    setError("");
    setSuccess("");
    setAttendanceError("");

    try {
      const updated = await apiFetch("/me/attendance/check-in", { method: "POST" });
      mergeAttendanceRecord(updated);
      setSuccess("Checked in successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      const message = requestError?.message || "Unable to check in.";
      setAttendanceError(message);
      setError(message);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleCheckOut() {
    setActionLoading(true);
    setError("");
    setSuccess("");
    setAttendanceError("");

    try {
      const updated = await apiFetch("/me/attendance/check-out", { method: "POST" });
      mergeAttendanceRecord(updated);
      setSuccess("Checked out successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      const message = requestError?.message || "Unable to check out.";
      setAttendanceError(message);
      setError(message);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleHalfDay() {
    setActionLoading(true);
    setError("");
    setSuccess("");
    setAttendanceError("");

    try {
      const updated = await apiFetch("/me/attendance/half-day", { method: "POST" });
      mergeAttendanceRecord(updated);
      setSuccess("Half-day attendance recorded successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      const message = requestError?.message || "Unable to mark half day.";
      setAttendanceError(message);
      setError(message);
    } finally {
      setActionLoading(false);
    }
  }

  function handleLeaveInput(event) {
    const { name, value } = event.target;
    setLeaveForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  }

  async function submitLeave(event) {
    event.preventDefault();

    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      await apiFetch("/me/leaves", {
        method: "POST",
        body: JSON.stringify({
          leaveType: leaveForm.leaveType,
          startDate: leaveForm.startDate,
          endDate: leaveForm.endDate,
          reason: leaveForm.reason.trim(),
        }),
      });

      setSuccess("Leave request submitted successfully.");

      setLeaveForm({
        leaveType: "CASUAL",
        startDate: "",
        endDate: "",
        reason: "",
      });

      await loadWorkspace(false);
    } catch (requestError) {
      setError(
        requestError?.message || "Unable to submit leave request."
      );
    } finally {
      setActionLoading(false);
    }
  }

  async function cancelLeave(leaveId) {
    if (!leaveId) return;

    const confirmed = window.confirm(
      "Do you want to cancel this leave request?"
    );

    if (!confirmed) return;

    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      await apiFetch(`/me/leaves/${leaveId}/cancel`, {
        method: "PUT",
      });

      setSuccess("Leave request cancelled successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      setError(
        requestError?.message || "Unable to cancel leave request."
      );
    } finally {
      setActionLoading(false);
    }
  }

  async function markAllNotificationsRead() {
    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      await apiFetch("/me/notifications/read-all", {
        method: "PUT",
      });

      setSuccess("Notifications marked as read.");
      await loadWorkspace(false);
    } catch (requestError) {
      setError(
        requestError?.message || "Unable to update notifications."
      );
    } finally {
      setActionLoading(false);
    }
  }

  function getAuthToken() {
    const savedUser = window.localStorage.getItem("nexushr-user");

    if (!savedUser) {
      throw new Error("Authentication required. Please log in again.");
    }

    try {
      const user = JSON.parse(savedUser);
      if (!user?.token) {
        throw new Error("Authentication required. Please log in again.");
      }
      return user.token;
    } catch (error) {
      if (error?.message?.startsWith("Authentication")) throw error;
      throw new Error("Invalid login information. Please log in again.");
    }
  }

  async function saveMyGoalProgress(goal) {
    if (!goal?.id) return;
    const progress = Number(goalProgressDrafts[goal.id] ?? goal.progress ?? 0);
    if (!Number.isFinite(progress) || progress < 0 || progress > 100) {
      setError("Goal progress must be between 0 and 100.");
      return;
    }

    setActionLoading(true);
    setError("");
    setSuccess("");
    try {
      await apiFetch(`/me/performance/goals/${goal.id}/progress`, {
        method: "PATCH",
        body: JSON.stringify({
          progress: Math.round(progress),
          employeeComment: goal.employeeComment || "",
        }),
      });
      setSuccess("Goal progress updated successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      setError(requestError?.message || "Unable to update goal progress.");
    } finally {
      setActionLoading(false);
    }
  }

  async function submitMySelfFeedback(event) {
    event.preventDefault();
    if (!selfFeedbackForm.reviewId) {
      setError("Select a performance review first.");
      return;
    }
    setActionLoading(true);
    setError("");
    setSuccess("");
    try {
      await apiFetch(`/me/performance/${selfFeedbackForm.reviewId}/self-feedback`, {
        method: "POST",
        body: JSON.stringify({
          rating: Number(selfFeedbackForm.rating),
          comments: selfFeedbackForm.comments,
          strengths: selfFeedbackForm.strengths,
          areasForImprovement: selfFeedbackForm.areasForImprovement,
        }),
      });
      setSuccess("Self feedback submitted successfully.");
      setSelfFeedbackForm({
        reviewId: "", rating: "3", comments: "", strengths: "", areasForImprovement: "",
      });
      await loadWorkspace(false);
    } catch (requestError) {
      setError(requestError?.message || "Unable to submit self feedback.");
    } finally {
      setActionLoading(false);
    }
  }

  async function downloadProtectedFile(endpoint, fallbackFilename) {
    const token = getAuthToken();

    const response = await fetch(apiUrl(endpoint), {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      let message = `Download failed with status ${response.status}.`;

      try {
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
          const data = await response.json();
          message = data?.message || data?.error || message;
        } else {
          const responseText = await response.text();
          if (responseText) message = responseText;
        }
      } catch {
        // Keep the fallback message.
      }

      throw new Error(message);
    }

    const blob = await response.blob();
    if (!blob.size) {
      throw new Error("Downloaded file is empty.");
    }

    const disposition = response.headers.get("content-disposition") || "";
    const match = disposition.match(/filename="?([^";]+)"?/i);
    const filename = match?.[1]?.trim() || fallbackFilename;

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  async function downloadPayslip(payrollItem) {
    if (!payrollItem?.id) {
      setError("Unable to download payslip: payroll ID is missing.");
      return;
    }

    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      const period = safeText(
        payrollItem.payrollMonth ||
          payrollItem.month ||
          payrollItem.payPeriod ||
          payrollItem.period,
        payrollItem.id
      );

      await downloadProtectedFile(
        `/me/payroll/${payrollItem.id}/payslip`,
        `NexusHR-Payslip-${period}.pdf`
      );

      setSuccess("Payslip downloaded successfully.");
    } catch (requestError) {
      setError(
        requestError?.message || "Unable to download payslip PDF."
      );
    } finally {
      setActionLoading(false);
    }
  }

  function handleDocumentInput(event) {
    const { name, value, files } = event.target;

    setDocumentForm((previous) => ({
      ...previous,
      [name]: name === "file" ? files?.[0] || null : value,
    }));
  }

  async function uploadDocument(event) {
    event.preventDefault();

    if (!documentForm.file) {
      setError("Please choose a file to upload.");
      return;
    }

    setDocumentUploading(true);
    setError("");
    setSuccess("");

    try {
      const token = getAuthToken();
      const formData = new FormData();

      formData.append("documentName", documentForm.documentName.trim());
      formData.append("documentType", documentForm.documentType);
      formData.append("file", documentForm.file);

      const response = await fetch(apiUrl("/me/documents"), {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (!response.ok) {
        let message = `Upload failed with status ${response.status}.`;

        try {
          const contentType = response.headers.get("content-type") || "";
          if (contentType.includes("application/json")) {
            const data = await response.json();
            message = data?.message || data?.error || message;
          } else {
            const responseText = await response.text();
            if (responseText) message = responseText;
          }
        } catch {
          // Keep fallback message.
        }

        throw new Error(message);
      }

      setDocumentForm({
        documentName: "",
        documentType: "OTHER",
        file: null,
      });

      const fileInput = document.getElementById("ews-document-file");
      if (fileInput) fileInput.value = "";

      setSuccess("Document uploaded successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      setError(requestError?.message || "Unable to upload document.");
    } finally {
      setDocumentUploading(false);
    }
  }

  async function downloadDocument(documentItem) {
    if (!documentItem?.id) return;

    setError("");
    setSuccess("");

    try {
      await downloadProtectedFile(
        `/me/documents/${documentItem.id}/download`,
        documentItem.fileName || `document-${documentItem.id}`
      );
    } catch (requestError) {
      setError(requestError?.message || "Unable to download document.");
    }
  }

  async function deleteDocument(documentItem) {
    if (!documentItem?.id) return;

    const confirmed = window.confirm(
      `Delete "${documentItem.documentName || documentItem.fileName || "this document"}"?`
    );

    if (!confirmed) return;

    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      await apiFetch(`/me/documents/${documentItem.id}`, {
        method: "DELETE",
      });

      setSuccess("Document deleted successfully.");
      await loadWorkspace(false);
    } catch (requestError) {
      setError(requestError?.message || "Unable to delete document.");
    } finally {
      setActionLoading(false);
    }
  }

  function openPayslip(payrollItem) {
    setSelectedPayslip(payrollItem);
  }

  function closePayslip() {
    setSelectedPayslip(null);
  }

  function printPayslip(payrollItem = selectedPayslip) {
    if (!payrollItem) return;

    const popup = window.open("", "_blank", "width=900,height=850");

    if (!popup) {
      setError("Popup was blocked. Allow popups and try again.");
      return;
    }

    const gross =
      numberValue(payrollItem.basicSalary, payrollItem.basic) +
      numberValue(payrollItem.allowances, payrollItem.totalAllowances) +
      numberValue(payrollItem.bonus) +
      numberValue(payrollItem.overtimePay);

    const employeeNameSafe = employeeName.replace(/[<>]/g, "");
    const period = safeText(
      payrollItem.payrollMonth ||
        payrollItem.month ||
        payrollItem.payPeriod ||
        payrollItem.period
    );

    popup.document.write(`
      <!doctype html>
      <html>
        <head>
          <title>NexusHR Payslip - ${period}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 0; padding: 36px; color: #172033; }
            .sheet { max-width: 760px; margin: auto; border: 1px solid #dbe3ed; border-radius: 12px; padding: 28px; }
            .top { display:flex; justify-content:space-between; gap:20px; border-bottom:2px solid #2563eb; padding-bottom:18px; }
            h1 { margin:0; font-size:24px; }
            .muted { color:#64748b; font-size:13px; }
            .grid { display:grid; grid-template-columns:1fr 1fr; gap:10px 24px; margin:22px 0; }
            .row { display:flex; justify-content:space-between; padding:11px 0; border-bottom:1px solid #edf1f6; }
            .net { font-size:18px; font-weight:700; color:#047857; }
            .footer { margin-top:28px; text-align:center; color:#64748b; font-size:12px; }
            @media print { body { padding:0; } .sheet { border:0; } }
          </style>
        </head>
        <body>
          <div class="sheet">
            <div class="top">
              <div><h1>NexusHR Payslip</h1><div class="muted">Workforce Intelligence</div></div>
              <div style="text-align:right"><strong>${period}</strong><div class="muted">Payroll period</div></div>
            </div>
            <div class="grid">
              <div><span class="muted">Employee</span><br><strong>${employeeNameSafe}</strong></div>
              <div><span class="muted">Employee ID</span><br><strong>${employeeId}</strong></div>
              <div><span class="muted">Department</span><br><strong>${department}</strong></div>
              <div><span class="muted">Job title</span><br><strong>${jobTitle}</strong></div>
            </div>
            <div class="row"><span>Basic Salary</span><strong>${formatMoney(payrollItem.basicSalary ?? payrollItem.basic)}</strong></div>
            <div class="row"><span>Allowances</span><strong>${formatMoney(payrollItem.allowances ?? payrollItem.totalAllowances)}</strong></div>
            <div class="row"><span>Bonus</span><strong>${formatMoney(payrollItem.bonus)}</strong></div>
            <div class="row"><span>Overtime Pay</span><strong>${formatMoney(payrollItem.overtimePay)}</strong></div>
            <div class="row"><span>Gross Salary</span><strong>${formatMoney(payrollItem.grossSalary ?? payrollItem.grossPay ?? gross)}</strong></div>
            <div class="row"><span>Provident Fund</span><strong>${formatMoney(payrollItem.providentFund)}</strong></div>
            <div class="row"><span>Professional Tax</span><strong>${formatMoney(payrollItem.professionalTax)}</strong></div>
            <div class="row"><span>Other Deductions</span><strong>${formatMoney(numberValue(payrollItem.deductions, payrollItem.totalDeductions) + numberValue(payrollItem.otherDeductions))}</strong></div>
            <div class="row"><span>Income Tax / TDS</span><strong>${formatMoney(payrollItem.tax ?? payrollItem.taxAmount)}</strong></div>
            <div class="row net"><span>Net Salary</span><strong>${formatMoney(payrollItem.netSalary ?? payrollItem.netPay)}</strong></div>
            <div class="row"><span>Status</span><strong>${safeText(payrollItem.status)}</strong></div>
            <div class="row"><span>Payment Date</span><strong>${safeText(payrollItem.paymentDate, "-")}</strong></div>
            <div class="row"><span>Payment Reference</span><strong>${safeText(payrollItem.paymentReference, "-")}</strong></div>
            <div class="footer">This is a computer-generated payslip from NexusHR. Use your browser's Print dialog and choose Save as PDF.</div>
          </div>
          <script>window.onload = () => { window.print(); };<\/script>
        </body>
      </html>
    `);

    popup.document.close();
  }

  /* =======================================================
     PROFILE VALUES
  ======================================================= */

  const employeeName = getEmployeeName(profile);
  const employeeId = profile?.id || profile?.employeeId || "-";
  const username = profile?.username || profile?.userName || "-";
  const department =
    profile?.department || profile?.departmentName || "-";
  const jobTitle =
    profile?.jobTitle || profile?.designation || "-";
  const employmentStatus =
    profile?.employmentStatus || profile?.status || "-";
  const employeeEmail = profile?.email || "-";
  const employeePhone = profile?.phone || profile?.phoneNumber || "-";
  const joiningDate = profile?.joiningDate || profile?.dateOfJoining || null;

  const tabs = [
    { id: "overview", label: "Overview", icon: LayoutDashboard },
    { id: "profile", label: "Profile", icon: User },
    { id: "attendance", label: "Attendance", icon: Clock3 },
    { id: "leave", label: "Leave", icon: CalendarDays },
    { id: "payroll", label: "Payroll", icon: WalletCards },
    { id: "documents", label: "Documents", icon: FolderOpen },
    { id: "performance", label: "Performance", icon: TrendingUp },
    {
      id: "notifications",
      label:
        unreadCount > 0
          ? `Notifications (${unreadCount})`
          : "Notifications",
      icon: Bell,
    },
  ];

  if (loading) {
    return (
      <div className="ews-loading">
        <Loader2 size={30} className="ews-spin" />
        <span>Loading employee workspace...</span>
        <WorkspaceStyles />
      </div>
    );
  }

  return (
    <div className="ews">
      {/* ===================================================
          HERO
      =================================================== */}

      <section className="ews-hero">
        <div className="ews-hero-main">
          <div className="ews-avatar">
            {employeeName
              .split(" ")
              .slice(0, 2)
              .map((part) => part.charAt(0))
              .join("")
              .toUpperCase()}
          </div>

          <div>
            <div className="ews-eyebrow">EMPLOYEE SELF-SERVICE</div>

            <h2>
              {greetingText()}, {employeeName.split(" ")[0]}.
            </h2>

            <p>
              {jobTitle} · {department}
            </p>

            <div className="ews-hero-tags">
              <span>
                <IdCard size={14} />
                Employee #{employeeId}
              </span>

              <span>
                <User size={14} />
                Username: @{username}
              </span>

              <span className={statusClass(employmentStatus)}>
                <CheckCircle2 size={14} />
                {employmentStatus}
              </span>
            </div>
          </div>
        </div>

        <div className="ews-hero-side">
          <div className="ews-date">
            {new Date().toLocaleDateString("en-IN", {
              weekday: "long",
              day: "2-digit",
              month: "long",
              year: "numeric",
            })}
          </div>

          <button
            type="button"
            className="ews-btn ews-btn-secondary"
            onClick={() => loadWorkspace()}
            disabled={loading || actionLoading}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
      </section>

      {error && <div className="ews-alert ews-alert-error">{error}</div>}
      {success && (
        <div className="ews-alert ews-alert-success">{success}</div>
      )}

      {/* ===================================================
          NAVIGATION
      =================================================== */}

      <nav className="ews-tabs" aria-label="Employee workspace sections">
        {tabs.map((tab) => {
          const Icon = tab.icon;

          return (
            <button
              type="button"
              key={tab.id}
              className={
                activeTab === tab.id ? "ews-tab active" : "ews-tab"
              }
              onClick={() => setActiveTab(tab.id)}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </nav>

      {/* ===================================================
          OVERVIEW
      =================================================== */}

      {activeTab === "overview" && (
        <>
          <div className="ews-summary-grid">
            <SummaryCard
              icon={<Clock3 size={20} />}
              title="Attendance"
              value={`${attendancePercentage}%`}
              caption={`${presentDays} present · ${absentDays} absent`}
              progress={attendancePercentage}
              onClick={() => setActiveTab("attendance")}
            />

            <SummaryCard
              icon={<CalendarDays size={20} />}
              title="Leave balance"
              value={`${totalLeaveRemaining} days`}
              caption={`${pendingLeaves} pending request(s)`}
              onClick={() => setActiveTab("leave")}
            />

            <SummaryCard
              icon={<CreditCard size={20} />}
              title="Latest salary"
              value={
                latestPayroll
                  ? formatMoney(
                      latestPayroll.netSalary ??
                        latestPayroll.netPay ??
                        latestPayroll.netAmount
                    )
                  : "-"
              }
              caption={safeText(
                latestPayroll?.payrollMonth ||
                  latestPayroll?.month ||
                  latestPayroll?.period,
                "No payroll record"
              )}
              onClick={() => setActiveTab("payroll")}
            />

            <SummaryCard
              icon={<Star size={20} />}
              title="Performance"
              value={performanceRating ? `${performanceRating}` : "-"}
              caption={
                latestPerformance
                  ? "Latest review score"
                  : "No review available"
              }
              progress={
                performanceRating > 0 && performanceRating <= 5
                  ? performanceRating * 20
                  : performanceRating <= 100
                  ? performanceRating
                  : 0
              }
              onClick={() => setActiveTab("performance")}
            />

            <SummaryCard
              icon={<Bell size={20} />}
              title="Unread notifications"
              value={unreadCount}
              caption={`${notifications.length} total notification(s)`}
              onClick={() => setActiveTab("notifications")}
            />

            <SummaryCard
              icon={<FolderOpen size={20} />}
              title="Documents"
              value={documents.length}
              caption="Files available in self-service"
              onClick={() => setActiveTab("documents")}
            />

            <SummaryCard
              icon={<Clock3 size={20} />}
              title="Hours this month"
              value={monthHoursLabel}
              caption={`${completedAttendanceDays} completed workday(s)`}
              onClick={() => setActiveTab("attendance")}
            />

            <SummaryCard
              icon={<WalletCards size={20} />}
              title="Payroll records"
              value={payroll.length}
              caption={latestPayroll ? "Latest payroll available" : "No payroll yet"}
              onClick={() => setActiveTab("payroll")}
            />

            <SummaryCard
              icon={<Clock3 size={20} />}
              title="Today check-in"
              value={formatTime(todayAttendance?.checkInTime || todayAttendance?.checkIn)}
              caption={checkedIn ? "Attendance started" : "Not checked in yet"}
              onClick={() => setActiveTab("attendance")}
            />

            <SummaryCard
              icon={<CheckCircle2 size={20} />}
              title="Today check-out"
              value={formatTime(todayAttendance?.checkOutTime || todayAttendance?.checkOut)}
              caption={checkedOut ? workingHours : checkedIn ? "Workday in progress" : "Not available yet"}
              onClick={() => setActiveTab("attendance")}
            />

            <SummaryCard
              icon={<CalendarDays size={20} />}
              title="Pending leave"
              value={leaves.filter((leave) => upper(leave.status) === "PENDING").length}
              caption="Requests waiting for action"
              onClick={() => setActiveTab("leave")}
            />

            <SummaryCard
              icon={<History size={20} />}
              title="Tracked this month"
              value={currentMonthAttendance.length}
              caption={`${completedAttendanceDays} completed · ${incompleteAttendanceDays} open`}
              onClick={() => setActiveTab("attendance")}
            />
          </div>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Quick actions</h3>
                <p>Common employee self-service actions.</p>
              </div>
            </div>

            <div className="ews-quick-grid">
              {!checkedIn ? (
                <QuickAction
                  icon={<Clock3 size={20} />}
                  title="Check in"
                  description="Start today's attendance"
                  onClick={handleCheckIn}
                  disabled={actionLoading}
                />
              ) : !checkedOut ? (
                <QuickAction
                  icon={<Clock3 size={20} />}
                  title="Check out"
                  description="Complete today's attendance"
                  onClick={handleCheckOut}
                  disabled={actionLoading}
                />
              ) : (
                <QuickAction
                  icon={<CheckCircle2 size={20} />}
                  title="Attendance done"
                  description="Today's attendance is complete"
                  onClick={() => setActiveTab("attendance")}
                />
              )}

              <QuickAction
                icon={<CalendarDays size={20} />}
                title="Apply leave"
                description="Create a leave request"
                onClick={() => setActiveTab("leave")}
              />

              <QuickAction
                icon={<CreditCard size={20} />}
                title="View payroll"
                description="Salary and payroll history"
                onClick={() => setActiveTab("payroll")}
              />

              <QuickAction
                icon={<User size={20} />}
                title="My profile"
                description="View employee information"
                onClick={() => setActiveTab("profile")}
              />
            </div>
          </section>

          <section className="ews-card" style={{marginBottom:16}}>
            <div className="ews-card-heading">
              <div>
                <h3>My HR snapshot</h3>
                <p>Important employment and self-service information in one place.</p>
              </div>
              <IdCard size={21} />
            </div>
            <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(170px,1fr))",gap:12}}>
              <div className="ews-today-status"><div><span>Employee ID</span><strong>{safeText(profile?.employeeId)}</strong></div></div>
              <div className="ews-today-status"><div><span>Department</span><strong>{safeText(profile?.department)}</strong></div></div>
              <div className="ews-today-status"><div><span>Job title</span><strong>{safeText(profile?.jobTitle)}</strong></div></div>
              <div className="ews-today-status"><div><span>Joined</span><strong>{formatDate(profile?.joiningDate)}</strong></div></div>
              <div className="ews-today-status"><div><span>Active goals</span><strong>{performanceGoals.filter((goal)=>upper(goal.status)==="ACTIVE").length}</strong></div></div>
              <div className="ews-today-status"><div><span>Next approved leave</span><strong>{approvedUpcomingLeaves.length ? formatShortDate(approvedUpcomingLeaves[0].startDate) : "None"}</strong></div></div>
            </div>
          </section>

          <div className="ews-dashboard-grid">
            <section className="ews-card">
              <div className="ews-card-heading">
                <div>
                  <h3>Today's attendance</h3>
                  <p>Live attendance status for today.</p>
                </div>

                <Clock3 size={21} />
              </div>

              <div className="ews-today-status">
                <div>
                  <span>Status</span>
                  <strong>
                    {safeText(
                      todayAttendance?.status,
                      checkedIn ? "PRESENT" : "Not checked in"
                    )}
                  </strong>
                </div>

                <div>
                  <span>Check in</span>
                  <strong>
                    {formatTime(
                      todayAttendance?.checkInTime ||
                        todayAttendance?.checkIn
                    )}
                  </strong>
                </div>

                <div>
                  <span>Check out</span>
                  <strong>
                    {formatTime(
                      todayAttendance?.checkOutTime ||
                        todayAttendance?.checkOut
                    )}
                  </strong>
                </div>

                <div>
                  <span>Working hours</span>
                  <strong>{workingHours}</strong>
                </div>
              </div>

              {!checkedIn ? (
                <button
                  type="button"
                  className="ews-btn ews-btn-primary"
                  onClick={handleCheckIn}
                  disabled={actionLoading}
                >
                  Check in
                </button>
              ) : !checkedOut ? (
                <button
                  type="button"
                  className="ews-btn ews-btn-primary"
                  onClick={handleCheckOut}
                  disabled={actionLoading}
                >
                  Check out
                </button>
              ) : (
                <div className="ews-completed">
                  <CheckCircle2 size={16} />
                  Attendance completed
                </div>
              )}
            </section>

            <section className="ews-card">
              <div className="ews-card-heading">
                <div>
                  <h3>Leave balance</h3>
                  <p>Your currently available leave.</p>
                </div>

                <CalendarDays size={21} />
              </div>

              {leaveBalances.length === 0 ? (
                <EmptyText text="No balance data." />
              ) : (
                <div className="ews-balance-list">
                  {leaveBalances.map((balance, index) => {
                    const remaining = numberValue(
                      balance.remainingDays,
                      balance.remaining,
                      balance.balance
                    );

                    const total = numberValue(
                      balance.totalDays,
                      balance.entitlement,
                      balance.allocatedDays,
                      remaining
                    );

                    const percentage =
                      total > 0
                        ? Math.min(100, Math.round((remaining / total) * 100))
                        : 0;

                    return (
                      <div
                        className="ews-balance-block"
                        key={balance.id || balance.leaveType || index}
                      >
                        <div className="ews-balance-line">
                          <strong>
                            {upper(balance.leaveType || balance.type)}
                          </strong>

                          <span>{remaining} days remaining</span>
                        </div>

                        <ProgressBar value={percentage} />
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="ews-card">
              <div className="ews-card-heading">
                <div>
                  <h3>Recent activity</h3>
                  <p>Your latest account activity.</p>
                </div>

                <History size={21} />
              </div>

              {recentActivity.length === 0 ? (
                <EmptyText text="No recent activity." />
              ) : (
                <div className="ews-activity-list">
                  {recentActivity.map((item) => (
                    <div className="ews-activity-item" key={item.id}>
                      <div className="ews-activity-dot" />

                      <div>
                        <strong>{item.title}</strong>
                        <span>{item.description}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="ews-card">
              <div className="ews-card-heading">
                <div>
                  <h3>Upcoming</h3>
                  <p>Upcoming approved leave and reminders.</p>
                </div>

                <CalendarDays size={21} />
              </div>

              {approvedUpcomingLeaves.length === 0 ? (
                <EmptyText text="No upcoming approved leave." />
              ) : (
                <div className="ews-upcoming-list">
                  {approvedUpcomingLeaves.slice(0, 4).map((leave) => (
                    <div className="ews-upcoming-item" key={leave.id}>
                      <div className="ews-upcoming-date">
                        <strong>{formatShortDate(leave.startDate)}</strong>
                      </div>

                      <div>
                        <strong>{safeText(leave.leaveType)} leave</strong>
                        <span>
                          {daysBetween(leave.startDate, leave.endDate)} day(s)
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Notifications</h3>
                <p>Latest alerts and announcements.</p>
              </div>

              <button
                type="button"
                className="ews-text-button"
                onClick={() => setActiveTab("notifications")}
              >
                View all
                <ChevronRight size={15} />
              </button>
            </div>

            {notifications.length === 0 ? (
              <EmptyText text="No notifications found." />
            ) : (
              <div className="ews-notification-list">
                {notifications.slice(0, 4).map((notification, index) => (
                  <NotificationItem
                    key={notification.id || index}
                    notification={notification}
                  />
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {/* ===================================================
          PROFILE
      =================================================== */}

      {activeTab === "profile" && (
        <div className="ews-profile-layout">
          <section className="ews-card ews-profile-summary">
            <div className="ews-large-avatar">
              {employeeName
                .split(" ")
                .slice(0, 2)
                .map((part) => part.charAt(0))
                .join("")
                .toUpperCase()}
            </div>

            <h3>{employeeName}</h3>
            <p>{jobTitle}</p>

            <StatusBadge value={employmentStatus} />

            <div className="ews-profile-mini">
              <span>Employee ID</span>
              <strong>{employeeId}</strong>
            </div>
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Employee information</h3>
                <p>Your NexusHR employee profile details.</p>
              </div>

              <User size={21} />
            </div>

            <div className="ews-detail-grid">
              <DetailItem
                icon={<User size={17} />}
                label="Full name"
                value={employeeName}
              />

              <DetailItem
                icon={<IdCard size={17} />}
                label="Employee ID"
                value={employeeId}
              />

              <DetailItem
                icon={<BriefcaseBusiness size={17} />}
                label="Department"
                value={department}
              />

              <DetailItem
                icon={<BriefcaseBusiness size={17} />}
                label="Job title"
                value={jobTitle}
              />

              <DetailItem
                icon={<Mail size={17} />}
                label="Email"
                value={employeeEmail}
              />

              <DetailItem
                icon={<Phone size={17} />}
                label="Phone"
                value={employeePhone}
              />

              <DetailItem
                icon={<CalendarDays size={17} />}
                label="Joining date"
                value={formatDate(joiningDate)}
              />

              <DetailItem
                icon={<CheckCircle2 size={17} />}
                label="Employment status"
                value={employmentStatus}
              />
            </div>
          </section>
        </div>
      )}

      {/* ===================================================
          ATTENDANCE
      =================================================== */}

      {activeTab === "attendance" && (
        <>
          {attendanceError && (
            <div className="ews-alert ews-alert-error" role="alert">
              <strong>Attendance unavailable.</strong> {attendanceError}
            </div>
          )}

          <div className="ews-attendance-hero">
            <div className="ews-attendance-hero-copy">
              <span className="ews-eyebrow">TODAY · {formatDate(new Date())}</span>
              <h3>{checkedOut ? "Workday completed" : checkedIn ? "You are checked in" : "Ready to start your workday?"}</h3>
              <p>Check in when you start work, check out when you finish, and keep your attendance record complete.</p>

              <div className="ews-attendance-actions">
                {!checkedIn ? (
                  <button type="button" className="ews-btn ews-btn-primary ews-btn-attendance" onClick={handleCheckIn} disabled={actionLoading}>
                    <Clock3 size={17} /> {actionLoading ? "Checking in..." : "Check in now"}
                  </button>
                ) : !checkedOut ? (
                  <button type="button" className="ews-btn ews-btn-primary ews-btn-attendance" onClick={handleCheckOut} disabled={actionLoading}>
                    <CheckCircle2 size={17} /> {actionLoading ? "Checking out..." : "Check out now"}
                  </button>
                ) : (
                  <div className="ews-completed"><CheckCircle2 size={16} /> Attendance completed for today</div>
                )}

                {!checkedOut && upper(todayAttendance?.status) !== "HALF_DAY" && (
                  <button type="button" className="ews-btn ews-btn-secondary" onClick={handleHalfDay} disabled={actionLoading}>
                    <Clock3 size={15} /> Take half day
                  </button>
                )}

                <button type="button" className="ews-btn ews-btn-secondary" onClick={() => loadWorkspace(false)} disabled={actionLoading}>
                  <RefreshCw size={15} /> Refresh
                </button>
              </div>
            </div>

            <div className="ews-attendance-clock-card">
              <div className={`ews-attendance-live-dot ${checkedIn && !checkedOut ? "is-live" : ""}`} />
              <span className="ews-attendance-clock-label">Current status</span>
              <strong>{safeText(todayAttendance?.status, checkedIn ? "PRESENT" : "NOT STARTED")}</strong>
              <small>{checkedOut ? "Attendance completed" : checkedIn ? "Work session in progress" : "No check-in recorded today"}</small>
            </div>
          </div>

          <div className="ews-summary-grid ews-attendance-summary-grid">
            <MetricMini label="Attendance rate" value={`${attendancePercentage}%`} caption="Current month" />
            <MetricMini label="Present days" value={presentDays} caption="Current month" />
            <MetricMini label="Absent / Leave" value={absentDays + leaveDaysThisMonth} caption={`${absentDays} absent · ${leaveDaysThisMonth} leave`} />
            <MetricMini label="Total worked" value={monthHoursLabel} caption={`${completedAttendanceDays} completed workday(s)`} />
            <MetricMini label="Average workday" value={averageHoursLabel} caption="Completed days only" />
            <MetricMini label="Open attendance" value={incompleteAttendanceDays} caption="Checked in without checkout" />
          </div>

          <section className="ews-card ews-attendance-today-card">
            <div className="ews-card-heading">
              <div>
                <h3>Today's timeline</h3>
                <p>Clear check-in, check-out and worked-time details for your current workday.</p>
              </div>
              <Clock3 size={21} />
            </div>

            <div className="ews-attendance-timeline">
              <div className={`ews-attendance-timeline-item ${checkedIn ? "is-complete" : ""}`}>
                <div className="ews-attendance-timeline-icon"><Clock3 size={18} /></div>
                <div><span>Check in</span><strong>{formatTime(todayAttendance?.checkInTime || todayAttendance?.checkIn)}</strong><small>{checkedIn ? "Workday started" : "Waiting for check-in"}</small></div>
              </div>

              <div className={`ews-attendance-timeline-line ${checkedIn ? "is-active" : ""}`} />

              <div className={`ews-attendance-timeline-item ${checkedOut ? "is-complete" : ""}`}>
                <div className="ews-attendance-timeline-icon"><CheckCircle2 size={18} /></div>
                <div><span>Check out</span><strong>{formatTime(todayAttendance?.checkOutTime || todayAttendance?.checkOut)}</strong><small>{checkedOut ? "Workday completed" : checkedIn ? "Checkout pending" : "Not started"}</small></div>
              </div>

              <div className="ews-attendance-duration">
                <span>Working time</span>
                <strong>{workingHours}</strong>
                <small>{checkedOut ? "Final duration for today" : checkedIn ? "Final duration appears after checkout" : "No duration yet"}</small>
              </div>
            </div>
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Date-wise attendance</h3>
                <p>Filter attendance by date range and status.</p>
              </div>

              <Clock3 size={21} />
            </div>

            <div className="ews-attendance-filters">
              <label>
                From
                <input
                  type="date"
                  value={attendanceStartDate}
                  onChange={(event) =>
                    setAttendanceStartDate(event.target.value)
                  }
                />
              </label>

              <label>
                To
                <input
                  type="date"
                  value={attendanceEndDate}
                  min={attendanceStartDate || undefined}
                  onChange={(event) =>
                    setAttendanceEndDate(event.target.value)
                  }
                />
              </label>

              <label>
                Status
                <select
                  value={attendanceStatusFilter}
                  onChange={(event) =>
                    setAttendanceStatusFilter(event.target.value)
                  }
                >
                  <option value="ALL">All</option>
                  <option value="PRESENT">Present</option>
                  <option value="ABSENT">Absent</option>
                  <option value="LATE">Late</option>
                  <option value="HALF_DAY">Half day</option>
                  <option value="LEAVE">Leave</option>
                  <option value="ON_LEAVE">On leave</option>
                </select>
              </label>

              <button
                type="button"
                className="ews-btn ews-btn-secondary ews-filter-clear"
                onClick={() => {
                  setAttendanceStartDate("");
                  setAttendanceEndDate("");
                  setAttendanceStatusFilter("ALL");
                }}
              >
                Clear filters
              </button>
            </div>

            <div className="ews-filter-result">
              Showing <strong>{filteredAttendance.length}</strong> record(s)
            </div>

            {filteredAttendance.length === 0 ? (
              <EmptyText text="No attendance records found for the selected dates." />
            ) : (
              <div className="ews-table-wrap">
                <table className="ews-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Day</th>
                      <th>Check in</th>
                      <th>Check out</th>
                      <th>Working hours</th>
                      <th>Status</th>
                      <th>Remarks</th>
                    </tr>
                  </thead>

                  <tbody>
                    {filteredAttendance.map((record, index) => {
                      const recordDate = parseDateValue(
                        extractAttendanceDate(record)
                      );

                      return (
                        <tr
                          key={
                            record.id ||
                            `${extractAttendanceDate(record)}-${index}`
                          }
                        >
                          <td>{formatDate(extractAttendanceDate(record))}</td>
                          <td>
                            {recordDate
                              ? recordDate.toLocaleDateString("en-IN", {
                                  weekday: "long",
                                })
                              : "-"}
                          </td>
                          <td>
                            {formatTime(record.checkInTime || record.checkIn)}
                          </td>
                          <td>
                            {formatTime(record.checkOutTime || record.checkOut)}
                          </td>
                          <td>
                            {formatDurationMinutes(recordWorkingMinutes(record))}
                          </td>
                          <td>
                            <StatusBadge value={record.status || "-"} />
                          </td>
                          <td>{safeText(record.remarks, "-")}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}

      {/* ===================================================
          LEAVE
      =================================================== */}

      {activeTab === "leave" && (
        <>
          <div className="ews-summary-grid">
            <MetricMini label="Leave balance" value={`${totalRemainingLeave} days`} caption="Remaining entitlement" />
            <MetricMini label="Pending" value={pendingLeaveCount} caption="Waiting for approval" />
            <MetricMini label="Approved" value={approvedLeaveCount} caption="Approved requests" />
            <MetricMini label="Rejected / Cancelled" value={rejectedLeaveCount} caption="Closed without approval" />
          </div>

          <div className="ews-section-grid">
          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Request leave</h3>
                <p>Submit a new leave request securely.</p>
              </div>

              <CalendarDays size={21} />
            </div>

            <form className="ews-form" onSubmit={submitLeave}>
              <label>
                Leave type
                <select
                  name="leaveType"
                  value={leaveForm.leaveType}
                  onChange={handleLeaveInput}
                  required
                >
                  <option value="CASUAL">Casual</option>
                  <option value="SICK">Sick</option>
                  <option value="ANNUAL">Annual</option>
                  <option value="MATERNITY">Maternity</option>
                  <option value="PATERNITY">Paternity</option>
                  <option value="UNPAID">Unpaid leave</option>
                </select>
              </label>

              <div className="ews-form-row">
                <label>
                  Start date
                  <input
                    type="date"
                    name="startDate"
                    value={leaveForm.startDate}
                    onChange={handleLeaveInput}
                    required
                  />
                </label>

                <label>
                  End date
                  <input
                    type="date"
                    name="endDate"
                    value={leaveForm.endDate}
                    min={leaveForm.startDate || undefined}
                    onChange={handleLeaveInput}
                    required
                  />
                </label>
              </div>

              {leaveForm.startDate && leaveForm.endDate && (
                <div className="ews-duration-box">
                  Requested duration:{" "}
                  <strong>
                    {daysBetween(leaveForm.startDate, leaveForm.endDate)} day(s)
                  </strong>
                </div>
              )}

              <label>
                Reason
                <textarea
                  name="reason"
                  rows="4"
                  value={leaveForm.reason}
                  onChange={handleLeaveInput}
                  placeholder="Enter reason for leave"
                  required
                />
              </label>

              <button
                type="submit"
                className="ews-btn ews-btn-primary"
                disabled={actionLoading}
              >
                {actionLoading && (
                  <Loader2 size={15} className="ews-spin" />
                )}
                Submit request
              </button>
            </form>
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Leave balance</h3>
                <p>Your available leave entitlement.</p>
              </div>
            </div>

            {leaveBalances.length === 0 ? (
              <EmptyText text="No balance data." />
            ) : (
              <div className="ews-balance-list">
                {leaveBalances.map((balance, index) => {
                  const remaining = numberValue(
                    balance.remainingDays,
                    balance.remaining,
                    balance.balance
                  );

                  const total = numberValue(
                    balance.totalDays,
                    balance.entitlement,
                    balance.allocatedDays,
                    remaining
                  );

                  const used = Math.max(0, total - remaining);
                  const percentage =
                    total > 0
                      ? Math.min(100, Math.round((remaining / total) * 100))
                      : 0;

                  return (
                    <div
                      className="ews-leave-card"
                      key={balance.id || balance.leaveType || index}
                    >
                      <div className="ews-balance-line">
                        <strong>
                          {upper(balance.leaveType || balance.type)}
                        </strong>
                        <span>{remaining} days remaining</span>
                      </div>

                      <ProgressBar value={percentage} />

                      <div className="ews-leave-meta">
                        <span>Used: {used}</span>
                        <span>Total: {total}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          <section className="ews-card ews-card-full">
            <div className="ews-card-heading">
              <div>
                <h3>My leave requests</h3>
                <p>Track submitted requests and approval status.</p>
              </div>

              <span className="ews-counter">{leaves.length} total</span>
            </div>

            {leaves.length === 0 ? (
              <EmptyText text="No leave requests found." />
            ) : (
              <div className="ews-table-wrap">
                <table className="ews-table">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Start</th>
                      <th>End</th>
                      <th>Duration</th>
                      <th>Reason</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>

                  <tbody>
                    {leaves.map((leave, index) => {
                      const status = upper(leave.status);

                      return (
                        <tr key={leave.id || index}>
                          <td>{safeText(leave.leaveType || leave.type)}</td>
                          <td>{formatDate(leave.startDate)}</td>
                          <td>{formatDate(leave.endDate)}</td>
                          <td>
                            {daysBetween(leave.startDate, leave.endDate)} day(s)
                          </td>
                          <td>{safeText(leave.reason)}</td>
                          <td>
                            <StatusBadge value={leave.status} />
                          </td>
                          <td>
                            {status === "PENDING" ? (
                              <button
                                type="button"
                                className="ews-link-danger"
                                disabled={actionLoading}
                                onClick={() => cancelLeave(leave.id)}
                              >
                                Cancel
                              </button>
                            ) : (
                              "-"
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          </div>
        </>
      )}

      {/* ===================================================
          PAYROLL
      =================================================== */}

      {activeTab === "payroll" && (
        <>
          <div className="ews-summary-grid ews-summary-grid-three">
            <MetricMini
              label="Net salary"
              value={
                latestPayroll
                  ? formatMoney(
                      latestPayroll.netSalary ??
                        latestPayroll.netPay ??
                        latestPayroll.netAmount
                    )
                  : "-"
              }
              caption="Latest payroll"
            />

            <MetricMini
              label="Gross salary"
              value={
                latestPayroll
                  ? formatMoney(
                      latestPayroll.grossSalary ??
                        latestPayroll.grossPay ??
                        numberValue(
                          latestPayroll.basicSalary,
                          latestPayroll.basic
                        ) +
                          numberValue(
                            latestPayroll.allowances,
                            latestPayroll.totalAllowances
                          )
                    )
                  : "-"
              }
              caption="Before deductions"
            />

            <MetricMini
              label="Total deductions"
              value={
                latestPayroll
                  ? formatMoney(
                      numberValue(latestPayroll.deductions, latestPayroll.totalDeductions) +
                        numberValue(latestPayroll.providentFund) +
                        numberValue(latestPayroll.professionalTax) +
                        numberValue(latestPayroll.otherDeductions) +
                        numberValue(latestPayroll.tax, latestPayroll.taxAmount)
                    )
                  : "-"
              }
              caption="PF, tax and deductions"
            />

            <MetricMini
              label="Provident fund"
              value={latestPayroll ? formatMoney(latestPayroll.providentFund) : "-"}
              caption="Latest payroll"
            />

            <MetricMini
              label="Bonus + overtime"
              value={
                latestPayroll
                  ? formatMoney(
                      numberValue(latestPayroll.bonus) +
                        numberValue(latestPayroll.overtimePay)
                    )
                  : "-"
              }
              caption="Variable earnings"
            />

            <MetricMini
              label="Payment status"
              value={safeText(latestPayroll?.status, "-")}
              caption={safeText(latestPayroll?.paymentDate, "Not paid yet")}
            />
          </div>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>My payroll</h3>
                <p>Salary history, deductions and payment status.</p>
              </div>

              <CreditCard size={21} />
            </div>

            {payroll.length === 0 ? (
              <EmptyText text="No payroll records found." />
            ) : (
              <div className="ews-table-wrap">
                <table className="ews-table">
                  <thead>
                    <tr>
                      <th>Period</th>
                      <th>Basic salary</th>
                      <th>Gross</th>
                      <th>PF</th>
                      <th>Tax</th>
                      <th>Total deductions</th>
                      <th>Net salary</th>
                      <th>Status / payment</th>
                      <th>Payslip</th>
                    </tr>
                  </thead>

                  <tbody>
                    {payroll.map((item, index) => (
                      <tr key={item.id || index}>
                        <td>
                          {safeText(
                            item.payrollMonth ||
                              item.month ||
                              item.payPeriod ||
                              item.period
                          )}
                        </td>

                        <td>
                          {formatMoney(item.basicSalary ?? item.basic)}
                        </td>

                        <td>
                          {formatMoney(
                            item.grossSalary ??
                              numberValue(item.basicSalary, item.basic) +
                                numberValue(item.allowances, item.totalAllowances) +
                                numberValue(item.bonus) +
                                numberValue(item.overtimePay)
                          )}
                        </td>

                        <td>{formatMoney(item.providentFund)}</td>

                        <td>{formatMoney(item.tax ?? item.taxAmount)}</td>

                        <td>
                          {formatMoney(
                            numberValue(item.deductions, item.totalDeductions) +
                              numberValue(item.providentFund) +
                              numberValue(item.professionalTax) +
                              numberValue(item.otherDeductions) +
                              numberValue(item.tax, item.taxAmount)
                          )}
                        </td>

                        <td>
                          <strong>{formatMoney(item.netSalary ?? item.netPay)}</strong>
                        </td>

                        <td>
                          <StatusBadge value={item.status || "-"} />
                          <div style={{ marginTop: 4, fontSize: 11, color: "#64748b" }}>
                            {safeText(item.paymentDate, "Pending payment")}
                            {item.paymentReference ? ` · ${item.paymentReference}` : ""}
                          </div>
                        </td>

                        <td>
                          <div className="ews-action-row">
                            <button
                              type="button"
                              className="ews-btn ews-btn-secondary ews-btn-small"
                              onClick={() => openPayslip(item)}
                            >
                              <FileText size={14} />
                              Preview
                            </button>

                            <button
                              type="button"
                              className="ews-btn ews-btn-primary ews-btn-small"
                              onClick={() => downloadPayslip(item)}
                              disabled={actionLoading}
                            >
                              <Download size={14} />
                              Download PDF
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <div className="ews-info-note">
            <FileText size={18} />
            <div>
              <strong>Secure payslip download</strong>
              <span>
                Download the server-generated PDF for your own payroll record,
                or preview it before printing.
              </span>
            </div>
          </div>
        </>
      )}

      {/* ===================================================
          DOCUMENTS
      =================================================== */}

      {activeTab === "documents" && (
        <div className="ews-section-grid">
          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Upload document</h3>
                <p>Store your own employee documents securely in NexusHR.</p>
              </div>

              <UploadCloud size={21} />
            </div>

            <form className="ews-form" onSubmit={uploadDocument}>
              <label>
                Document name
                <input
                  type="text"
                  name="documentName"
                  value={documentForm.documentName}
                  onChange={handleDocumentInput}
                  placeholder="Example: Aadhaar Card, Degree Certificate"
                  required
                />
              </label>

              <label>
                Document type
                <select
                  name="documentType"
                  value={documentForm.documentType}
                  onChange={handleDocumentInput}
                  required
                >
                  <option value="IDENTITY">Identity</option>
                  <option value="EDUCATION">Education</option>
                  <option value="CERTIFICATE">Certificate</option>
                  <option value="OFFER_LETTER">Offer Letter</option>
                  <option value="EXPERIENCE">Experience</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>

              <label>
                File
                <input
                  id="ews-document-file"
                  type="file"
                  name="file"
                  onChange={handleDocumentInput}
                  accept=".pdf,.png,.jpg,.jpeg,.doc,.docx"
                  required
                />
              </label>

              <div className="ews-duration-box">
                Maximum file size: 10 MB.
              </div>

              <button
                type="submit"
                className="ews-btn ews-btn-primary"
                disabled={documentUploading}
              >
                {documentUploading ? (
                  <Loader2 size={15} className="ews-spin" />
                ) : (
                  <UploadCloud size={15} />
                )}
                {documentUploading ? "Uploading..." : "Upload document"}
              </button>
            </form>
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Document summary</h3>
                <p>Your currently stored employee documents.</p>
              </div>

              <FolderOpen size={21} />
            </div>

            <div className="ews-document-stats">
              <MetricMini
                label="Documents"
                value={documents.length}
                caption="Stored in your profile"
              />

              <MetricMini
                label="PDF files"
                value={
                  documents.filter((item) =>
                    String(item.contentType || "")
                      .toLowerCase()
                      .includes("pdf")
                  ).length
                }
                caption="Portable documents"
              />
            </div>
          </section>

          <section className="ews-card ews-card-full">
            <div className="ews-card-heading">
              <div>
                <h3>My documents</h3>
                <p>Download or remove documents that belong to your account.</p>
              </div>

              <span className="ews-counter">{documents.length} total</span>
            </div>

            {documents.length === 0 ? (
              <EmptyText text="No employee documents uploaded yet." />
            ) : (
              <div className="ews-table-wrap">
                <table className="ews-table">
                  <thead>
                    <tr>
                      <th>Document</th>
                      <th>Type</th>
                      <th>File</th>
                      <th>Size</th>
                      <th>Uploaded</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {documents.map((item, index) => (
                      <tr key={item.id || index}>
                        <td>
                          <strong>{safeText(item.documentName)}</strong>
                        </td>

                        <td>
                          <StatusBadge value={item.documentType || "OTHER"} />
                        </td>

                        <td>{safeText(item.fileName)}</td>

                        <td>
                          {item.fileSize
                            ? `${(Number(item.fileSize) / 1024).toFixed(
                                Number(item.fileSize) >= 1024 * 1024 ? 1 : 0
                              )} KB`
                            : "-"}
                        </td>

                        <td>{formatDate(item.uploadedAt)}</td>

                        <td>
                          <div className="ews-action-row">
                            <button
                              type="button"
                              className="ews-btn ews-btn-secondary ews-btn-small"
                              onClick={() => downloadDocument(item)}
                            >
                              <Download size={14} />
                              Download
                            </button>

                            <button
                              type="button"
                              className="ews-btn ews-btn-danger ews-btn-small"
                              onClick={() => deleteDocument(item)}
                              disabled={actionLoading}
                            >
                              <Trash2 size={14} />
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}

      {/* ===================================================
          PERFORMANCE
      =================================================== */}

      {activeTab === "performance" && (
        <>
          <div className="ews-summary-grid ews-summary-grid-three">
            <MetricMini
              label="Latest rating"
              value={performanceRating || "-"}
              caption="Most recent review"
            />

            <MetricMini
              label="Final score"
              value={
                latestPerformance?.finalScore != null
                  ? `${Number(latestPerformance.finalScore).toFixed(1)}%`
                  : "-"
              }
              caption="Weighted score"
            />

            <MetricMini
              label="Active goals"
              value={performanceGoals.filter((goal) => upper(goal.status) === "ACTIVE").length}
              caption={`${performanceGoals.length} total goals`}
            />

            <MetricMini
              label="Goal progress"
              value={
                performanceGoals.length
                  ? `${Math.round(
                      performanceGoals.reduce(
                        (sum, goal) => sum + numberValue(goal.progress),
                        0
                      ) / performanceGoals.length
                    )}%`
                  : "-"
              }
              caption="Average completion"
            />

            <MetricMini
              label="360 feedback"
              value={performanceFeedback.length}
              caption="Self, manager, peer and subordinate"
            />

            <MetricMini
              label="Review status"
              value={humanizeStatus(latestPerformance?.status, "-")}
              caption={safeText(latestPerformance?.reviewCycle, "Latest review")}
            />
          </div>

          <section className="ews-card ews-performance-insights">
            <div className="ews-card-heading">
              <div>
                <h3>Performance insights</h3>
                <p>
                  A current snapshot of review history, goal delivery and feedback trends.
                </p>
              </div>
              <TrendingUp size={21} />
            </div>

            <div className="ews-performance-insight-grid">
              <div className="ews-performance-insight">
                <span>Average rating</span>
                <strong>
                  {averagePerformanceRating
                    ? `${averagePerformanceRating.toFixed(1)}/5`
                    : "-"}
                </strong>
                <small>{completedPerformanceReviews} completed review(s)</small>
              </div>

              <div className="ews-performance-insight">
                <span>Goal completion</span>
                <strong>
                  {performanceGoals.length
                    ? `${Math.round(
                        (completedPerformanceGoals.length /
                          performanceGoals.length) *
                          100
                      )}%`
                    : "-"}
                </strong>
                <small>
                  {completedPerformanceGoals.length} completed ·{" "}
                  {activePerformanceGoals.length} active
                </small>
              </div>

              <div className="ews-performance-insight">
                <span>Average goal progress</span>
                <strong>
                  {performanceGoals.length ? `${averageGoalProgress}%` : "-"}
                </strong>
                <small>Across {performanceGoals.length} assigned goal(s)</small>
              </div>

              <div className="ews-performance-insight">
                <span>Feedback rating</span>
                <strong>
                  {averageFeedbackRating
                    ? `${averageFeedbackRating.toFixed(1)}/5`
                    : "-"}
                </strong>
                <small>{performanceFeedback.length} feedback response(s)</small>
              </div>
            </div>

            <div className="ews-performance-dashboard-grid">
              <div className="ews-performance-panel">
                <div className="ews-performance-panel-head">
                  <div>
                    <span>Review trend</span>
                    <strong>Recent performance score</strong>
                  </div>
                  {latestPerformanceTrendDelta != null && (
                    <span
                      className={`ews-trend-pill ${
                        latestPerformanceTrendDelta > 0
                          ? "positive"
                          : latestPerformanceTrendDelta < 0
                          ? "negative"
                          : "neutral"
                      }`}
                    >
                      {latestPerformanceTrendDelta > 0 ? "+" : ""}
                      {latestPerformanceTrendDelta}% vs previous
                    </span>
                  )}
                </div>

                {performanceTrend.length === 0 ? (
                  <EmptyText text="No review trend available yet." />
                ) : (
                  <div className="ews-trend-list">
                    {performanceTrend.map((item) => (
                      <div className="ews-trend-row" key={item.id}>
                        <div className="ews-trend-meta">
                          <span>{item.label}</span>
                          <strong>
                            {item.score ? `${item.score.toFixed(0)}%` : "-"}
                          </strong>
                        </div>
                        <div className="ews-trend-track">
                          <div
                            className="ews-trend-fill"
                            style={{ width: `${item.score}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="ews-performance-panel">
                <div className="ews-performance-panel-head">
                  <div>
                    <span>Latest review</span>
                    <strong>
                      {safeText(
                        latestPerformance?.reviewCycle,
                        "No active review cycle"
                      )}
                    </strong>
                  </div>
                  {latestPerformance && (
                    <StatusBadge value={latestPerformance.status || "DRAFT"} />
                  )}
                </div>

                <div className="ews-latest-review-grid">
                  <div>
                    <span>Review type</span>
                    <strong>
                      {humanizeStatus(
                        latestPerformance?.reviewType,
                        "-"
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>Rating</span>
                    <strong>
                      {performanceRating ? `${performanceRating}/5` : "-"}
                    </strong>
                  </div>

                  <div>
                    <span>Final score</span>
                    <strong>
                      {latestPerformance?.finalScore != null
                        ? `${Number(latestPerformance.finalScore).toFixed(1)}%`
                        : "Pending"}
                    </strong>
                  </div>

                  <div>
                    <span>Period</span>
                    <strong>
                      {latestPerformance
                        ? `${formatShortDate(
                            latestPerformance.reviewPeriodStart
                          )} - ${formatShortDate(
                            latestPerformance.reviewPeriodEnd
                          )}`
                        : "-"}
                    </strong>
                  </div>
                </div>

                {latestPerformance?.managerComments && (
                  <div className="ews-manager-note">
                    <span>Latest manager feedback</span>
                    <p>{latestPerformance.managerComments}</p>
                  </div>
                )}
              </div>
            </div>
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>My goals</h3>
                <p>Track objectives, KPIs, due dates and progress.</p>
              </div>
              <Target size={21} />
            </div>

            {performanceGoals.length === 0 ? (
              <EmptyText text="No performance goals assigned yet." />
            ) : (
              <div className="ews-performance-grid">
                {performanceGoals.map((goal, index) => (
                  <article className="ews-review-card" key={goal.id || index}>
                    <div className="ews-review-top">
                      <div>
                        <span>Goal</span>
                        <strong>{safeText(goal.title, "Untitled goal")}</strong>
                      </div>
                      <StatusBadge value={goal.status || "DRAFT"} />
                    </div>

                    <div className="ews-review-feedback">
                      <span>Progress</span>
                      <strong>{numberValue(goal.progress)}%</strong>
                    </div>
                    <ProgressBar value={numberValue(goal.progress)} />

                    {upper(goal.status) !== "COMPLETED" && upper(goal.status) !== "CANCELLED" && (
                      <div className="ews-action-row" style={{ marginTop: 10 }}>
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={goalProgressDrafts[goal.id] ?? numberValue(goal.progress)}
                          onChange={(event) =>
                            setGoalProgressDrafts((current) => ({
                              ...current,
                              [goal.id]: event.target.value,
                            }))
                          }
                          style={{ width: 90 }}
                          aria-label={`Progress for ${safeText(goal.title, "goal")}`}
                        />
                        <button
                          type="button"
                          className="ews-btn ews-btn-secondary ews-btn-small"
                          onClick={() => saveMyGoalProgress(goal)}
                          disabled={actionLoading}
                        >
                          Save progress
                        </button>
                      </div>
                    )}

                    <div className="ews-review-feedback">
                      <span>Success metric</span>
                      <p>{safeText(goal.successMetric, "No KPI defined.")}</p>
                    </div>

                    <div className="ews-review-feedback">
                      <span>Target date</span>
                      <p>{formatDate(goal.targetDate)}</p>
                    </div>

                    {goal.managerComment && (
                      <div className="ews-review-feedback">
                        <span>Manager comment</span>
                        <p>{goal.managerComment}</p>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Performance reviews</h3>
                <p>Ratings, weighted scores, strengths and development feedback.</p>
              </div>
              <TrendingUp size={21} />
            </div>

            {performance.length === 0 ? (
              <EmptyText text="No performance reviews found." />
            ) : (
              <div className="ews-performance-grid">
                {performance.map((review, index) => {
                  const rating = numberValue(
                    review.rating,
                    review.overallRating,
                    review.performanceScore,
                    review.score
                  );
                  const progress = numberValue(
                    review.finalScore,
                    rating > 0 && rating <= 5 ? rating * 20 : rating
                  );

                  return (
                    <article className="ews-review-card" key={review.id || index}>
                      <div className="ews-review-top">
                        <div>
                          <span>{safeText(review.reviewType, "Review")}</span>
                          <strong>
                            {safeText(
                              review.reviewCycle,
                              `${safeText(review.reviewPeriodStart)} - ${safeText(
                                review.reviewPeriodEnd
                              )}`
                            )}
                          </strong>
                        </div>
                        <StatusBadge value={review.status || "DRAFT"} />
                      </div>

                      <div className="ews-review-rating">
                        <Star size={18} />
                        <strong>{rating || "-"}/5</strong>
                      </div>

                      {progress > 0 && <ProgressBar value={progress} />}

                      <div className="ews-review-feedback">
                        <span>Final score</span>
                        <p>{review.finalScore != null ? `${Number(review.finalScore).toFixed(1)}%` : "Pending"}</p>
                      </div>

                      <div className="ews-review-feedback">
                        <span>Strengths</span>
                        <p>{safeText(review.strengths, "No strengths recorded yet.")}</p>
                      </div>

                      <div className="ews-review-feedback">
                        <span>Development areas</span>
                        <p>{safeText(review.areasForImprovement, "No development areas recorded.")}</p>
                      </div>

                      <div className="ews-review-feedback">
                        <span>Manager comments</span>
                        <p>{safeText(review.managerComments, "No manager comments yet.")}</p>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>360-degree feedback</h3>
                <p>Feedback received from different review perspectives.</p>
              </div>
              <Star size={21} />
            </div>

            {performanceFeedback.length === 0 ? (
              <EmptyText text="No 360-degree feedback available yet." />
            ) : (
              <div className="ews-performance-grid">
                {performanceFeedback.map((item, index) => (
                  <article className="ews-review-card" key={item.id || index}>
                    <div className="ews-review-top">
                      <div>
                        <span>{safeText(item.feedbackType, "FEEDBACK")}</span>
                        <strong>{safeText(item.reviewer?.firstName, "Reviewer")} {safeText(item.reviewer?.lastName, "")}</strong>
                      </div>
                      <div className="ews-review-rating">
                        <Star size={16} />
                        <strong>{numberValue(item.rating)}/5</strong>
                      </div>
                    </div>
                    <div className="ews-review-feedback"><span>Comments</span><p>{safeText(item.comments, "No comments.")}</p></div>
                    <div className="ews-review-feedback"><span>Strengths</span><p>{safeText(item.strengths, "-")}</p></div>
                    <div className="ews-review-feedback"><span>Improve</span><p>{safeText(item.areasForImprovement, "-")}</p></div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="ews-card">
            <div className="ews-card-heading">
              <div>
                <h3>Submit self feedback</h3>
                <p>Add your own review perspective for an active performance cycle.</p>
              </div>
              <Star size={21} />
            </div>

            <form className="ews-form" onSubmit={submitMySelfFeedback}>
              <label>
                Review
                <select
                  value={selfFeedbackForm.reviewId}
                  onChange={(event) =>
                    setSelfFeedbackForm((current) => ({
                      ...current,
                      reviewId: event.target.value,
                    }))
                  }
                  required
                >
                  <option value="">Select review</option>
                  {performance.map((review) => (
                    <option key={review.id} value={review.id}>
                      {safeText(review.reviewCycle, `Review #${review.id}`)} — {safeText(review.status)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Self rating
                <select
                  value={selfFeedbackForm.rating}
                  onChange={(event) =>
                    setSelfFeedbackForm((current) => ({
                      ...current,
                      rating: event.target.value,
                    }))
                  }
                >
                  <option value="1">1 - Needs improvement</option>
                  <option value="2">2 - Developing</option>
                  <option value="3">3 - Meets expectations</option>
                  <option value="4">4 - Exceeds expectations</option>
                  <option value="5">5 - Outstanding</option>
                </select>
              </label>

              <label>
                Comments
                <textarea
                  value={selfFeedbackForm.comments}
                  onChange={(event) =>
                    setSelfFeedbackForm((current) => ({
                      ...current,
                      comments: event.target.value,
                    }))
                  }
                  placeholder="Summarize your contribution and outcomes."
                />
              </label>

              <label>
                Strengths
                <textarea
                  value={selfFeedbackForm.strengths}
                  onChange={(event) =>
                    setSelfFeedbackForm((current) => ({
                      ...current,
                      strengths: event.target.value,
                    }))
                  }
                  placeholder="What went well?"
                />
              </label>

              <label>
                Areas for improvement
                <textarea
                  value={selfFeedbackForm.areasForImprovement}
                  onChange={(event) =>
                    setSelfFeedbackForm((current) => ({
                      ...current,
                      areasForImprovement: event.target.value,
                    }))
                  }
                  placeholder="What would you improve next cycle?"
                />
              </label>

              <button
                type="submit"
                className="ews-btn ews-btn-primary"
                disabled={actionLoading || performance.length === 0}
              >
                Submit self feedback
              </button>
            </form>
          </section>
        </>
      )}

      {/* ===================================================
          NOTIFICATIONS
      =================================================== */}

      {activeTab === "notifications" && (
        <>
          <div className="ews-summary-grid ews-summary-grid-three">
            <MetricMini label="Total notifications" value={notifications.length} caption="All messages" />
            <MetricMini label="Unread" value={unreadCount} caption="Needs your attention" />
            <MetricMini label="Read" value={Math.max(0, notifications.length - unreadCount)} caption="Already reviewed" />
          </div>

          <section className="ews-card">
          <div className="ews-card-heading">
            <div>
              <h3>Notifications</h3>
              <p>Your alerts, reminders and announcements.</p>
            </div>

            {unreadCount > 0 && (
              <button
                type="button"
                className="ews-btn ews-btn-secondary"
                onClick={markAllNotificationsRead}
                disabled={actionLoading}
              >
                Mark all as read
              </button>
            )}
          </div>

          <div className="ews-filter-row">
            {[
              ["ALL", `All (${notifications.length})`],
              ["UNREAD", `Unread (${unreadCount})`],
              ["READ", "Read"],
            ].map(([key, label]) => (
              <button
                type="button"
                key={key}
                className={
                  notificationFilter === key
                    ? "ews-filter active"
                    : "ews-filter"
                }
                onClick={() => setNotificationFilter(key)}
              >
                {label}
              </button>
            ))}
          </div>

          {filteredNotifications.length === 0 ? (
            <EmptyText text="No notifications in this filter." />
          ) : (
            <div className="ews-notification-list">
              {filteredNotifications.map((notification, index) => (
                <NotificationItem
                  key={notification.id || index}
                  notification={notification}
                />
              ))}
            </div>
          )}
          </section>
        </>
      )}

      {selectedPayslip && (
        <div className="ews-modal-backdrop" onClick={closePayslip}>
          <div
            className="ews-payslip-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="ews-payslip-header">
              <div>
                <span>NEXUSHR</span>
                <h3>Employee Payslip</h3>
                <p>Payroll period: {safeText(
                  selectedPayslip.payrollMonth ||
                    selectedPayslip.month ||
                    selectedPayslip.payPeriod ||
                    selectedPayslip.period
                )}</p>
              </div>

              <StatusBadge value={selectedPayslip.status || "-"} />
            </div>

            <div className="ews-payslip-employee">
              <DetailItem icon={<User size={17} />} label="Employee" value={employeeName} />
              <DetailItem icon={<IdCard size={17} />} label="Employee ID" value={employeeId} />
              <DetailItem icon={<BriefcaseBusiness size={17} />} label="Department" value={department} />
              <DetailItem icon={<BriefcaseBusiness size={17} />} label="Job title" value={jobTitle} />
            </div>

            <div className="ews-payslip-lines">
              <PayslipLine label="Basic salary" value={formatMoney(selectedPayslip.basicSalary ?? selectedPayslip.basic)} />
              <PayslipLine label="Allowances" value={formatMoney(selectedPayslip.allowances ?? selectedPayslip.totalAllowances)} />
              <PayslipLine
                label="Gross salary"
                value={formatMoney(
                  selectedPayslip.grossSalary ??
                    selectedPayslip.grossPay ??
                    (numberValue(selectedPayslip.basicSalary, selectedPayslip.basic) +
                      numberValue(selectedPayslip.allowances, selectedPayslip.totalAllowances))
                )}
              />
              <PayslipLine label="Deductions" value={formatMoney(selectedPayslip.deductions ?? selectedPayslip.totalDeductions)} />
              <PayslipLine label="Tax" value={formatMoney(selectedPayslip.tax ?? selectedPayslip.taxAmount)} />
              <PayslipLine
                label="Net salary"
                value={formatMoney(selectedPayslip.netSalary ?? selectedPayslip.netPay)}
                strong
              />
            </div>

            <div className="ews-payslip-actions">
              <button
                type="button"
                className="ews-btn ews-btn-primary"
                onClick={() => downloadPayslip(selectedPayslip)}
                disabled={actionLoading}
              >
                <Download size={15} />
                Download PDF
              </button>

              <button
                type="button"
                className="ews-btn ews-btn-secondary"
                onClick={() => printPayslip(selectedPayslip)}
              >
                <FileText size={15} />
                Print / Save PDF
              </button>

              <button
                type="button"
                className="ews-btn ews-btn-secondary"
                onClick={closePayslip}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      <WorkspaceStyles />
    </div>
  );
}

/* =========================================================
   SUB COMPONENTS
========================================================= */

function SummaryCard({
  icon,
  title,
  value,
  caption,
  progress,
  onClick,
}) {
  return (
    <button
      type="button"
      className="ews-summary-card"
      onClick={onClick}
    >
      <div className="ews-summary-icon">{icon}</div>

      <div className="ews-summary-copy">
        <span>{title}</span>
        <strong>{value}</strong>
        <small>{caption}</small>

        {typeof progress === "number" && (
          <ProgressBar value={progress} compact />
        )}
      </div>

      <ChevronRight className="ews-summary-arrow" size={18} />
    </button>
  );
}

function MetricMini({ label, value, caption }) {
  return (
    <div className="ews-metric-mini">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{caption}</small>
    </div>
  );
}

function QuickAction({
  icon,
  title,
  description,
  onClick,
  disabled,
}) {
  return (
    <button
      type="button"
      className="ews-quick-action"
      onClick={onClick}
      disabled={disabled}
    >
      <div className="ews-quick-icon">{icon}</div>

      <div>
        <strong>{title}</strong>
        <span>{description}</span>
      </div>

      <ChevronRight size={17} />
    </button>
  );
}

function DetailItem({ icon, label, value }) {
  return (
    <div className="ews-detail-item">
      <div className="ews-detail-icon">{icon}</div>

      <div>
        <span>{label}</span>
        <strong>{safeText(value)}</strong>
      </div>
    </div>
  );
}

function StatusBadge({ value }) {
  return (
    <span className={`ews-status ${statusClass(value)}`}>
      {safeText(value)}
    </span>
  );
}

function EmptyText({ text }) {
  return <div className="ews-empty">{text}</div>;
}

function ProgressBar({ value, compact = false }) {
  const safeValue = Math.min(100, Math.max(0, Number(value) || 0));

  return (
    <div
      className={compact ? "ews-progress compact" : "ews-progress"}
      aria-label={`${safeValue}%`}
    >
      <div
        className="ews-progress-fill"
        style={{ width: `${safeValue}%` }}
      />
    </div>
  );
}

function NotificationItem({ notification }) {
  const unread = notificationIsUnread(notification);

  return (
    <div
      className={`ews-notification-item ${
        unread ? "unread" : ""
      }`}
    >
      <div className="ews-notification-icon">
        <Bell size={17} />
      </div>

      <div className="ews-notification-content">
        <div className="ews-notification-top">
          <strong>
            {safeText(notification.title, "Notification")}
          </strong>

          {unread && <span className="ews-unread-dot" />}
        </div>

        <p>{safeText(notification.message, "")}</p>

        <small style={{ display: "block", marginBottom: "4px", fontWeight: 700 }}>
          Sent by @{safeText(notification.senderUsername, "SYSTEM")} · {safeText(notification.senderRole, "SYSTEM")}
        </small>

        <small>
          {formatDate(
            notification.createdAt ||
              notification.createdDate ||
              notification.timestamp
          )}
        </small>
      </div>
    </div>
  );
}

function PayslipLine({ label, value, strong = false }) {
  return (
    <div className={strong ? "ews-payslip-line strong" : "ews-payslip-line"}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

/* =========================================================
   STYLES
========================================================= */

function WorkspaceStyles() {
  return (
    <style>{`
      .ews {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 18px;
        color: #172033;
      }

      .ews * {
        box-sizing: border-box;
      }

      .ews-hero {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 24px;
        padding: 26px;
        background:
          linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(255, 255, 255, 0.95)),
          #ffffff;
        border: 1px solid #dbe7fb;
        border-radius: 20px;
        box-shadow: 0 8px 28px rgba(15, 23, 42, 0.05);
      }

      .ews-hero-main {
        display: flex;
        align-items: center;
        gap: 17px;
      }

      .ews-avatar,
      .ews-large-avatar {
        display: grid;
        place-items: center;
        background: #2563eb;
        color: #ffffff;
        font-weight: 800;
        letter-spacing: 0.04em;
      }

      .ews-avatar {
        width: 62px;
        height: 62px;
        flex: 0 0 62px;
        border-radius: 18px;
        font-size: 18px;
      }

      .ews-large-avatar {
        width: 86px;
        height: 86px;
        margin: 0 auto 15px;
        border-radius: 24px;
        font-size: 24px;
      }

      .ews-eyebrow {
        margin-bottom: 5px;
        color: #2563eb;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.12em;
      }

      .ews-hero h2 {
        margin: 0;
        font-size: 27px;
        line-height: 1.2;
      }

      .ews-hero p {
        margin: 6px 0 0;
        color: #64748b;
        font-size: 14px;
      }

      .ews-hero-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 11px;
      }

      .ews-hero-tags > span {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 9px;
        border-radius: 999px;
        background: #f8fafc;
        color: #475569;
        font-size: 11px;
        font-weight: 700;
      }

      .ews-hero-side {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 11px;
      }

      .ews-date {
        color: #64748b;
        font-size: 12px;
        font-weight: 600;
      }

      .ews-alert {
        border-radius: 12px;
        padding: 12px 14px;
        font-size: 13px;
        font-weight: 600;
      }

      .ews-alert-error {
        border: 1px solid #fecaca;
        background: #fef2f2;
        color: #b91c1c;
      }

      .ews-alert-success {
        border: 1px solid #bbf7d0;
        background: #f0fdf4;
        color: #15803d;
      }

      .ews-tabs {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;
        width: 100%;
        max-width: 100%;
        overflow-x: visible;
        overflow-y: visible;
        padding: 6px;
        background: #ffffff;
        border: 1px solid #e8edf4;
        border-radius: 15px;
      }

      .ews-tab {
        min-width: max-content;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 7px;
        border: 0;
        padding: 10px 14px;
        border-radius: 10px;
        background: transparent;
        color: #64748b;
        font: inherit;
        font-size: 13px;
        font-weight: 700;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .ews-tab:hover {
        background: #f8fafc;
        color: #172033;
      }

      .ews-tab.active {
        background: #eff6ff;
        color: #2563eb;
      }

      .ews-summary-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 14px;
      }

      .ews-summary-grid-three {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      .ews-summary-card {
        width: 100%;
        position: relative;
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 18px;
        text-align: left;
        border: 1px solid #e8edf4;
        border-radius: 16px;
        background: #ffffff;
        font: inherit;
        cursor: pointer;
        box-shadow: 0 5px 18px rgba(15, 23, 42, 0.03);
        transition: 0.18s ease;
      }

      .ews-summary-card:hover {
        transform: translateY(-2px);
        border-color: #cbdcf8;
        box-shadow: 0 10px 25px rgba(15, 23, 42, 0.06);
      }

      .ews-summary-icon,
      .ews-quick-icon,
      .ews-detail-icon {
        display: grid;
        place-items: center;
        background: #eff6ff;
        color: #2563eb;
        border-radius: 11px;
      }

      .ews-summary-icon {
        width: 42px;
        height: 42px;
        flex: 0 0 42px;
      }

      .ews-summary-copy {
        min-width: 0;
        flex: 1;
      }

      .ews-summary-copy > span,
      .ews-metric-mini > span {
        display: block;
        color: #64748b;
        font-size: 12px;
        margin-bottom: 5px;
      }

      .ews-summary-copy > strong {
        display: block;
        font-size: 21px;
        margin-bottom: 4px;
      }

      .ews-summary-copy > small,
      .ews-metric-mini > small {
        color: #94a3b8;
        font-size: 11px;
      }

      .ews-summary-arrow {
        margin-top: 4px;
        color: #cbd5e1;
      }

      .ews-metric-mini {
        padding: 18px;
        border: 1px solid #e8edf4;
        border-radius: 15px;
        background: #ffffff;
      }

      .ews-metric-mini strong {
        display: block;
        margin: 4px 0;
        font-size: 23px;
      }

      .ews-card {
        padding: 22px;
        background: #ffffff;
        border: 1px solid #e8edf4;
        border-radius: 17px;
        box-shadow: 0 6px 20px rgba(15, 23, 42, 0.035);
      }

      .ews-card-full {
        grid-column: 1 / -1;
      }

      .ews-card-heading {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 16px;
        margin-bottom: 20px;
        color: #64748b;
      }

      .ews-card-heading h3 {
        margin: 0;
        color: #172033;
        font-size: 17px;
      }

      .ews-card-heading p {
        margin: 5px 0 0;
        color: #64748b;
        font-size: 13px;
        line-height: 1.5;
      }

      .ews-dashboard-grid,
      .ews-section-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
      }

      .ews-quick-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
      }

      .ews-quick-action {
        display: flex;
        align-items: center;
        gap: 11px;
        min-height: 74px;
        padding: 13px;
        border: 1px solid #e8edf4;
        border-radius: 13px;
        background: #fafcff;
        text-align: left;
        font: inherit;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .ews-quick-action:hover:not(:disabled) {
        background: #f2f7ff;
        border-color: #cbdcf8;
      }

      .ews-quick-action:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }

      .ews-quick-icon {
        width: 40px;
        height: 40px;
        flex: 0 0 40px;
      }

      .ews-quick-action > div:nth-child(2) {
        flex: 1;
        min-width: 0;
      }

      .ews-quick-action strong {
        display: block;
        color: #172033;
        font-size: 13px;
      }

      .ews-quick-action span {
        display: block;
        margin-top: 3px;
        color: #64748b;
        font-size: 11px;
        line-height: 1.35;
      }

      .ews-attendance-hero {
        display: grid;
        grid-template-columns: minmax(0, 1.7fr) minmax(220px, 0.75fr);
        gap: 18px;
        padding: 24px;
        margin-bottom: 18px;
        border: 1px solid #dbe7f7;
        border-radius: 18px;
        background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 55%, #f7fbff 100%);
        box-shadow: 0 12px 30px rgba(37, 99, 235, 0.07);
      }

      .ews-attendance-hero-copy h3 {
        margin: 6px 0 7px;
        color: #172033;
        font-size: 22px;
      }

      .ews-attendance-hero-copy p {
        margin: 0;
        max-width: 680px;
        color: #64748b;
        font-size: 13px;
        line-height: 1.55;
      }

      .ews-eyebrow {
        color: #2563eb;
        font-size: 10px;
        font-weight: 800;
        letter-spacing: 0.1em;
      }

      .ews-attendance-actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        margin-top: 18px;
      }

      .ews-btn-attendance {
        min-width: 150px;
      }

      .ews-attendance-clock-card {
        position: relative;
        display: flex;
        flex-direction: column;
        justify-content: center;
        min-height: 142px;
        padding: 20px;
        border: 1px solid #d8e4f5;
        border-radius: 15px;
        background: rgba(255, 255, 255, 0.9);
      }

      .ews-attendance-live-dot {
        position: absolute;
        top: 18px;
        right: 18px;
        width: 9px;
        height: 9px;
        border-radius: 50%;
        background: #cbd5e1;
      }

      .ews-attendance-live-dot.is-live {
        background: #22c55e;
        box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.12);
      }

      .ews-attendance-clock-label {
        color: #64748b;
        font-size: 11px;
      }

      .ews-attendance-clock-card strong {
        margin-top: 7px;
        color: #172033;
        font-size: 23px;
      }

      .ews-attendance-clock-card small {
        margin-top: 5px;
        color: #64748b;
        font-size: 11px;
      }

      .ews-attendance-summary-grid {
        margin-bottom: 18px;
      }

      .ews-attendance-today-card {
        margin-bottom: 18px;
      }

      .ews-attendance-timeline {
        display: grid;
        grid-template-columns: minmax(180px, 1fr) 42px minmax(180px, 1fr) minmax(180px, 0.9fr);
        gap: 12px;
        align-items: center;
      }

      .ews-attendance-timeline-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px;
        border: 1px solid #edf1f6;
        border-radius: 13px;
        background: #fafcff;
      }

      .ews-attendance-timeline-item.is-complete {
        border-color: #bbf7d0;
        background: #f7fef9;
      }

      .ews-attendance-timeline-icon {
        display: grid;
        place-items: center;
        width: 38px;
        height: 38px;
        flex: 0 0 38px;
        border-radius: 11px;
        background: #eff6ff;
        color: #2563eb;
      }

      .ews-attendance-timeline-item span,
      .ews-attendance-duration span {
        display: block;
        color: #64748b;
        font-size: 11px;
      }

      .ews-attendance-timeline-item strong,
      .ews-attendance-duration strong {
        display: block;
        margin-top: 3px;
        color: #172033;
        font-size: 16px;
      }

      .ews-attendance-timeline-item small,
      .ews-attendance-duration small {
        display: block;
        margin-top: 3px;
        color: #94a3b8;
        font-size: 10px;
      }

      .ews-attendance-timeline-line {
        height: 2px;
        background: #e2e8f0;
      }

      .ews-attendance-timeline-line.is-active {
        background: #93c5fd;
      }

      .ews-attendance-duration {
        padding: 14px;
        border: 1px solid #dbeafe;
        border-radius: 13px;
        background: #eff6ff;
      }

      .ews-today-status {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
        margin-bottom: 17px;
      }

      .ews-today-status > div {
        padding: 12px;
        border: 1px solid #edf1f6;
        border-radius: 11px;
        background: #fafcff;
      }

      .ews-today-status span {
        display: block;
        margin-bottom: 4px;
        color: #64748b;
        font-size: 11px;
      }

      .ews-today-status strong {
        font-size: 14px;
      }

      .ews-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 7px;
        min-height: 38px;
        padding: 8px 14px;
        border-radius: 10px;
        font: inherit;
        font-size: 13px;
        font-weight: 700;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .ews-btn:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }

      .ews-btn-primary {
        border: 1px solid #2563eb;
        background: #2563eb;
        color: #ffffff;
      }

      .ews-btn-primary:hover:not(:disabled) {
        background: #1d4ed8;
      }

      .ews-btn-secondary {
        border: 1px solid #dbe3ed;
        background: #ffffff;
        color: #172033;
      }

      .ews-btn-secondary:hover:not(:disabled) {
        background: #f8fafc;
      }

      .ews-text-button {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        border: 0;
        background: transparent;
        color: #2563eb;
        font: inherit;
        font-size: 12px;
        font-weight: 700;
        cursor: pointer;
      }

      .ews-completed {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        width: max-content;
        padding: 8px 12px;
        border-radius: 10px;
        background: #ecfdf5;
        color: #047857;
        font-size: 13px;
        font-weight: 700;
      }

      .ews-progress {
        width: 100%;
        height: 7px;
        overflow: hidden;
        margin-top: 9px;
        border-radius: 999px;
        background: #eef2f7;
      }

      .ews-progress.compact {
        height: 5px;
        margin-top: 9px;
      }

      .ews-progress-fill {
        height: 100%;
        border-radius: inherit;
        background: currentColor;
        color: #2563eb;
        transition: width 0.25s ease;
      }

      .ews-balance-list {
        display: flex;
        flex-direction: column;
        gap: 13px;
      }

      .ews-balance-block,
      .ews-leave-card {
        padding: 13px 14px;
        border: 1px solid #edf1f6;
        border-radius: 12px;
        background: #fafcff;
      }

      .ews-balance-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
      }

      .ews-balance-line strong {
        font-size: 12px;
      }

      .ews-balance-line span {
        color: #64748b;
        font-size: 12px;
      }

      .ews-leave-meta {
        display: flex;
        justify-content: space-between;
        margin-top: 7px;
        color: #94a3b8;
        font-size: 11px;
      }

      .ews-activity-list,
      .ews-upcoming-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .ews-activity-item {
        display: flex;
        align-items: flex-start;
        gap: 11px;
      }

      .ews-activity-dot {
        width: 9px;
        height: 9px;
        flex: 0 0 9px;
        margin-top: 5px;
        border: 2px solid #93c5fd;
        border-radius: 50%;
        background: #2563eb;
      }

      .ews-activity-item strong,
      .ews-upcoming-item strong {
        display: block;
        color: #172033;
        font-size: 12px;
      }

      .ews-activity-item span,
      .ews-upcoming-item span {
        display: block;
        margin-top: 3px;
        color: #64748b;
        font-size: 11px;
        line-height: 1.4;
      }

      .ews-upcoming-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px;
        border: 1px solid #edf1f6;
        border-radius: 11px;
      }

      .ews-upcoming-date {
        min-width: 62px;
        padding: 8px;
        border-radius: 9px;
        background: #eff6ff;
        text-align: center;
        color: #2563eb;
      }

      .ews-profile-layout {
        display: grid;
        grid-template-columns: 280px minmax(0, 1fr);
        gap: 16px;
      }

      .ews-profile-summary {
        text-align: center;
      }

      .ews-profile-summary h3 {
        margin: 0;
        font-size: 20px;
      }

      .ews-profile-summary > p {
        margin: 6px 0 13px;
        color: #64748b;
        font-size: 13px;
      }

      .ews-profile-mini {
        margin-top: 20px;
        padding-top: 16px;
        border-top: 1px solid #edf1f6;
      }

      .ews-profile-mini span {
        display: block;
        color: #64748b;
        font-size: 11px;
      }

      .ews-profile-mini strong {
        display: block;
        margin-top: 3px;
        font-size: 14px;
      }

      .ews-detail-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
      }

      .ews-detail-item {
        display: flex;
        align-items: center;
        gap: 11px;
        padding: 13px;
        border: 1px solid #edf1f6;
        border-radius: 12px;
        background: #fafcff;
      }

      .ews-detail-icon {
        width: 38px;
        height: 38px;
        flex: 0 0 38px;
      }

      .ews-detail-item span {
        display: block;
        color: #64748b;
        font-size: 11px;
      }

      .ews-detail-item strong {
        display: block;
        margin-top: 3px;
        font-size: 13px;
        word-break: break-word;
      }

      .ews-form {
        display: flex;
        flex-direction: column;
        gap: 15px;
      }

      .ews-form-row {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
      }

      .ews-form label {
        display: flex;
        flex-direction: column;
        gap: 7px;
        color: #334155;
        font-size: 12px;
        font-weight: 700;
      }

      .ews-form input,
      .ews-form select,
      .ews-form textarea {
        width: 100%;
        border: 1px solid #dbe3ed;
        border-radius: 10px;
        background: #ffffff;
        padding: 10px 11px;
        outline: none;
        font: inherit;
        font-size: 13px;
        color: #172033;
      }

      .ews-form input:focus,
      .ews-form select:focus,
      .ews-form textarea:focus {
        border-color: #2563eb;
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.09);
      }

      .ews-duration-box {
        padding: 10px 12px;
        border-radius: 10px;
        background: #eff6ff;
        color: #475569;
        font-size: 12px;
      }

      .ews-table-wrap {
        width: 100%;
        overflow-x: auto;
      }

      .ews-table {
        width: 100%;
        border-collapse: collapse;
        min-width: 720px;
      }

      .ews-table th {
        text-align: left;
        padding: 11px 12px;
        border-bottom: 1px solid #e8edf4;
        color: #64748b;
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .ews-table td {
        padding: 13px 12px;
        border-bottom: 1px solid #f0f2f6;
        color: #334155;
        font-size: 13px;
        vertical-align: middle;
      }

      .ews-table tbody tr:hover {
        background: #fafcff;
      }

      .ews-status {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 5px 9px;
        border-radius: 999px;
        font-size: 11px;
        font-weight: 800;
        text-transform: uppercase;
      }

      .ews-status-success {
        background: #ecfdf5 !important;
        color: #047857 !important;
      }

      .ews-status-danger {
        background: #fef2f2 !important;
        color: #b91c1c !important;
      }

      .ews-status-warning {
        background: #fffbeb !important;
        color: #b45309 !important;
      }

      .ews-status-neutral {
        background: #f1f5f9 !important;
        color: #475569 !important;
      }

      .ews-link-danger {
        border: 0;
        background: transparent;
        color: #dc2626;
        font: inherit;
        font-size: 12px;
        font-weight: 700;
        cursor: pointer;
        padding: 0;
      }

      .ews-counter {
        padding: 5px 9px;
        border-radius: 999px;
        background: #f1f5f9;
        color: #64748b;
        font-size: 11px;
        font-weight: 700;
      }

      .ews-performance-insights {
        overflow: hidden;
      }

      .ews-performance-insight-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
        margin-top: 4px;
      }

      .ews-performance-insight {
        min-width: 0;
        padding: 16px;
        border: 1px solid #e8edf4;
        border-radius: 14px;
        background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
      }

      .ews-performance-insight span {
        display: block;
        color: #64748b;
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: .04em;
      }

      .ews-performance-insight strong {
        display: block;
        margin-top: 8px;
        color: #0f172a;
        font-size: 24px;
        line-height: 1.15;
      }

      .ews-performance-insight small {
        display: block;
        margin-top: 7px;
        color: #94a3b8;
        font-size: 11px;
        line-height: 1.45;
      }

      .ews-performance-dashboard-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.15fr) minmax(0, .85fr);
        gap: 14px;
        margin-top: 16px;
      }

      .ews-performance-panel {
        min-width: 0;
        padding: 16px;
        border: 1px solid #e8edf4;
        border-radius: 14px;
        background: #fafcff;
      }

      .ews-performance-panel-head {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 14px;
      }

      .ews-performance-panel-head > div > span,
      .ews-latest-review-grid span,
      .ews-manager-note span {
        display: block;
        color: #64748b;
        font-size: 11px;
        font-weight: 700;
      }

      .ews-performance-panel-head > div > strong {
        display: block;
        margin-top: 3px;
        color: #0f172a;
        font-size: 15px;
      }

      .ews-trend-pill {
        display: inline-flex;
        align-items: center;
        padding: 5px 8px;
        border-radius: 999px;
        font-size: 10px;
        font-weight: 800;
        white-space: nowrap;
      }

      .ews-trend-pill.positive {
        background: #ecfdf5;
        color: #047857;
      }

      .ews-trend-pill.negative {
        background: #fef2f2;
        color: #b91c1c;
      }

      .ews-trend-pill.neutral {
        background: #f1f5f9;
        color: #475569;
      }

      .ews-trend-list {
        display: grid;
        gap: 11px;
      }

      .ews-trend-row {
        display: grid;
        gap: 5px;
      }

      .ews-trend-meta {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        color: #64748b;
        font-size: 11px;
      }

      .ews-trend-meta strong {
        color: #0f172a;
        font-size: 11px;
      }

      .ews-trend-track {
        height: 8px;
        overflow: hidden;
        border-radius: 999px;
        background: #e7edf7;
      }

      .ews-trend-fill {
        height: 100%;
        min-width: 2px;
        border-radius: inherit;
        background: linear-gradient(90deg, #2563eb, #7c3aed);
      }

      .ews-latest-review-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
      }

      .ews-latest-review-grid > div {
        min-width: 0;
        padding: 11px;
        border: 1px solid #e8edf4;
        border-radius: 11px;
        background: #ffffff;
      }

      .ews-latest-review-grid strong {
        display: block;
        margin-top: 4px;
        color: #0f172a;
        font-size: 12px;
        line-height: 1.4;
        overflow-wrap: anywhere;
      }

      .ews-manager-note {
        margin-top: 12px;
        padding: 12px;
        border: 1px solid #dbeafe;
        border-radius: 11px;
        background: #eff6ff;
      }

      .ews-manager-note p {
        margin: 5px 0 0;
        color: #334155;
        font-size: 12px;
        line-height: 1.55;
      }

      .ews-performance-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
      }

      .ews-review-card {
        padding: 16px;
        border: 1px solid #e8edf4;
        border-radius: 14px;
        background: #fafcff;
      }

      .ews-review-top {
        display: flex;
        justify-content: space-between;
        gap: 12px;
      }

      .ews-review-top span,
      .ews-review-feedback span {
        display: block;
        color: #64748b;
        font-size: 11px;
      }

      .ews-review-top strong {
        display: block;
        margin-top: 3px;
        font-size: 13px;
      }

      .ews-review-rating {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 15px;
        color: #2563eb;
      }

      .ews-review-rating strong {
        font-size: 24px;
      }

      .ews-review-feedback {
        margin-top: 14px;
      }

      .ews-review-feedback p {
        margin: 5px 0 0;
        color: #475569;
        font-size: 12px;
        line-height: 1.5;
      }

      .ews-info-note {
        display: flex;
        align-items: flex-start;
        gap: 11px;
        padding: 14px;
        border: 1px solid #dbeafe;
        border-radius: 12px;
        background: #f8fbff;
        color: #2563eb;
      }

      .ews-info-note div {
        flex: 1;
      }

      .ews-info-note strong {
        display: block;
        margin-bottom: 3px;
        color: #172033;
        font-size: 12px;
      }

      .ews-info-note span {
        display: block;
        color: #64748b;
        font-size: 11px;
        line-height: 1.5;
      }

      .ews-filter-row {
        display: flex;
        flex-wrap: wrap;
        gap: 7px;
        margin-bottom: 16px;
      }

      .ews-filter {
        border: 1px solid #e2e8f0;
        border-radius: 999px;
        background: #ffffff;
        padding: 7px 11px;
        color: #64748b;
        font: inherit;
        font-size: 11px;
        font-weight: 700;
        cursor: pointer;
      }

      .ews-filter.active {
        border-color: #bfdbfe;
        background: #eff6ff;
        color: #2563eb;
      }

      .ews-notification-list {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }

      .ews-notification-item {
        display: flex;
        gap: 12px;
        padding: 14px;
        border: 1px solid #edf1f6;
        border-radius: 12px;
        background: #ffffff;
      }

      .ews-notification-item.unread {
        background: #f8fbff;
        border-color: #dbeafe;
      }

      .ews-notification-icon {
        width: 36px;
        height: 36px;
        flex: 0 0 36px;
        display: grid;
        place-items: center;
        border-radius: 10px;
        background: #eff6ff;
        color: #2563eb;
      }

      .ews-notification-content {
        flex: 1;
        min-width: 0;
      }

      .ews-notification-top {
        display: flex;
        align-items: center;
        gap: 7px;
        color: #172033;
      }

      .ews-unread-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #2563eb;
      }

      .ews-notification-content p {
        margin: 5px 0;
        color: #64748b;
        line-height: 1.5;
        font-size: 13px;
      }

      .ews-notification-content small {
        color: #94a3b8;
        font-size: 11px;
      }

      .ews-empty {
        padding: 22px 10px;
        text-align: center;
        color: #94a3b8;
        font-size: 13px;
      }

      .ews-loading {
        min-height: 300px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        color: #64748b;
      }

      .ews-spin {
        animation: ewsSpin 0.8s linear infinite;
      }

      @keyframes ewsSpin {
        to {
          transform: rotate(360deg);
        }
      }


      .ews-attendance-filters {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
        gap: 10px;
        align-items: end;
        margin-bottom: 12px;
        padding: 14px;
        border: 1px solid #edf1f6;
        border-radius: 12px;
        background: #fafcff;
      }

      .ews-attendance-filters label {
        display: flex;
        flex-direction: column;
        gap: 6px;
        color: #64748b;
        font-size: 11px;
        font-weight: 700;
      }

      .ews-attendance-filters input,
      .ews-attendance-filters select {
        min-height: 38px;
        padding: 8px 10px;
        border: 1px solid #dbe3ed;
        border-radius: 9px;
        background: #ffffff;
        color: #172033;
        font: inherit;
        font-size: 12px;
      }

      .ews-filter-result {
        margin: 0 0 12px;
        color: #64748b;
        font-size: 12px;
      }

      .ews-btn-small {
        min-height: 32px;
        padding: 6px 9px;
        white-space: nowrap;
        font-size: 11px;
      }

      .ews-modal-backdrop {
        position: fixed;
        inset: 0;
        z-index: 9999;
        display: grid;
        place-items: center;
        padding: 20px;
        background: rgba(15, 23, 42, 0.55);
  backdrop-filter: none !important;
      }

      .ews-payslip-modal {
        width: min(760px, 100%);
        max-height: 90vh;
        overflow-y: auto;
        padding: 24px;
        border-radius: 18px;
        background: #ffffff;
        box-shadow: 0 25px 70px rgba(15, 23, 42, 0.28);
      }

      .ews-payslip-header {
        display: flex;
        justify-content: space-between;
        gap: 18px;
        padding-bottom: 18px;
        border-bottom: 2px solid #2563eb;
      }

      .ews-payslip-header span {
        color: #2563eb;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.12em;
      }

      .ews-payslip-header h3 {
        margin: 4px 0;
        font-size: 22px;
      }

      .ews-payslip-header p {
        margin: 0;
        color: #64748b;
        font-size: 12px;
      }

      .ews-payslip-employee {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
        margin: 18px 0;
      }

      .ews-payslip-lines {
        border: 1px solid #edf1f6;
        border-radius: 12px;
        overflow: hidden;
      }

      .ews-payslip-line {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        padding: 12px 14px;
        border-bottom: 1px solid #edf1f6;
        font-size: 13px;
      }

      .ews-payslip-line:last-child {
        border-bottom: 0;
      }

      .ews-payslip-line.strong {
        background: #ecfdf5;
        color: #047857;
        font-size: 15px;
      }

      .ews-payslip-actions {
        display: flex;
        justify-content: flex-end;
        gap: 9px;
        margin-top: 18px;
      }


      .ews-action-row {
        display: flex;
        flex-wrap: wrap;
        gap: 7px;
      }

      .ews-btn-small {
        min-height: 32px;
        padding: 6px 10px;
        font-size: 11px;
      }

      .ews-btn-danger {
        border: 1px solid #fecaca;
        background: #fff;
        color: #b91c1c;
      }

      .ews-btn-danger:hover:not(:disabled) {
        background: #fef2f2;
      }

      .ews-document-stats {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
      }

      @media (max-width: 1150px) {
        .ews-summary-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .ews-quick-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }


      .ews-tabs > button {
        flex: 0 0 auto;
        min-width: 0;
        white-space: nowrap;
      }

      @media (max-width: 900px) {
        .ews-tabs {
          gap: 5px;
        }

        .ews-tabs > button {
          padding-left: 10px;
          padding-right: 10px;
        }
      }

      @media (max-width: 620px) {
        .ews-tabs {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 7px;
        }

        .ews-tabs > button {
          width: 100%;
          justify-content: center;
        }
      }
      @media (max-width: 850px) {
        .ews-attendance-hero {
          grid-template-columns: 1fr;
        }

        .ews-attendance-timeline {
          grid-template-columns: 1fr;
        }

        .ews-attendance-timeline-line {
          width: 2px;
          height: 20px;
          margin-left: 31px;
        }

        .ews-attendance-filters {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .ews-hero {
          align-items: flex-start;
          flex-direction: column;
        }

        .ews-hero-side {
          align-items: flex-start;
        }

        .ews-dashboard-grid,
        .ews-section-grid,
        .ews-profile-layout {
          grid-template-columns: 1fr;
        }

        .ews-card-full {
          grid-column: auto;
        }

        .ews-performance-insight-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .ews-performance-dashboard-grid {
          grid-template-columns: 1fr;
        }

        .ews-performance-grid {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 620px) {
        .ews-attendance-filters,
        .ews-payslip-employee {
          grid-template-columns: 1fr;
        }

        .ews-payslip-actions {
          flex-direction: column;
        }

        .ews-summary-grid,
        .ews-summary-grid-three,
        .ews-quick-grid,
        .ews-detail-grid,
        .ews-form-row,
        .ews-today-status {
          grid-template-columns: 1fr;
        }

        .ews-hero,
        .ews-card {
          padding: 17px;
        }

        .ews-hero-main {
          align-items: flex-start;
        }

        .ews-hero h2 {
          font-size: 22px;
        }

        .ews-avatar {
          width: 50px;
          height: 50px;
          flex-basis: 50px;
          border-radius: 14px;
        }
      }
    `}</style>
  );
}

