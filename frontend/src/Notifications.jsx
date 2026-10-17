import React, {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  Bell,
  CheckCheck,
  Mail,
  MessageSquareText,
  Radio,
  RefreshCw,
  Send,
  Trash2,
  Users,
} from "lucide-react";

import {
  apiFetch,
} from "./api/apiClient";

import {
  connectRealtimeNotifications,
} from "./api/notificationSocket";

const TYPES = [
  "GENERAL",
  "ANNOUNCEMENT",
  "REMINDER",
  "APPROVAL",
  "ALERT",
  "PAYROLL",
  "LEAVE",
  "JOB_ALERT",
];

const COMMUNICATION_TEMPLATES = {
  ANNOUNCEMENT: {
    type: "ANNOUNCEMENT",
    title: "Company Announcement",
    message: "Please review this important NexusHR announcement.",
  },
  REMINDER: {
    type: "REMINDER",
    title: "Action Reminder",
    message: "This is a reminder that an action is pending. Please review NexusHR for the complete details.",
  },
  APPROVAL: {
    type: "APPROVAL",
    title: "Approval Update",
    message: "Your request has an approval update. Please review NexusHR for the current status and details.",
  },
  PAYROLL: {
    type: "PAYROLL",
    title: "Payroll Update",
    message: "A payroll update is available. Please open NexusHR Payroll for the complete information.",
  },
};

const styles = {
  page: {
    display: "grid",
    gap: 20,
  },

  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: 16,
    flexWrap: "wrap",
  },

  title: {
    margin: 0,
    fontSize: 26,
    color: "#0f172a",
  },

  muted: {
    color: "#64748b",
    fontSize: 13,
    lineHeight: 1.6,
  },

  card: {
    background: "#fff",
    border: "1px solid #e2e8f0",
    borderRadius: 16,
    boxShadow: "0 8px 24px rgba(15,23,42,.05)",
  },

  input: {
    width: "100%",
    boxSizing: "border-box",
    padding: "10px 12px",
    border: "1px solid #dbe3ef",
    borderRadius: 10,
    fontSize: 14,
    background: "#fff",
    color: "#0f172a",
  },

  label: {
    display: "grid",
    gap: 7,
    fontSize: 12,
    fontWeight: 700,
    color: "#475569",
  },

  button: {
    display: "inline-flex",
    alignItems: "center",
    gap: 8,
    border: 0,
    borderRadius: 10,
    padding: "10px 14px",
    fontWeight: 700,
    cursor: "pointer",
  },
};

function formatDate(value) {

  if (!value) {
    return "-";
  }

  const date =
    new Date(value);

  return Number.isNaN(
    date.getTime()
  )
    ? value
    : date.toLocaleString();
}

function typeStyle(type) {

  const value =
    String(
      type || "GENERAL"
    ).toUpperCase();

  if (value === "ALERT") {

    return {
      background: "#fee2e2",
      color: "#991b1b",
    };
  }

  if (value === "APPROVAL") {

    return {
      background: "#ede9fe",
      color: "#5b21b6",
    };
  }

  if (value === "REMINDER") {

    return {
      background: "#fef3c7",
      color: "#92400e",
    };
  }

  if (value === "ANNOUNCEMENT") {

    return {
      background: "#dbeafe",
      color: "#1d4ed8",
    };
  }

  if (value === "PAYROLL") {

    return {
      background: "#dcfce7",
      color: "#166534",
    };
  }

  if (value === "LEAVE") {

    return {
      background: "#f3e8ff",
      color: "#7e22ce",
    };
  }

  if (value === "JOB_ALERT") {
    return {
      background: "#ecfeff",
      color: "#0e7490",
    };
  }

  return {
    background: "#f1f5f9",
    color: "#475569",
  };
}

