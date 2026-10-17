import React, { useEffect, useMemo, useState } from "react";
import { apiFetch, apiUrl } from "./api/apiClient";

function getToken() {
  try {
    const saved =
      window.localStorage.getItem(
        "nexushr-user"
      );

    const user = saved
      ? JSON.parse(saved)
      : null;

    return user?.token || "";
  } catch {
    return "";
  }
}

function getFilename(
  response,
  fallback
) {
  const disposition =
    response.headers.get(
      "content-disposition"
    );

  if (!disposition) {
    return fallback;
  }

  const match =
    disposition.match(
      /filename="?([^"]+)"?/i
    );

  return match?.[1] || fallback;
}

async function downloadReport(
  endpoint,
  fallbackFilename
) {
  const token = getToken();

  if (!token) {
    throw new Error(
      "Authentication token not found. Please sign in again."
    );
  }

  const response = await fetch(
    apiUrl(endpoint),
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    const message =
      await response.text();

    throw new Error(
      message ||
        `Download failed with status ${response.status}.`
    );
  }

  const blob = await response.blob();

  const url =
    window.URL.createObjectURL(blob);

  const anchor =
    document.createElement("a");

  anchor.href = url;
  anchor.download = getFilename(
    response,
    fallbackFilename
  );

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  window.URL.revokeObjectURL(url);
}

