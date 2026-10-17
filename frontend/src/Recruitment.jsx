import React, { useEffect, useMemo, useState } from "react";
import { apiFetch } from "./api/apiClient";

const initialForm = {
  title: "",
  department: "",
  location: "",
  employmentType: "FULL_TIME",
  description: "",
  requirements: "",
  salaryMin: "",
  salaryMax: "",
  status: "DRAFT",
  postedDate: new Date().toISOString().split("T")[0],
  closingDate: "",
  notifyEmployees: false,
  alertSubject: "",
  alertMessage: "",
  sendAlertEmail: true,
  sendAlertSms: false,
};

function Recruitment() {
  const [jobs, setJobs] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [alertingId, setAlertingId] = useState(null);

  useEffect(() => {
    loadJobs();
  }, []);

  async function loadJobs() {
    try {
      setLoading(true);
      setError("");

      const data = await apiFetch("/jobs");

      setJobs(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to load jobs:", err);
      setError(err.message || "Failed to load jobs.");
    } finally {
      setLoading(false);
    }
  }

  function handleChange(event) {
    const { name, value, type, checked } = event.target;

    setForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  function resetForm() {
    setForm({
      ...initialForm,
      postedDate: new Date().toISOString().split("T")[0],
    });

    setEditingId(null);
  }

  function startEdit(job) {
    setEditingId(job.id);

    setForm({
      title: job.title || "",
      department: job.department || "",
      location: job.location || "",
      employmentType: job.employmentType || "FULL_TIME",
      description: job.description || "",
      requirements: job.requirements || "",
      salaryMin: job.salaryMin ?? "",
      salaryMax: job.salaryMax ?? "",
      status: job.status || "DRAFT",
      postedDate: job.postedDate || "",
      closingDate: job.closingDate || "",
      notifyEmployees: false,
      alertSubject: `Job Opportunity: ${job.title || ""}`,
      alertMessage: `A new opportunity is available in ${job.department || "our organization"}. Please review the attached job alert PDF for complete details.`,
      sendAlertEmail: true,
      sendAlertSms: false,
    });

    setMessage("");
    setError("");

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  function cancelEdit() {
    resetForm();
    setMessage("");
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!form.title.trim()) {
      setError("Job title is required.");
      return;
    }

    if (!form.department.trim()) {
      setError("Department is required.");
      return;
    }

    if (!form.location.trim()) {
      setError("Location is required.");
      return;
    }

    if (!form.postedDate) {
      setError("Posted date is required.");
      return;
    }

    if (
      form.salaryMin !== "" &&
      form.salaryMax !== "" &&
      Number(form.salaryMin) > Number(form.salaryMax)
    ) {
      setError("Minimum salary cannot be greater than maximum salary.");
      return;
    }

    if (
      form.closingDate &&
      form.closingDate < form.postedDate
    ) {
      setError("Closing date cannot be before posted date.");
      return;
    }

    const payload = {
      title: form.title.trim(),
      department: form.department.trim(),
      location: form.location.trim(),
      employmentType: form.employmentType,
      description: form.description.trim(),
      requirements: form.requirements.trim(),
      salaryMin:
        form.salaryMin === ""
          ? null
          : Number(form.salaryMin),
      salaryMax:
        form.salaryMax === ""
          ? null
          : Number(form.salaryMax),
      status: form.status,
      postedDate: form.postedDate,
      closingDate: form.closingDate || null,
    };

    try {
      setSaving(true);

      let savedJob;

      if (editingId) {
        savedJob = await apiFetch(`/jobs/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });

        setMessage("Job updated successfully.");
      } else {
        savedJob = await apiFetch("/jobs", {
          method: "POST",
          body: JSON.stringify(payload),
        });

        setMessage("Job created successfully.");
      }

      if (form.notifyEmployees && savedJob?.id) {
        const alertResult = await apiFetch(`/jobs/${savedJob.id}/announce`, {
          method: "POST",
          body: JSON.stringify({
            subject:
              form.alertSubject.trim() ||
              `New Job Opportunity: ${savedJob.title}`,
            message:
              form.alertMessage.trim() ||
              `A new internal job opportunity is available: ${savedJob.title}. Please review your email and the attached PDF for complete details.`,
            sendEmail: form.sendAlertEmail,
            sendSms: form.sendAlertSms,
          }),
        });

        setMessage(
          `${editingId ? "Job updated" : "Job created"} and ${
            alertResult?.message || "employee alert sent"
          }.`
        );
      }

      resetForm();
      await loadJobs();
    } catch (err) {
      console.error("Failed to save job:", err);
      setError(err.message || "Failed to save job.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id) {
    const confirmed = window.confirm(
      "Are you sure you want to delete this job?"
    );

    if (!confirmed) {
      return;
    }

    try {
      setDeletingId(id);
      setMessage("");
      setError("");

      await apiFetch(`/jobs/${id}`, {
        method: "DELETE",
      });

      setMessage("Job deleted successfully.");

      if (editingId === id) {
        resetForm();
      }

      await loadJobs();
    } catch (err) {
      console.error("Failed to delete job:", err);
      setError(err.message || "Failed to delete job.");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleAnnounceJob(job) {
    const confirmed = window.confirm(
      `Send this job alert to all active employees?\n\n${job.title}\n\nEmail will include a generated PDF. SMS is kept short.`
    );
    if (!confirmed) return;

    try {
      setAlertingId(job.id);
      setMessage("");
      setError("");

      const result = await apiFetch(`/jobs/${job.id}/announce`, {
        method: "POST",
        body: JSON.stringify({
          subject: `New Job Opportunity: ${job.title}`,
          message: `A new internal job opportunity is available in ${job.department}. Please review the attached NexusHR PDF for complete details and requirements.`,
          sendEmail: true,
          sendSms: false,
        }),
      });

      setMessage(result?.message || "Job alert sent successfully.");
    } catch (err) {
      setError(err.message || "Failed to send job alert.");
    } finally {
      setAlertingId(null);
    }
  }

  const filteredJobs = useMemo(() => {
    const search = searchTerm.trim().toLowerCase();

    return jobs.filter((job) => {
      const matchesSearch =
        !search ||
        job.title?.toLowerCase().includes(search) ||
        job.department?.toLowerCase().includes(search) ||
        job.location?.toLowerCase().includes(search);

      const matchesStatus =
        statusFilter === "ALL" ||
        job.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [jobs, searchTerm, statusFilter]);

  const openJobs = jobs.filter(
    (job) => job.status === "OPEN"
  ).length;

  const draftJobs = jobs.filter(
    (job) => job.status === "DRAFT"
  ).length;

  const closedJobs = jobs.filter(
    (job) => job.status === "CLOSED"
  ).length;

  function formatEmploymentType(type) {
    const labels = {
      FULL_TIME: "Full Time",
      PART_TIME: "Part Time",
      CONTRACT: "Contract",
      INTERNSHIP: "Internship",
    };

    return labels[type] || type || "—";
  }

  function formatStatus(status) {
    const labels = {
      DRAFT: "Draft",
      OPEN: "Open",
      CLOSED: "Closed",
    };

    return labels[status] || status || "—";
  }

  function publicCareersUrl(jobId) {
    const base = typeof window !== "undefined" ? window.location.origin : "";
    return `${base}/careers${jobId ? `?job=${jobId}` : ""}`;
  }

  async function copyPublicLink(job) {
    const link = publicCareersUrl(job?.id);
    try {
      await navigator.clipboard.writeText(link);
      setMessage(`Public application link copied for ${job?.title || "careers portal"}.`);
    } catch {
      window.prompt("Copy this public application link", link);
    }
  }

  function formatSalary(min, max) {
    if (min == null && max == null) {
      return "Not specified";
    }

    const format = (value) =>
      Number(value).toLocaleString("en-IN");

    if (min != null && max != null) {
      return `₹${format(min)} - ₹${format(max)}`;
    }

    if (min != null) {
      return `From ₹${format(min)}`;
    }

    return `Up to ₹${format(max)}`;
  }

  return (
    <div
      style={{
        padding: "24px",
        maxWidth: "1400px",
        margin: "0 auto",
      }}
    >
      <div
        style={{
          marginBottom: "24px",
        }}
      >
        <h1
          style={{
            margin: 0,
            fontSize: "28px",
            fontWeight: 700,
          }}
        >
          Recruitment
        </h1>

        <p
          style={{
            marginTop: "8px",
            color: "#64748b",
          }}
        >
          Manage job openings and recruitment opportunities.
        </p>
      </div>

      <div style={{background:"linear-gradient(135deg,#eff6ff,#f5f3ff)",border:"1px solid #dbeafe",borderRadius:14,padding:16,marginBottom:18,display:"flex",justifyContent:"space-between",gap:12,alignItems:"center",flexWrap:"wrap"}}>
        <div><strong style={{display:"block",fontSize:16}}>Public Application Portal</strong><span style={{fontSize:12,color:"#64748b"}}>Share a Google-Form-style NexusHR careers link with students and external candidates. No login is required.</span></div>
        <div style={{display:"flex",gap:8}}><button type="button" onClick={()=>copyPublicLink(null)} style={{border:"1px solid #cbd5e1",background:"#fff",borderRadius:9,padding:"9px 12px",fontWeight:700,cursor:"pointer"}}>Copy careers link</button><button type="button" onClick={()=>window.open("/careers","_blank")} style={{border:0,background:"#2563eb",color:"#fff",borderRadius:9,padding:"9px 12px",fontWeight:700,cursor:"pointer"}}>Open public form</button></div>
      </div>

      {message && (
        <div
          style={{
            padding: "12px 16px",
            marginBottom: "16px",
            borderRadius: "8px",
            background: "#ecfdf5",
            color: "#047857",
            border: "1px solid #a7f3d0",
          }}
        >
          {message}
        </div>
      )}

      {error && (
        <div
          style={{
            padding: "12px 16px",
            marginBottom: "16px",
            borderRadius: "8px",
            background: "#fef2f2",
            color: "#b91c1c",
            border: "1px solid #fecaca",
          }}
        >
          {error}
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns:
            "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "16px",
          marginBottom: "24px",
        }}
      >
        <div
          style={{
            padding: "20px",
            borderRadius: "12px",
            background: "#ffffff",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ color: "#64748b", fontSize: "14px" }}>
            Total Jobs
          </div>

          <div
            style={{
              marginTop: "8px",
              fontSize: "28px",
              fontWeight: 700,
            }}
          >
            {jobs.length}
          </div>
        </div>

        <div
          style={{
            padding: "20px",
            borderRadius: "12px",
            background: "#ffffff",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ color: "#64748b", fontSize: "14px" }}>
            Open Jobs
          </div>

          <div
            style={{
              marginTop: "8px",
              fontSize: "28px",
              fontWeight: 700,
            }}
          >
            {openJobs}
          </div>
        </div>

        <div
          style={{
            padding: "20px",
            borderRadius: "12px",
            background: "#ffffff",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ color: "#64748b", fontSize: "14px" }}>
            Draft Jobs
          </div>

          <div
            style={{
              marginTop: "8px",
              fontSize: "28px",
              fontWeight: 700,
            }}
          >
            {draftJobs}
          </div>
        </div>

        <div
          style={{
            padding: "20px",
            borderRadius: "12px",
            background: "#ffffff",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ color: "#64748b", fontSize: "14px" }}>
            Closed Jobs
          </div>

          <div
            style={{
              marginTop: "8px",
              fontSize: "28px",
              fontWeight: 700,
            }}
          >
            {closedJobs}
          </div>
        </div>
      </div>

      <div
        style={{
          background: "#ffffff",
          border: "1px solid #e2e8f0",
          borderRadius: "12px",
          padding: "24px",
          marginBottom: "24px",
        }}
      >
        <h2
          style={{
            marginTop: 0,
            marginBottom: "20px",
            fontSize: "20px",
          }}
        >
          {editingId ? "Edit Job" : "Create Job"}
        </h2>

        <form onSubmit={handleSubmit}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(240px, 1fr))",
              gap: "16px",
            }}
          >
            <div>
              <label>Job Title</label>

              <input
                type="text"
                name="title"
                value={form.title}
                onChange={handleChange}
                placeholder="e.g. Java Full Stack Developer"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Department</label>

              <input
                type="text"
                name="department"
                value={form.department}
                onChange={handleChange}
                placeholder="e.g. Engineering"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Location</label>

              <input
                type="text"
                name="location"
                value={form.location}
                onChange={handleChange}
                placeholder="e.g. Pune, India"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Employment Type</label>

              <select
                name="employmentType"
                value={form.employmentType}
                onChange={handleChange}
                style={inputStyle}
              >
                <option value="FULL_TIME">
                  Full Time
                </option>

                <option value="PART_TIME">
                  Part Time
                </option>

                <option value="CONTRACT">
                  Contract
                </option>

                <option value="INTERNSHIP">
                  Internship
                </option>
              </select>
            </div>

            <div>
              <label>Salary Min</label>

              <input
                type="number"
                name="salaryMin"
                value={form.salaryMin}
                onChange={handleChange}
                placeholder="e.g. 500000"
                min="0"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Salary Max</label>

              <input
                type="number"
                name="salaryMax"
                value={form.salaryMax}
                onChange={handleChange}
                placeholder="e.g. 900000"
                min="0"
                style={inputStyle}
              />
            </div>

            <div>
              <label>Status</label>

              <select
                name="status"
                value={form.status}
                onChange={handleChange}
                style={inputStyle}
              >
                <option value="DRAFT">Draft</option>
                <option value="OPEN">Open</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>

            <div>
              <label>Posted Date</label>

              <input
                type="date"
                name="postedDate"
                value={form.postedDate}
                onChange={handleChange}
                style={inputStyle}
              />
            </div>

            <div>
              <label>Closing Date</label>

              <input
                type="date"
                name="closingDate"
                value={form.closingDate}
                onChange={handleChange}
                style={inputStyle}
              />
            </div>
          </div>

          <div style={{ marginTop: "16px" }}>
            <label>Description</label>

            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              placeholder="Describe the role..."
              rows="4"
              style={textareaStyle}
            />
          </div>

          <div style={{ marginTop: "16px" }}>
            <label>Requirements</label>

            <textarea
              name="requirements"
              value={form.requirements}
              onChange={handleChange}
              placeholder="Enter required skills and qualifications..."
              rows="4"
              style={textareaStyle}
            />
          </div>

          <div
            style={{
              marginTop: "20px",
              padding: "18px",
              border: "1px solid #dbeafe",
              borderRadius: "10px",
              background: "#f8fbff",
            }}
          >
            <label style={{ display: "flex", alignItems: "center", gap: "9px", fontWeight: 700 }}>
              <input
                type="checkbox"
                name="notifyEmployees"
                checked={form.notifyEmployees}
                onChange={handleChange}
              />
              Send job alert to all active employees after saving
            </label>

            {form.notifyEmployees && (
              <div style={{ marginTop: "14px", display: "grid", gap: "14px" }}>
                <div>
                  <label>Email / Notification Subject</label>
                  <input
                    type="text"
                    name="alertSubject"
                    value={form.alertSubject}
                    onChange={handleChange}
                    placeholder={`New Job Opportunity: ${form.title || "Position"}`}
                    style={inputStyle}
                  />
                </div>

                <div>
                  <label>Employee Message</label>
                  <textarea
                    name="alertMessage"
                    value={form.alertMessage}
                    onChange={handleChange}
                    placeholder="Write the job alert message employees should receive..."
                    rows="3"
                    style={textareaStyle}
                  />
                </div>

                <div style={{ display: "flex", gap: "20px", flexWrap: "wrap" }}>
                  <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      name="sendAlertEmail"
                      checked={form.sendAlertEmail}
                      onChange={handleChange}
                    />
                    Email + job PDF (recommended)
                  </label>

                  <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      name="sendAlertSms"
                      checked={form.sendAlertSms}
                      onChange={handleChange}
                    />
                    Short SMS alert
                  </label>
                </div>

                <small style={{ color: "#64748b" }}>
                  Employees also receive the alert inside NexusHR and can see the sender username and role.
                </small>
              </div>
            )}
          </div>

          <div
            style={{
              display: "flex",
              gap: "12px",
              marginTop: "20px",
            }}
          >
            <button
              type="submit"
              disabled={saving}
              style={primaryButtonStyle}
            >
              {saving
                ? "Saving..."
                : editingId
                  ? "Update Job"
                  : "Create Job"}
            </button>

            {editingId && (
              <button
                type="button"
                onClick={cancelEdit}
                style={secondaryButtonStyle}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      <div
        style={{
          background: "#ffffff",
          border: "1px solid #e2e8f0",
          borderRadius: "12px",
          padding: "24px",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "16px",
            flexWrap: "wrap",
            marginBottom: "20px",
          }}
        >
          <h2
            style={{
              margin: 0,
              fontSize: "20px",
            }}
          >
            Job Openings
          </h2>

          <div
            style={{
              display: "flex",
              gap: "12px",
              flexWrap: "wrap",
            }}
          >
            <input
              type="text"
              value={searchTerm}
              onChange={(event) =>
                setSearchTerm(event.target.value)
              }
              placeholder="Search jobs..."
              style={{
                ...inputStyle,
                width: "220px",
              }}
            />

            <select
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value)
              }
              style={{
                ...inputStyle,
                width: "150px",
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="OPEN">Open</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
        </div>

        {loading ? (
          <p>Loading jobs...</p>
        ) : filteredJobs.length === 0 ? (
          <div
            style={{
              padding: "40px 20px",
              textAlign: "center",
              color: "#64748b",
              border: "1px dashed #cbd5e1",
              borderRadius: "10px",
            }}
          >
            <div
              style={{
                fontSize: "18px",
                fontWeight: 600,
                marginBottom: "8px",
              }}
            >
              No jobs found
            </div>

            <div>
              Create your first job opening using the form above.
            </div>
          </div>
        ) : (
          <div
            style={{
              overflowX: "auto",
            }}
          >
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                minWidth: "1000px",
              }}
            >
              <thead>
                <tr>
                  <th style={tableHeaderStyle}>Job</th>
                  <th style={tableHeaderStyle}>Department</th>
                  <th style={tableHeaderStyle}>Location</th>
                  <th style={tableHeaderStyle}>Employment</th>
                  <th style={tableHeaderStyle}>Salary</th>
                  <th style={tableHeaderStyle}>Status</th>
                  <th style={tableHeaderStyle}>Posted</th>
                  <th style={tableHeaderStyle}>Actions</th>
                </tr>
              </thead>

              <tbody>
                {filteredJobs.map((job) => (
                  <tr key={job.id}>
                    <td style={tableCellStyle}>
                      <div
                        style={{
                          fontWeight: 600,
                        }}
                      >
                        {job.title}
                      </div>

                      {job.description && (
                        <div
                          style={{
                            marginTop: "4px",
                            color: "#64748b",
                            fontSize: "13px",
                            maxWidth: "260px",
                          }}
                        >
                          {job.description.length > 90
                            ? `${job.description.substring(
                                0,
                                90
                              )}...`
                            : job.description}
                        </div>
                      )}
                    </td>

                    <td style={tableCellStyle}>
                      {job.department}
                    </td>

                    <td style={tableCellStyle}>
                      {job.location}
                    </td>

                    <td style={tableCellStyle}>
                      {formatEmploymentType(
                        job.employmentType
                      )}
                    </td>

                    <td style={tableCellStyle}>
                      {formatSalary(
                        job.salaryMin,
                        job.salaryMax
                      )}
                    </td>

                    <td style={tableCellStyle}>
                      <span
                        style={{
                          ...statusBadgeStyle,
                          ...(job.status === "OPEN"
                            ? openBadgeStyle
                            : job.status === "CLOSED"
                              ? closedBadgeStyle
                              : draftBadgeStyle),
                        }}
                      >
                        {formatStatus(job.status)}
                      </span>
                    </td>

                    <td style={tableCellStyle}>
                      {job.postedDate || "—"}
                    </td>

                    <td style={tableCellStyle}>
                      <div
                        style={{
                          display: "flex",
                          gap: "8px",
                        }}
                      >
                        <button
                          type="button"
                          onClick={() => handleAnnounceJob(job)}
                          disabled={alertingId === job.id}
                          style={alertButtonStyle}
                        >
                          {alertingId === job.id ? "Sending..." : "Send Alert"}
                        </button>

                        <button
                          type="button"
                          onClick={() => copyPublicLink(job)}
                          style={editButtonStyle}
                        >
                          Copy Apply Link
                        </button>

                        <button
                          type="button"
                          onClick={() => startEdit(job)}
                          style={editButtonStyle}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            handleDelete(job.id)
                          }
                          disabled={deletingId === job.id}
                          style={deleteButtonStyle}
                        >
                          {deletingId === job.id
                            ? "Deleting..."
                            : "Delete"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

const inputStyle = {
  width: "100%",
  boxSizing: "border-box",
  padding: "10px 12px",
  marginTop: "6px",
  border: "1px solid #cbd5e1",
  borderRadius: "8px",
  fontSize: "14px",
  background: "#ffffff",
};

const textareaStyle = {
  ...inputStyle,
  resize: "vertical",
};

const primaryButtonStyle = {
  border: "none",
  borderRadius: "8px",
  padding: "10px 18px",
  background: "#2563eb",
  color: "#ffffff",
  cursor: "pointer",
  fontWeight: 600,
};

const secondaryButtonStyle = {
  border: "1px solid #cbd5e1",
  borderRadius: "8px",
  padding: "10px 18px",
  background: "#ffffff",
  color: "#334155",
  cursor: "pointer",
  fontWeight: 600,
};

const alertButtonStyle = {
  border: "1px solid #7c3aed",
  borderRadius: "6px",
  padding: "6px 10px",
  background: "#f5f3ff",
  color: "#6d28d9",
  cursor: "pointer",
  fontWeight: 600,
};

const editButtonStyle = {
  border: "1px solid #2563eb",
  borderRadius: "6px",
  padding: "6px 10px",
  background: "#ffffff",
  color: "#2563eb",
  cursor: "pointer",
  fontWeight: 600,
};

const deleteButtonStyle = {
  border: "1px solid #dc2626",
  borderRadius: "6px",
  padding: "6px 10px",
  background: "#ffffff",
  color: "#dc2626",
  cursor: "pointer",
  fontWeight: 600,
};

const statusBadgeStyle = {
  display: "inline-block",
  padding: "4px 9px",
  borderRadius: "999px",
  fontSize: "12px",
  fontWeight: 600,
};

const openBadgeStyle = {
  background: "#dcfce7",
  color: "#166534",
};

const draftBadgeStyle = {
  background: "#fef3c7",
  color: "#92400e",
};

const closedBadgeStyle = {
  background: "#fee2e2",
  color: "#991b1b",
};

const tableHeaderStyle = {
  textAlign: "left",
  padding: "12px",
  borderBottom: "1px solid #e2e8f0",
  color: "#475569",
  fontSize: "13px",
  fontWeight: 600,
};

const tableCellStyle = {
  padding: "14px 12px",
  borderBottom: "1px solid #f1f5f9",
  verticalAlign: "top",
  fontSize: "14px",
};

export default Recruitment;