export default function Notifications() {

  const [
    employees,
    setEmployees,
  ] = useState([]);

  const [
    selectedEmployeeId,
    setSelectedEmployeeId,
  ] = useState("");

  const [
    notifications,
    setNotifications,
  ] = useState([]);

  const [
    summary,
    setSummary,
  ] = useState({
    total: 0,
    unread: 0,
    read: 0,
    byType: {},
  });

  const [
    loading,
    setLoading,
  ] = useState(true);

  const [
    saving,
    setSaving,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState("");

  const [
    success,
    setSuccess,
  ] = useState("");

  const [
    unreadOnly,
    setUnreadOnly,
  ] = useState(false);

  const [
    realtimeConnected,
    setRealtimeConnected,
  ] = useState(false);

  const [
    form,
    setForm,
  ] = useState({
    targetMode: "EMPLOYEE",
    employeeId: "",
    department: "ALL",
    title: "",
    message: "",
    type: "ANNOUNCEMENT",
    sendEmail: true,
    sendSms: false,
  });

  const departments =
    useMemo(
      () =>
        [
          ...new Set(
            employees
              .map(
                (employee) =>
                  employee.department
              )
              .filter(Boolean)
          ),
        ].sort(),
      [
        employees,
      ]
    );

  const selectedEmployee =
    useMemo(
      () =>
        employees.find(
          (employee) =>
            String(
              employee.id
            ) ===
            String(
              selectedEmployeeId
            )
        ) || null,
      [
        employees,
        selectedEmployeeId,
      ]
    );

  async function loadEmployees() {

    const rows =
      await apiFetch(
        "/employees"
      );

    const list =
      Array.isArray(rows)
        ? rows
        : [];

    setEmployees(
      list
    );

    if (
      !selectedEmployeeId &&
      list.length
    ) {

      const id =
        String(
          list[0].id
        );

      setSelectedEmployeeId(
        id
      );

      setForm(
        (current) => ({
          ...current,
          employeeId: id,
        })
      );
    }

    return list;
  }

  async function loadNotifications(
    employeeId =
      selectedEmployeeId,
    onlyUnread =
      unreadOnly
  ) {

    if (!employeeId) {

      setNotifications([]);

      setSummary({
        total: 0,
        unread: 0,
        read: 0,
        byType: {},
      });

      return;
    }

    const [
      rows,
      stats,
    ] =
      await Promise.all([
        apiFetch(
          onlyUnread
            ? `/notifications/employee/${employeeId}/unread`
            : `/notifications/employee/${employeeId}`
        ),

        apiFetch(
          `/notifications/employee/${employeeId}/summary`
        ),
      ]);

    setNotifications(
      Array.isArray(rows)
        ? rows
        : []
    );

    setSummary(
      stats || {
        total: 0,
        unread: 0,
        read: 0,
        byType: {},
      }
    );
  }

  async function refreshAll() {

    try {

      setLoading(true);

      setError("");

      setSuccess("");

      const list =
        await loadEmployees();

      const employeeId =
        selectedEmployeeId ||
        (
          list.length
            ? String(
                list[0].id
              )
            : ""
        );

      if (employeeId) {

        await loadNotifications(
          employeeId,
          unreadOnly
        );
      }

    } catch (err) {

      setError(
        err?.message ||
          "Unable to load notification data."
      );

    } finally {

      setLoading(false);
    }
  }

  useEffect(
    () => {

      let active =
        true;

      (
        async () => {

          try {

            setLoading(true);

            const rows =
              await apiFetch(
                "/employees"
              );

            if (!active) {
              return;
            }

            const list =
              Array.isArray(rows)
                ? rows
                : [];

            setEmployees(
              list
            );

            if (
              list.length
            ) {

              const id =
                String(
                  list[0].id
                );

              setSelectedEmployeeId(
                (current) =>
                  current || id
              );

              setForm(
                (current) => ({
                  ...current,
                  employeeId:
                    current.employeeId ||
                    id,
                })
              );
            }

          } catch (err) {

            if (active) {

              setError(
                err?.message ||
                  "Unable to load employees."
              );
            }

          } finally {

            if (active) {

              setLoading(
                false
              );
            }
          }
        }
      )();

      return () => {

        active =
          false;
      };
    },
    []
  );

  useEffect(
    () => {

      if (
        !selectedEmployeeId
      ) {
        return;
      }

      loadNotifications(
        selectedEmployeeId,
        unreadOnly
      ).catch(
        (err) => {

          setError(
            err?.message ||
              "Unable to load notifications."
          );
        }
      );
    },
    [
      selectedEmployeeId,
      unreadOnly,
    ]
  );

  useEffect(
    () => {

      const disconnect =
        connectRealtimeNotifications(
          (notification) => {

            if (
              String(
                notification?.employeeId
              ) !==
              String(
                selectedEmployeeId
              )
            ) {
              return;
            }

            setNotifications(
              (current) => {

                const filtered =
                  current.filter(
                    (item) =>
                      item.id !==
                      notification.id
                  );

                return [
                  notification,
                  ...filtered,
                ];
              }
            );

            setSummary(
              (current) => ({
                ...current,

                total:
                  Number(
                    current.total ||
                      0
                  ) + 1,

                unread:
                  Number(
                    current.unread ||
                      0
                  ) +
                  (
                    notification.read
                      ? 0
                      : 1
                  ),
              })
            );
          },

          setRealtimeConnected
        );

      return () => {

        if (
          typeof disconnect ===
          "function"
        ) {

          disconnect();
        }
      };
    },
    [
      selectedEmployeeId,
    ]
  );

  function updateForm(
    field,
    value
  ) {

    setForm(
      (current) => ({
        ...current,
        [field]: value,
      })
    );
  }

  async function handleSubmit(
    event
  ) {

    event.preventDefault();

    setError("");

    setSuccess("");

    const title =
      form.title.trim();

    const message =
      form.message.trim();

    if (
      !title ||
      !message
    ) {

      setError(
        "Subject and message are required."
      );

      return;
    }

    if (
      form.targetMode ===
        "EMPLOYEE" &&
      !form.employeeId
    ) {

      setError(
        "Select an employee."
      );

      return;
    }

    if (
      form.targetMode ===
        "DEPARTMENT" &&
      !form.department
    ) {

      setError(
        "Select a department."
      );

      return;
    }

    try {

      setSaving(true);

      let response;

      if (
        form.targetMode ===
        "EMPLOYEE"
      ) {

        response =
          await apiFetch(
            "/notifications",
            {
              method:
                "POST",

              body:
                JSON.stringify({
                  employeeId:
                    Number(
                      form.employeeId
                    ),

                  title,

                  message,

                  type:
                    form.type,

                  sendEmail:
                    form.sendEmail,

                  sendSms:
                    form.sendSms,
                }),
            }
          );

      } else {

        response =
          await apiFetch(
            "/notifications/broadcast",
            {
              method:
                "POST",

              body:
                JSON.stringify({
                  title,

                  message,

                  type:
                    form.type,

                  department:
                    form.targetMode ===
                    "DEPARTMENT"
                      ? form.department
                      : "ALL",

                  sendEmail:
                    form.sendEmail,

                  sendSms:
                    form.sendSms,
                }),
            }
          );
      }

      let resultMessage =
        response?.message ||
        "Notification sent successfully.";

      if (
        response?.email
      ) {

        const emailSent =
          response.email.sent ??
          response.email.status ??
          0;

        const emailFailed =
          response.email.failed ??
          0;

        resultMessage +=
          ` Email: ${emailSent} sent/status, ${emailFailed} failed.`;
      }

      if (
        response?.sms
      ) {

        const smsSent =
          response.sms.sent ??
          response.sms.status ??
          0;

        resultMessage +=
          ` SMS: ${smsSent} sent/status.`;
      }

      setSuccess(
        resultMessage
      );

      setForm(
        (current) => ({
          ...current,
          title: "",
          message: "",
        })
      );

      if (
        selectedEmployeeId
      ) {

        await loadNotifications(
          selectedEmployeeId,
          unreadOnly
        );
      }

    } catch (err) {

      setError(
        err?.message ||
          "Unable to send notification."
      );

    } finally {

      setSaving(false);
    }
  }

  async function markRead(
    id
  ) {

    try {

      setError("");

      await apiFetch(
        `/notifications/${id}/read`,
        {
          method:
            "PUT",
        }
      );

      await loadNotifications();

    } catch (err) {

      setError(
        err?.message ||
          "Unable to update notification."
      );
    }
  }

  async function markAllRead() {

    if (
      !selectedEmployeeId
    ) {
      return;
    }

    try {

      setError("");

      await apiFetch(
        `/notifications/employee/${selectedEmployeeId}/read-all`,
        {
          method:
            "PUT",
        }
      );

      await loadNotifications();

    } catch (err) {

      setError(
        err?.message ||
          "Unable to mark notifications as read."
      );
    }
  }

  async function deleteNotification(
    id
  ) {

    try {

      setError("");

      await apiFetch(
        `/notifications/${id}`,
        {
          method:
            "DELETE",
        }
      );

      await loadNotifications();

    } catch (err) {

      setError(
        err?.message ||
          "Unable to delete notification."
      );
    }
  }

  const stats = [
    {
      label:
        "Total messages",
      value:
        summary.total ||
        0,
      icon:
        Bell,
    },

    {
      label:
        "Unread",
      value:
        summary.unread ||
        0,
      icon:
        MessageSquareText,
    },

    {
      label:
        "Read",
      value:
        summary.read ||
        0,
      icon:
        CheckCheck,
    },

    {
      label:
        "Live channel",
      value:
        realtimeConnected
          ? "Connected"
          : "Offline",
      icon:
        Radio,
    },
  ];

  return (
    <div
      style={
        styles.page
      }
    >

      <div
        style={
          styles.header
        }
      >

        <div>

          <h2
            style={
              styles.title
            }
          >
            Notifications & Communications
          </h2>

          <div
            style={
              styles.muted
            }
          >
            Create announcements, direct messages and department updates from one organized workspace.
          </div>

        </div>

        <button
          type="button"
          onClick={
            refreshAll
          }
          style={{
            ...styles.button,
            background:
              "#eff6ff",
            color:
              "#1d4ed8",
          }}
        >
          <RefreshCw
            size={16}
          />

          Refresh
        </button>

      </div>

      {
        (
          error ||
          success
        ) && (

          <div
            style={{
              ...styles.card,

              padding:
                14,

              borderColor:
                error
                  ? "#fecaca"
                  : "#bbf7d0",

              background:
                error
                  ? "#fef2f2"
                  : "#f0fdf4",

              color:
                error
                  ? "#991b1b"
                  : "#166534",
            }}
          >
            {
              error ||
              success
            }
          </div>
        )
      }

      <div
        style={{
          display:
            "grid",

          gridTemplateColumns:
            "repeat(auto-fit,minmax(180px,1fr))",

          gap:
            14,
        }}
      >

        {
          stats.map(
            ({
              label,
              value,
              icon:
                Icon,
            }) => (

              <div
                key={
                  label
                }
                style={{
                  ...styles.card,
                  padding:
                    16,
                }}
              >

                <div
                  style={{
                    display:
                      "flex",

                    justifyContent:
                      "space-between",

                    color:
                      "#64748b",

                    fontSize:
                      12,
                  }}
                >

                  <span>
                    {
                      label
                    }
                  </span>

                  <Icon
                    size={18}
                  />

                </div>

                <div
                  style={{
                    marginTop:
                      10,

                    fontSize:
                      24,

                    fontWeight:
                      800,

                    color:
                      "#0f172a",
                  }}
                >
                  {
                    value
                  }
                </div>

              </div>
            )
          )
        }

      </div>

      <div
        style={{
          display:
            "grid",

          gridTemplateColumns:
            "repeat(auto-fit,minmax(380px,1fr))",

          gap:
            18,

          alignItems:
            "start",
        }}
      >

        <form
          onSubmit={
            handleSubmit
          }
          style={{
            ...styles.card,

            padding:
              18,

            display:
              "grid",

            gap:
              14,
          }}
        >

          <div
            style={{
              fontWeight:
                800,

              color:
                "#0f172a",

              display:
                "flex",

              alignItems:
                "center",

              gap:
                8,
            }}
          >

            <Send
              size={18}
            />

            Compose message

          </div>

          <label
            style={
              styles.label
            }
          >

            Audience

            <select
              style={
                styles.input
              }
              value={
                form.targetMode
              }
              onChange={
                (
                  event
                ) =>
                  updateForm(
                    "targetMode",
                    event
                      .target
                      .value
                  )
              }
            >

              <option
                value="EMPLOYEE"
              >
                Single employee
              </option>

              <option
                value="DEPARTMENT"
              >
                Department
                broadcast
              </option>

              <option
                value="ALL"
              >
                All active
                employees
              </option>

            </select>

          </label>

          {
            form.targetMode ===
              "EMPLOYEE" && (

              <label
                style={
                  styles.label
                }
              >

                Employee

                <select
                  style={
                    styles.input
                  }
                  value={
                    form.employeeId
                  }
                  onChange={
                    (
                      event
                    ) =>
                      updateForm(
                        "employeeId",
                        event
                          .target
                          .value
                      )
                  }
                >

                  <option
                    value=""
                  >
                    Select employee
                  </option>

                  {
                    employees.map(
                      (
                        employee
                      ) => (

                        <option
                          key={
                            employee.id
                          }
                          value={
                            employee.id
                          }
                        >
                          {
                            employee.firstName
                          }{" "}
                          {
                            employee.lastName
                          }
                          {" — "}
                          {
                            employee.department ||
                            "No department"
                          }
                        </option>
                      )
                    )
                  }

                </select>

              </label>
            )
          }

          {
            form.targetMode ===
              "DEPARTMENT" && (

              <label
                style={
                  styles.label
                }
              >

                Department

                <select
                  style={
                    styles.input
                  }
                  value={
                    form.department
                  }
                  onChange={
                    (
                      event
                    ) =>
                      updateForm(
                        "department",
                        event
                          .target
                          .value
                      )
                  }
                >

                  <option
                    value="ALL"
                  >
                    All departments
                  </option>

                  {
                    departments.map(
                      (
                        department
                      ) => (

                        <option
                          key={
                            department
                          }
                          value={
                            department
                          }
                        >
                          {
                            department
                          }
                        </option>
                      )
                    )
                  }

                </select>

              </label>
            )
          }

          <label
            style={styles.label}
          >
            Quick template

            <select
              style={styles.input}
              defaultValue=""
              onChange={(event) => {
                const template = COMMUNICATION_TEMPLATES[event.target.value];
                if (!template) return;
                setForm((current) => ({
                  ...current,
                  type: template.type,
                  title: template.title,
                  message: template.message,
                }));
                event.target.value = "";
              }}
            >
              <option value="">Choose a template...</option>
              <option value="ANNOUNCEMENT">Company announcement</option>
              <option value="REMINDER">Action reminder</option>
              <option value="APPROVAL">Approval update</option>
              <option value="PAYROLL">Payroll update</option>
            </select>
          </label>

          <label
            style={
              styles.label
            }
          >

            Type

            <select
              style={
                styles.input
              }
              value={
                form.type
              }
              onChange={
                (
                  event
                ) =>
                  updateForm(
                    "type",
                    event
                      .target
                      .value
                  )
              }
            >

              {
                TYPES.map(
                  (
                    type
                  ) => (

                    <option
                      key={
                        type
                      }
                      value={
                        type
                      }
                    >
                      {
                        type
                      }
                    </option>
                  )
                )
              }

            </select>

          </label>

          <label
            style={
              styles.label
            }
          >

            Subject

            <input
              style={
                styles.input
              }
              maxLength={
                150
              }
              value={
                form.title
              }
              onChange={
                (
                  event
                ) =>
                  updateForm(
                    "title",
                    event
                      .target
                      .value
                  )
              }
              placeholder="e.g. Leave request approved"
            />

          </label>

          <label
            style={
              styles.label
            }
          >

            Message

            <textarea
              style={{
                ...styles.input,

                minHeight:
                  120,

                resize:
                  "vertical",
              }}
              maxLength={
                2000
              }
              value={
                form.message
              }
              onChange={
                (
                  event
                ) =>
                  updateForm(
                    "message",
                    event
                      .target
                      .value
                  )
              }
              placeholder="Write the employee communication..."
            />

          </label>

          <div
            style={{
              display:
                "grid",

              gap:
                8,
            }}
          >

            <label
              style={{
                display:
                  "flex",

                alignItems:
                  "center",

                gap:
                  8,

                fontSize:
                  13,

                color:
                  "#475569",
              }}
            >

              <input
                type="checkbox"
                checked={
                  form.sendEmail
                }
                onChange={
                  (
                    event
                  ) =>
                    updateForm(
                      "sendEmail",
                      event
                        .target
                        .checked
                    )
                }
              />

              <Mail
                size={16}
              />

              Send email when
              SMTP is configured

            </label>

            <label
              style={{
                display:
                  "flex",

                alignItems:
                  "center",

                gap:
                  8,

                fontSize:
                  13,

                color:
                  "#475569",
              }}
            >

              <input
                type="checkbox"
                checked={
                  form.sendSms
                }
                onChange={
                  (
                    event
                  ) =>
                    updateForm(
                      "sendSms",
                      event
                        .target
                        .checked
                    )
                }
              />

              <MessageSquareText
                size={16}
              />

              Send short SMS alert

            </label>

          </div>

          <button
            type="submit"
            disabled={
              saving
            }
            style={{
              ...styles.button,

              justifyContent:
                "center",

              background:
                "#2563eb",

              color:
                "white",

              opacity:
                saving
                  ? 0.65
                  : 1,
            }}
          >

            {
              form.targetMode ===
              "EMPLOYEE"
                ? (
                    <Send
                      size={16}
                    />
                  )
                : (
                    <Radio
                      size={16}
                    />
                  )
            }

            {
              saving
                ? "Sending..."
                : "Send communication"
            }

          </button>

        </form>

        <div
          style={{
            display:
              "grid",

            gap:
              14,
          }}
        >

          <div
            style={{
              ...styles.card,

              padding:
                16,

              display:
                "flex",

              alignItems:
                "center",

              gap:
                12,

              flexWrap:
                "wrap",
            }}
          >

            <Users
              size={18}
              color="#2563eb"
            />

            <select
              style={{
                ...styles.input,

                flex:
                  "1 1 280px",

                width:
                  "auto",
              }}
              value={
                selectedEmployeeId
              }
              onChange={
                (
                  event
                ) => {

                  const value =
                    event.target.value;

                  setSelectedEmployeeId(
                    value
                  );

                  setForm(
                    (
                      current
                    ) => ({
                      ...current,
                      employeeId:
                        value,
                    })
                  );
                }
              }
            >

              <option
                value=""
              >
                Select employee
                inbox
              </option>

              {
                employees.map(
                  (
                    employee
                  ) => (

                    <option
                      key={
                        employee.id
                      }
                      value={
                        employee.id
                      }
                    >
                      {
                        employee.firstName
                      }{" "}
                      {
                        employee.lastName
                      }
                      {" — "}
                      {
                        employee.email
                      }
                    </option>
                  )
                )
              }

            </select>

            <label
              style={{
                display:
                  "flex",

                alignItems:
                  "center",

                gap:
                  7,

                fontSize:
                  13,

                color:
                  "#475569",
              }}
            >

              <input
                type="checkbox"
                checked={
                  unreadOnly
                }
                onChange={
                  (
                    event
                  ) =>
                    setUnreadOnly(
                      event
                        .target
                        .checked
                    )
                }
              />

              Unread only

            </label>

            <button
              type="button"
              onClick={
                markAllRead
              }
              disabled={
                !selectedEmployeeId ||
                !summary.unread
              }
              style={{
                ...styles.button,

                background:
                  "#f1f5f9",

                color:
                  "#334155",
              }}
            >

              <CheckCheck
                size={16}
              />

              Mark all read

            </button>

          </div>

          <div
            style={{
              ...styles.card,
              overflow:
                "hidden",
            }}
          >

            <div
              style={{
                padding:
                  "15px 18px",

                borderBottom:
                  "1px solid #e2e8f0",

                fontWeight:
                  800,

                color:
                  "#0f172a",
              }}
            >

              {
                selectedEmployee
                  ? `${selectedEmployee.firstName} ${selectedEmployee.lastName}'s inbox`
                  : "Employee inbox"
              }

            </div>

            {
              loading
                ? (

                    <div
                      style={{
                        padding:
                          30,

                        ...styles.muted,
                      }}
                    >
                      Loading
                      communication
                      center...
                    </div>
                  )

                : notifications.length ===
                  0
                ? (

                    <div
                      style={{
                        padding:
                          34,

                        textAlign:
                          "center",

                        ...styles.muted,
                      }}
                    >
                      No notifications
                      found for this
                      employee.
                    </div>
                  )

                : notifications.map(
                    (
                      notification
                    ) => (

                      <div
                        key={
                          notification.id
                        }
                        style={{
                          padding:
                            16,

                          borderBottom:
                            "1px solid #f1f5f9",

                          background:
                            notification.read
                              ? "#fff"
                              : "#f8fbff",
                        }}
                      >

                        <div
                          style={{
                            display:
                              "flex",

                            justifyContent:
                              "space-between",

                            gap:
                              12,

                            alignItems:
                              "flex-start",
                          }}
                        >

                          <div
                            style={{
                              minWidth:
                                0,
                            }}
                          >

                            <div
                              style={{
                                display:
                                  "flex",

                                gap:
                                  8,

                                alignItems:
                                  "center",

                                flexWrap:
                                  "wrap",
                              }}
                            >

                              <strong
                                style={{
                                  color:
                                    "#0f172a",
                                }}
                              >
                                {
                                  notification.title
                                }
                              </strong>

                              <span
                                style={{
                                  ...typeStyle(
                                    notification.type
                                  ),

                                  padding:
                                    "3px 8px",

                                  borderRadius:
                                    999,

                                  fontSize:
                                    10,

                                  fontWeight:
                                    800,
                                }}
                              >
                                {
                                  notification.type ||
                                  "GENERAL"
                                }
                              </span>

                              {
                                !notification.read && (

                                  <span
                                    style={{
                                      background:
                                        "#2563eb",

                                      width:
                                        7,

                                      height:
                                        7,

                                      borderRadius:
                                        999,
                                    }}
                                  />
                                )
                              }

                            </div>

                            <div
                              style={{
                                marginTop: 7,
                                display: "flex",
                                gap: 6,
                                alignItems: "center",
                                flexWrap: "wrap",
                                color: "#64748b",
                                fontSize: 11,
                                fontWeight: 700,
                              }}
                            >
                              <span>Sent by @{notification.senderUsername || "SYSTEM"}</span>
                              <span>•</span>
                              <span>{notification.senderRole || "SYSTEM"}</span>
                            </div>

                            <div
                              style={{
                                marginTop:
                                  7,

                                color:
                                  "#475569",

                                fontSize:
                                  13,

                                lineHeight:
                                  1.55,
                              }}
                            >
                              {
                                notification.message
                              }
                            </div>

                            <div
                              style={{
                                marginTop:
                                  8,

                                color:
                                  "#94a3b8",

                                fontSize:
                                  11,
                              }}
                            >
                              {
                                formatDate(
                                  notification.createdAt
                                )
                              }
                            </div>

                          </div>

                          <div
                            style={{
                              display:
                                "flex",

                              gap:
                                6,
                            }}
                          >

                            {
                              !notification.read && (

                                <button
                                  type="button"
                                  title="Mark as read"
                                  onClick={
                                    () =>
                                      markRead(
                                        notification.id
                                      )
                                  }
                                  style={{
                                    ...styles.button,

                                    padding:
                                      8,

                                    background:
                                      "#eff6ff",

                                    color:
                                      "#1d4ed8",
                                  }}
                                >
                                  <CheckCheck
                                    size={15}
                                  />
                                </button>
                              )
                            }

                            <button
                              type="button"
                              title="Delete"
                              onClick={
                                () =>
                                  deleteNotification(
                                    notification.id
                                  )
                              }
                              style={{
                                ...styles.button,

                                padding:
                                  8,

                                background:
                                  "#fef2f2",

                                color:
                                  "#dc2626",
                              }}
                            >
                              <Trash2
                                size={15}
                              />
                            </button>

                          </div>

                        </div>

                      </div>
                    )
                  )
            }

          </div>

        </div>

      </div>

    </div>
  );
}