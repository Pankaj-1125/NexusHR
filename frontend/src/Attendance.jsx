import React, { useEffect, useMemo, useState } from "react";
import {
  CalendarDays,
  Clock3,
  LogIn,
  LogOut,
  RefreshCw,
  Users,
  UserCheck,
  UserX,
  AlertTriangle,
  CheckCircle2,
  X,
} from "lucide-react";
import { apiFetch } from "./api/apiClient";

const todayISO = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(
    2,
    "0"
  )}-${String(now.getDate()).padStart(2, "0")}`;
};

const formatTime = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
};

const calculateHours = (checkIn, checkOut) => {
  if (!checkIn || !checkOut) return "—";

  const start = new Date(checkIn);
  const end = new Date(checkOut);

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return "—";
  }

  const difference = end.getTime() - start.getTime();

  if (difference < 0) return "—";

  const totalMinutes = Math.floor(difference / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  return `${hours}h ${String(minutes).padStart(2, "0")}m`;
};

const normalizeEmployee = (employee) => {
  const id =
    employee?.id ??
    employee?.employeeId ??
    employee?.userId ??
    employee?.employeeID;

  const firstName = employee?.firstName ?? "";
  const lastName = employee?.lastName ?? "";

  const fullName =
    employee?.name ||
    employee?.fullName ||
    `${firstName} ${lastName}`.trim() ||
    employee?.employeeName ||
    `Employee ${id ?? ""}`;

  return {
    ...employee,
    id,
    displayName: fullName,
    department:
      employee?.departmentName ||
      employee?.department?.name ||
      employee?.department ||
      "—",
    email: employee?.email || employee?.workEmail || "",
  };
};

const getAttendanceEmployeeId = (record) => {
  return (
    record?.employeeId ??
    record?.employee?.id ??
    record?.employee?.employeeId
  );
};

const getRecordStatus = (record) => {
  if (!record) return "ABSENT";

  const status = String(record.status || "").toUpperCase();

  if (status) return status;

  if (record.checkIn) return "PRESENT";

  return "ABSENT";
};

const statusLabel = (status) => {
  switch (String(status).toUpperCase()) {
    case "PRESENT":
      return "Present";
    case "ABSENT":
      return "Absent";
    case "LATE":
      return "Late";
    case "HALF_DAY":
    case "HALF DAY":
      return "Half Day";
    case "ON_LEAVE":
    case "ON LEAVE":
      return "On Leave";
    default:
      return status || "Absent";
  }
};

const statusClass = (status) => {
  switch (String(status).toUpperCase()) {
    case "PRESENT":
      return "nx-attendance-status-present";

    case "LATE":
    case "HALF_DAY":
    case "HALF DAY":
      return "nx-attendance-status-warning";

    case "ON_LEAVE":
    case "ON LEAVE":
      return "nx-attendance-status-leave";

    case "ABSENT":
    default:
      return "nx-attendance-status-absent";
  }
};

export default function Attendance() {
  const [selectedDate, setSelectedDate] = useState(todayISO());
  const [employees, setEmployees] = useState([]);
  const [records, setRecords] = useState([]);

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState("");
  const [activeFilter, setActiveFilter] = useState("ALL");

  const isToday = selectedDate === todayISO();

  const loadAttendance = async (showRefresh = false) => {
    try {
      setError("");

      if (showRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      const [employeeResponse, attendanceResponse] = await Promise.all([
        apiFetch("/employees"),
        apiFetch(`/attendance/date/${selectedDate}`),
      ]);

      const employeeData = Array.isArray(employeeResponse)
        ? employeeResponse
        : employeeResponse?.content ||
          employeeResponse?.data ||
          employeeResponse?.employees ||
          [];

      const attendanceData = Array.isArray(attendanceResponse)
        ? attendanceResponse
        : attendanceResponse?.content ||
          attendanceResponse?.data ||
          attendanceResponse?.attendance ||
          [];

      setEmployees(employeeData.map(normalizeEmployee));
      setRecords(attendanceData);
    } catch (err) {
      console.error("Attendance load error:", err);

      setError(
        err?.message ||
          "Unable to load attendance data. Please check the backend."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadAttendance(false);
  }, [selectedDate]);

  const employeeRows = useMemo(() => {
    const recordMap = new Map();

    records.forEach((record) => {
      const employeeId = getAttendanceEmployeeId(record);

      if (employeeId !== undefined && employeeId !== null) {
        recordMap.set(String(employeeId), record);
      }
    });

    return employees.map((employee) => {
      const record = recordMap.get(String(employee.id));

      return {
        employee,
        record,
        checkIn: record?.checkIn ?? null,
        checkOut: record?.checkOut ?? null,
        status: getRecordStatus(record),
      };
    });
  }, [employees, records]);

  const filteredRows = useMemo(() => {
    if (activeFilter === "ALL") {
      return employeeRows;
    }

    return employeeRows.filter(
      (row) => row.status.toUpperCase() === activeFilter
    );
  }, [employeeRows, activeFilter]);

  const stats = useMemo(() => {
    const total = employeeRows.length;

    const present = employeeRows.filter(
      (row) => row.status.toUpperCase() === "PRESENT"
    ).length;

    const absent = employeeRows.filter(
      (row) => row.status.toUpperCase() === "ABSENT"
    ).length;

    const late = employeeRows.filter((row) => {
      const status = row.status.toUpperCase();
      return status === "LATE" || status === "HALF_DAY";
    }).length;

    const onLeave = employeeRows.filter((row) => {
      const status = row.status.toUpperCase();
      return status === "ON_LEAVE" || status === "ON LEAVE";
    }).length;

    const presentPercentage =
      total > 0 ? Math.round((present / total) * 100) : 0;

    return {
      total,
      present,
      absent,
      late,
      onLeave,
      presentPercentage,
    };
  }, [employeeRows]);

  const handleCheckIn = async (employeeId) => {
    if (!isToday) return;

    try {
      setActionLoading(`checkin-${employeeId}`);
      setError("");

      await apiFetch(`/attendance/check-in/${employeeId}`, {
        method: "POST",
      });

      await loadAttendance(true);
    } catch (err) {
      console.error("Check-in error:", err);

      setError(
        err?.message ||
          "Unable to check in employee. Please check the backend."
      );
    } finally {
      setActionLoading(null);
    }
  };

  const handleCheckOut = async (employeeId) => {
    if (!isToday) return;

    try {
      setActionLoading(`checkout-${employeeId}`);
      setError("");

      await apiFetch(`/attendance/check-out/${employeeId}`, {
        method: "POST",
      });

      await loadAttendance(true);
    } catch (err) {
      console.error("Check-out error:", err);

      setError(
        err?.message ||
          "Unable to check out employee. Please check the backend."
      );
    } finally {
      setActionLoading(null);
    }
  };

  const handleHalfDay = async (employeeId) => {
    try {
      setActionLoading(`halfday-${employeeId}`);
      setError("");
      await apiFetch(`/attendance/half-day/${employeeId}?date=${selectedDate}`, { method: "POST" });
      await loadAttendance(true);
    } catch (err) {
      setError(err?.message || "Unable to mark half day.");
    } finally {
      setActionLoading(null);
    }
  };

  const exportAttendanceCsv = () => {
    const rows = [["Date","Employee","Email","Department","Check In","Check Out","Hours","Status"], ...filteredRows.map(({employee,checkIn,checkOut,status}) => [selectedDate,employee.displayName,employee.email||"",employee.department||"",formatTime(checkIn),formatTime(checkOut),calculateHours(checkIn,checkOut),status])];
    const csv = rows.map(row => row.map(value => `"${String(value).replaceAll('\"','\"\"')}"`).join(",")).join("\n");
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    link.download = `NexusHR-Attendance-${selectedDate}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const initials = (name) => {
    return String(name)
      .split(" ")
      .filter(Boolean)
      .map((part) => part.charAt(0))
      .join("")
      .slice(0, 2)
      .toUpperCase();
  };

  return (
    <div className="nx-attendance-page">
      <style>{`
        .nx-attendance-page {
          padding: 24px;
          width: 100%;
          box-sizing: border-box;
        }

        .nx-attendance-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: 20px;
          margin-bottom: 24px;
        }

        .nx-attendance-title-wrap {
          display: flex;
          align-items: flex-start;
          gap: 14px;
        }

        .nx-attendance-title-icon {
          width: 46px;
          height: 46px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(59, 130, 246, 0.12);
          color: #2563eb;
          flex-shrink: 0;
        }

        .nx-attendance-title {
          margin: 0;
          font-size: 28px;
          font-weight: 700;
        }

        .nx-attendance-subtitle {
          margin: 6px 0 0;
          color: #64748b;
          font-size: 14px;
        }

        .nx-attendance-actions {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .nx-attendance-date {
          display: flex;
          align-items: center;
          gap: 8px;
          border: 1px solid #dbe2ea;
          border-radius: 10px;
          padding: 10px 12px;
          background: #fff;
        }

        .nx-attendance-date input {
          border: 0;
          outline: 0;
          background: transparent;
          font-size: 14px;
          color: #334155;
        }

        .nx-attendance-refresh {
          border: 1px solid #dbe2ea;
          background: #fff;
          border-radius: 10px;
          height: 42px;
          width: 42px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
        }

        .nx-attendance-refresh:hover {
          background: #f8fafc;
        }

        .nx-attendance-refresh:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .nx-attendance-error {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
          background: #fef2f2;
          color: #b91c1c;
          border: 1px solid #fecaca;
          padding: 12px 14px;
          border-radius: 10px;
          margin-bottom: 20px;
        }

        .nx-attendance-error-content {
          display: flex;
          align-items: center;
          gap: 9px;
          font-size: 14px;
        }

        .nx-attendance-error-close {
          border: 0;
          background: transparent;
          cursor: pointer;
          color: inherit;
        }

        .nx-attendance-stats {
          display: grid;
          grid-template-columns: repeat(4, minmax(0, 1fr));
          gap: 16px;
          margin-bottom: 22px;
        }

        .nx-attendance-stat {
          background: #fff;
          border: 1px solid #e2e8f0;
          border-radius: 14px;
          padding: 18px;
        }

        .nx-attendance-stat-top {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 10px;
        }

        .nx-attendance-stat-label {
          font-size: 13px;
          color: #64748b;
        }

        .nx-attendance-stat-value {
          margin-top: 8px;
          font-size: 28px;
          font-weight: 700;
          color: #0f172a;
        }

        .nx-attendance-stat-note {
          margin-top: 5px;
          font-size: 12px;
          color: #94a3b8;
        }

        .nx-attendance-stat-icon {
          width: 38px;
          height: 38px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f8fafc;
        }

        .nx-attendance-filters {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
          margin-bottom: 18px;
        }

        .nx-attendance-filter {
          border: 1px solid #dbe2ea;
          background: #fff;
          color: #475569;
          border-radius: 999px;
          padding: 8px 14px;
          cursor: pointer;
          font-size: 13px;
        }

        .nx-attendance-filter.active {
          background: #0f172a;
          color: #fff;
          border-color: #0f172a;
        }

        .nx-attendance-card {
          background: #fff;
          border: 1px solid #e2e8f0;
          border-radius: 14px;
          overflow: hidden;
        }

        .nx-attendance-card-header {
          padding: 18px 20px;
          border-bottom: 1px solid #e2e8f0;
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 16px;
        }

        .nx-attendance-card-title {
          margin: 0;
          font-size: 17px;
          font-weight: 700;
        }

        .nx-attendance-card-subtitle {
          margin: 5px 0 0;
          color: #64748b;
          font-size: 13px;
        }

        .nx-attendance-updated {
          color: #94a3b8;
          font-size: 12px;
        }

        .nx-attendance-table-wrap {
          overflow-x: auto;
        }

        .nx-attendance-table {
          width: 100%;
          border-collapse: collapse;
          min-width: 920px;
        }

        .nx-attendance-table th {
          text-align: left;
          padding: 13px 18px;
          background: #f8fafc;
          color: #64748b;
          font-size: 12px;
          font-weight: 600;
          white-space: nowrap;
        }

        .nx-attendance-table td {
          padding: 15px 18px;
          border-top: 1px solid #eef2f7;
          color: #334155;
          font-size: 13px;
          vertical-align: middle;
        }

        .nx-attendance-employee {
          display: flex;
          align-items: center;
          gap: 11px;
        }

        .nx-attendance-avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #e2e8f0;
          color: #334155;
          font-weight: 700;
          font-size: 12px;
          flex-shrink: 0;
        }

        .nx-attendance-employee-name {
          font-weight: 600;
          color: #0f172a;
        }

        .nx-attendance-employee-email {
          margin-top: 3px;
          color: #94a3b8;
          font-size: 11px;
        }

        .nx-attendance-status {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          border-radius: 999px;
          padding: 5px 9px;
          font-size: 12px;
          font-weight: 600;
        }

        .nx-attendance-status-present {
          background: #ecfdf5;
          color: #047857;
        }

        .nx-attendance-status-absent {
          background: #fef2f2;
          color: #b91c1c;
        }

        .nx-attendance-status-warning {
          background: #fffbeb;
          color: #b45309;
        }

        .nx-attendance-status-leave {
          background: #eff6ff;
          color: #1d4ed8;
        }

        .nx-attendance-action {
          border: 0;
          border-radius: 8px;
          padding: 8px 11px;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          cursor: pointer;
          font-size: 12px;
          font-weight: 600;
        }

        .nx-attendance-action.checkin {
          background: #eff6ff;
          color: #1d4ed8;
        }

        .nx-attendance-action.checkout {
          background: #fff7ed;
          color: #c2410c;
        }

        .nx-attendance-action:disabled {
          opacity: 0.55;
          cursor: not-allowed;
        }

        .nx-attendance-completed {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          color: #047857;
          font-size: 12px;
          font-weight: 600;
        }

        .nx-attendance-readonly {
          color: #94a3b8;
          font-size: 12px;
        }

        .nx-attendance-empty,
        .nx-attendance-loading {
          padding: 45px 20px;
          text-align: center;
          color: #94a3b8;
        }

        .nx-attendance-empty-icon {
          margin: 0 auto 10px;
          width: 44px;
          height: 44px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 50%;
          background: #f8fafc;
        }

        @media (max-width: 900px) {
          .nx-attendance-stats {
            grid-template-columns: repeat(2, minmax(0, 1fr));
          }

          .nx-attendance-header {
            flex-direction: column;
          }
        }

        @media (max-width: 600px) {
          .nx-attendance-page {
            padding: 16px;
          }

          .nx-attendance-stats {
            grid-template-columns: 1fr;
          }

          .nx-attendance-actions {
            width: 100%;
            flex-wrap: wrap;
          }
        }
      `}</style>

      <div className="nx-attendance-header">
        <div className="nx-attendance-title-wrap">
          <div className="nx-attendance-title-icon">
            <Clock3 size={24} />
          </div>

          <div>
            <h1 className="nx-attendance-title">Attendance</h1>
            <p className="nx-attendance-subtitle">
              Track employee attendance and daily workforce presence.
            </p>
          </div>
        </div>

        <div className="nx-attendance-actions">
          <div className="nx-attendance-date">
            <CalendarDays size={17} />

            <input
              type="date"
              value={selectedDate}
              onChange={(event) => {
                setSelectedDate(event.target.value);
                setActiveFilter("ALL");
              }}
            />
          </div>

          <button
            type="button"
            className="nx-attendance-refresh"
            onClick={() => loadAttendance(true)}
            disabled={refreshing}
            title="Refresh attendance"
          >
            <RefreshCw size={18} />
          </button>
        </div>
      </div>

      {error && (
        <div className="nx-attendance-error">
          <div className="nx-attendance-error-content">
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>

          <button
            type="button"
            className="nx-attendance-error-close"
            onClick={() => setError("")}
          >
            <X size={18} />
          </button>
        </div>
      )}

      <div className="nx-attendance-stats">
        <div className="nx-attendance-stat">
          <div className="nx-attendance-stat-top">
            <div>
              <div className="nx-attendance-stat-label">
                Total employees
              </div>

              <div className="nx-attendance-stat-value">
                {stats.total}
              </div>
            </div>

            <div className="nx-attendance-stat-icon">
              <Users size={19} />
            </div>
          </div>

          <div className="nx-attendance-stat-note">
            For selected date
          </div>
        </div>

        <div className="nx-attendance-stat">
          <div className="nx-attendance-stat-top">
            <div>
              <div className="nx-attendance-stat-label">Present</div>

              <div className="nx-attendance-stat-value">
                {stats.present}
              </div>
            </div>

            <div className="nx-attendance-stat-icon">
              <UserCheck size={19} />
            </div>
          </div>

          <div className="nx-attendance-stat-note">
            {stats.presentPercentage}% of employees
          </div>
        </div>

        <div className="nx-attendance-stat">
          <div className="nx-attendance-stat-top">
            <div>
              <div className="nx-attendance-stat-label">
                Late / half day
              </div>

              <div className="nx-attendance-stat-value">
                {stats.late}
              </div>
            </div>

            <div className="nx-attendance-stat-icon">
              <AlertTriangle size={19} />
            </div>
          </div>

          <div className="nx-attendance-stat-note">
            Needs attention
          </div>
        </div>

        <div className="nx-attendance-stat">
          <div className="nx-attendance-stat-top">
            <div>
              <div className="nx-attendance-stat-label">Absent</div>

              <div className="nx-attendance-stat-value">
                {stats.absent}
              </div>
            </div>

            <div className="nx-attendance-stat-icon">
              <UserX size={19} />
            </div>
          </div>

          <div className="nx-attendance-stat-note">
            For selected date
          </div>
        </div>
      </div>

      <div className="nx-attendance-filters">
        {[
          ["ALL", "All"],
          ["PRESENT", "Present"],
          ["ABSENT", "Absent"],
          ["LATE", "Late"],
          ["HALF_DAY", "Half Day"],
          ["ON_LEAVE", "On Leave"],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            className={`nx-attendance-filter ${
              activeFilter === value ? "active" : ""
            }`}
            onClick={() => setActiveFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="nx-attendance-card">
        <div className="nx-attendance-card-header">
          <div>
            <h2 className="nx-attendance-card-title">
              Daily attendance
            </h2>

            <p className="nx-attendance-card-subtitle">
              {new Date(
                `${selectedDate}T00:00:00`
              ).toLocaleDateString(undefined, {
                day: "2-digit",
                month: "short",
                year: "numeric",
              })}{" "}
              · {filteredRows.length} employees shown
            </p>
          </div>

          <div style={{display:"flex",alignItems:"center",gap:10,flexWrap:"wrap"}}>
            <button type="button" className="nx-attendance-action checkin" onClick={exportAttendanceCsv}>Download CSV</button>
            <div className="nx-attendance-updated">
              Updated {new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
            </div>
          </div>
        </div>

        {loading ? (
          <div className="nx-attendance-loading">
            Loading attendance...
          </div>
        ) : filteredRows.length === 0 ? (
          <div className="nx-attendance-empty">
            <div className="nx-attendance-empty-icon">
              <Users size={20} />
            </div>

            No employees found for this filter.
          </div>
        ) : (
          <div className="nx-attendance-table-wrap">
            <table className="nx-attendance-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Department</th>
                  <th>Check in</th>
                  <th>Check out</th>
                  <th>Hours</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {filteredRows.map(
                  ({
                    employee,
                    checkIn,
                    checkOut,
                    status,
                  }) => {
                    const employeeId = employee.id;

                    const hasCheckIn = Boolean(checkIn);
                    const hasCheckOut = Boolean(checkOut);

                    const checkInLoading =
                      actionLoading === `checkin-${employeeId}`;

                    const checkOutLoading =
                      actionLoading === `checkout-${employeeId}`;

                    return (
                      <tr key={employeeId}>
                        <td>
                          <div className="nx-attendance-employee">
                            <div className="nx-attendance-avatar">
                              {initials(employee.displayName)}
                            </div>

                            <div>
                              <div className="nx-attendance-employee-name">
                                {employee.displayName}
                              </div>

                              {employee.email && (
                                <div className="nx-attendance-employee-email">
                                  {employee.email}
                                </div>
                              )}
                            </div>
                          </div>
                        </td>

                        <td>{employee.department}</td>

                        <td>{formatTime(checkIn)}</td>

                        <td>{formatTime(checkOut)}</td>

                        <td>
                          {calculateHours(checkIn, checkOut)}
                        </td>

                        <td>
                          <span
                            className={`nx-attendance-status ${statusClass(
                              status
                            )}`}
                          >
                            {String(status).toUpperCase() ===
                              "PRESENT" && (
                              <CheckCircle2 size={13} />
                            )}

                            {statusLabel(status)}
                          </span>
                        </td>

                        <td>
                          <div style={{ display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center" }}>
                            {!isToday ? (
                              <span className="nx-attendance-readonly">Read only</span>
                            ) : hasCheckIn && hasCheckOut ? (
                              <span className="nx-attendance-completed"><CheckCircle2 size={14} /> Completed</span>
                            ) : !hasCheckIn ? (
                              <button type="button" className="nx-attendance-action checkin" onClick={() => handleCheckIn(employeeId)} disabled={checkInLoading || checkOutLoading || employeeId == null}>
                                <LogIn size={14} /> {checkInLoading ? "Checking in..." : "Check in"}
                              </button>
                            ) : (
                              <button type="button" className="nx-attendance-action checkout" onClick={() => handleCheckOut(employeeId)} disabled={checkOutLoading || checkInLoading || employeeId == null}>
                                <LogOut size={14} /> {checkOutLoading ? "Checking out..." : "Check out"}
                              </button>
                            )}

                            {isToday && String(status).toUpperCase() !== "HALF_DAY" && (
                              <button type="button" className="nx-attendance-action" style={{ background: "#fff7ed", color: "#c2410c", border: "1px solid #fdba74" }} onClick={() => handleHalfDay(employeeId)} disabled={actionLoading === `halfday-${employeeId}` || employeeId == null}>
                                {actionLoading === `halfday-${employeeId}` ? "Marking..." : "Half day"}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  }
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}