export default function Reports() {
  const [summary, setSummary] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [downloading, setDownloading] =
    useState("");

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState("");

  async function loadSummary() {
    try {
      setLoading(true);
      setError("");

      const data = await apiFetch(
        "/reports/summary"
      );

      setSummary(data || null);
    } catch (err) {
      console.error(
        "Report summary load error:",
        err
      );

      setError(
        err.message ||
          "Unable to load reports summary."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSummary();
  }, []);

  async function handleDownload(
    type
  ) {
    try {
      setDownloading(type);
      setError("");
      setSuccess("");

      if (type === "pdf") {
        await downloadReport(
          "/reports/export/pdf",
          "nexushr-workforce-report.pdf"
        );

        setSuccess(
          "PDF report downloaded successfully."
        );
      } else {
        await downloadReport(
          "/reports/export/excel",
          "nexushr-workforce-report.xlsx"
        );

        setSuccess(
          "Excel report downloaded successfully."
        );
      }
    } catch (err) {
      console.error(
        "Report download error:",
        err
      );

      setError(
        err.message ||
          "Unable to download report."
      );
    } finally {
      setDownloading("");
    }
  }

  const departmentRows =
    useMemo(() => {
      return Object.entries(
        summary?.departmentStats || {}
      ).sort((a, b) =>
        a[0].localeCompare(b[0])
      );
    }, [summary]);

  const cardStyle = {
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: "12px",
    boxShadow:
      "0 1px 3px rgba(0,0,0,0.05)",
  };

  function metricCard(
    label,
    value,
    subtitle
  ) {
    return (
      <div
        key={label}
        style={{
          ...cardStyle,
          padding: "18px",
        }}
      >
        <div
          style={{
            color: "#6b7280",
            fontSize: "12px",
            fontWeight: "600",
            marginBottom: "8px",
          }}
        >
          {label}
        </div>

        <div
          style={{
            fontSize: "26px",
            fontWeight: "800",
            color: "#111827",
          }}
        >
          {value}
        </div>

        <div
          style={{
            marginTop: "6px",
            color: "#9ca3af",
            fontSize: "12px",
          }}
        >
          {subtitle}
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div
        style={{
          padding: "24px",
        }}
      >
        <div
          style={{
            ...cardStyle,
            padding: "40px",
            textAlign: "center",
            color: "#6b7280",
          }}
        >
          Loading reports...
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        padding: "24px",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent:
            "space-between",
          alignItems: "flex-start",
          flexWrap: "wrap",
          gap: "16px",
          marginBottom: "24px",
        }}
      >
        <div>
          <h1
            style={{
              margin: 0,
            }}
          >
            Reports & Export
          </h1>

          <p
            style={{
              marginTop: "8px",
              color: "#6b7280",
              maxWidth: "760px",
            }}
          >
            Review workforce,
            attendance, leave, payroll,
            performance, and department
            summaries and export them as
            PDF or Excel.
          </p>
        </div>

        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "10px",
          }}
        >
          <button
            type="button"
            onClick={loadSummary}
            style={{
              padding: "10px 16px",
              border:
                "1px solid #d1d5db",
              borderRadius: "7px",
              background: "#fff",
              color: "#374151",
              fontWeight: "600",
              cursor: "pointer",
            }}
          >
            Refresh
          </button>

          <button
            type="button"
            disabled={
              downloading !== ""
            }
            onClick={() =>
              handleDownload("pdf")
            }
            style={{
              padding: "10px 16px",
              border: "none",
              borderRadius: "7px",
              background: "#dc2626",
              color: "#fff",
              fontWeight: "600",
              cursor:
                downloading !== ""
                  ? "not-allowed"
                  : "pointer",
            }}
          >
            {downloading === "pdf"
              ? "Downloading..."
              : "Export PDF"}
          </button>

          <button
            type="button"
            disabled={
              downloading !== ""
            }
            onClick={() =>
              handleDownload("excel")
            }
            style={{
              padding: "10px 16px",
              border: "none",
              borderRadius: "7px",
              background: "#059669",
              color: "#fff",
              fontWeight: "600",
              cursor:
                downloading !== ""
                  ? "not-allowed"
                  : "pointer",
            }}
          >
            {downloading === "excel"
              ? "Downloading..."
              : "Export Excel"}
          </button>
        </div>
      </div>

      {error && (
        <div
          style={{
            marginBottom: "18px",
            padding: "14px 16px",
            borderRadius: "8px",
            background: "#fee2e2",
            border:
              "1px solid #fecaca",
            color: "#991b1b",
          }}
        >
          {error}
        </div>
      )}

      {success && (
        <div
          style={{
            marginBottom: "18px",
            padding: "14px 16px",
            borderRadius: "8px",
            background: "#dcfce7",
            border:
              "1px solid #bbf7d0",
            color: "#166534",
          }}
        >
          {success}
        </div>
      )}

      {!summary ? (
        <div
          style={{
            ...cardStyle,
            padding: "40px",
            textAlign: "center",
            color: "#6b7280",
          }}
        >
          No report data available.
        </div>
      ) : (
        <>
          <h2>
            Workforce Summary
          </h2>

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(170px, 1fr))",
              gap: "16px",
              marginBottom: "28px",
            }}
          >
            {metricCard(
              "Total Employees",
              summary.totalEmployees,
              "All employee records"
            )}

            {metricCard(
              "Active Employees",
              summary.activeEmployees,
              "Currently active"
            )}

            {metricCard(
              "Inactive Employees",
              summary.inactiveEmployees,
              "Inactive workforce records"
            )}
          </div>

          <h2>
            Attendance & Leave
          </h2>

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(170px, 1fr))",
              gap: "16px",
              marginBottom: "28px",
            }}
          >
            {metricCard(
              "Present Today",
              summary.todayPresent,
              "Attendance marked present"
            )}

            {metricCard(
              "Absent Today",
              summary.todayAbsent,
              "Attendance marked absent"
            )}

            {metricCard(
              "Pending Leaves",
              summary.pendingLeaves,
              "Waiting for approval"
            )}

            {metricCard(
              "Approved Leaves",
              summary.approvedLeaves,
              "Approved leave requests"
            )}
          </div>

          <h2>
            Payroll Summary
          </h2>

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(170px, 1fr))",
              gap: "16px",
              marginBottom: "28px",
            }}
          >
            {metricCard(
              "Payroll Records",
              summary.totalPayrollRecords,
              "All payroll entries"
            )}

            {metricCard(
              "Draft Payrolls",
              summary.draftPayrolls,
              "Not yet processed"
            )}

            {metricCard(
              "Processed Payrolls",
              summary.processedPayrolls,
              "Processed payroll entries"
            )}

            {metricCard(
              "Paid Payrolls",
              summary.paidPayrolls,
              "Completed salary payments"
            )}
          </div>

          <h2>
            Performance Summary
          </h2>

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(170px, 1fr))",
              gap: "16px",
              marginBottom: "28px",
            }}
          >
            {metricCard(
              "Performance Reviews",
              summary.totalPerformanceReviews,
              "All employee reviews"
            )}

            {metricCard(
              "Draft Reviews",
              summary.draftPerformanceReviews,
              "Still being prepared"
            )}

            {metricCard(
              "Submitted Reviews",
              summary.submittedPerformanceReviews,
              "Submitted for review"
            )}

            {metricCard(
              "Completed Reviews",
              summary.completedPerformanceReviews,
              "Finished reviews"
            )}

            {metricCard(
              "Average Rating",
              `${Number(
                summary.averagePerformanceRating ||
                  0
              ).toFixed(2)}/5`,
              "Average employee rating"
            )}
          </div>

          <h2>
            Employees by Department
          </h2>

          <div
            style={{
              ...cardStyle,
              overflowX: "auto",
            }}
          >
            <table
              style={{
                width: "100%",
                borderCollapse:
                  "collapse",
                minWidth: "500px",
              }}
            >
              <thead>
                <tr>
                  <th
                    style={{
                      padding: "14px",
                      textAlign: "left",
                      background:
                        "#f9fafb",
                      borderBottom:
                        "1px solid #e5e7eb",
                    }}
                  >
                    Department
                  </th>

                  <th
                    style={{
                      padding: "14px",
                      textAlign: "left",
                      background:
                        "#f9fafb",
                      borderBottom:
                        "1px solid #e5e7eb",
                    }}
                  >
                    Employees
                  </th>
                </tr>
              </thead>

              <tbody>
                {departmentRows.length ===
                0 ? (
                  <tr>
                    <td
                      colSpan="2"
                      style={{
                        padding: "25px",
                        color:
                          "#6b7280",
                        textAlign:
                          "center",
                      }}
                    >
                      No department data.
                    </td>
                  </tr>
                ) : (
                  departmentRows.map(
                    ([
                      department,
                      count,
                    ]) => (
                      <tr
                        key={
                          department
                        }
                      >
                        <td
                          style={{
                            padding:
                              "14px",
                            borderBottom:
                              "1px solid #f3f4f6",
                          }}
                        >
                          {department}
                        </td>

                        <td
                          style={{
                            padding:
                              "14px",
                            borderBottom:
                              "1px solid #f3f4f6",
                            fontWeight:
                              "700",
                          }}
                        >
                          {count}
                        </td>
                      </tr>
                    )
                  )
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
