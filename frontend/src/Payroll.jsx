import React, { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Clock3,
  Download,
  FileText,
  Mail,
  RefreshCw,
  Search,
  Send,
  Trash2,
  Users,
  WalletCards,
  X,
} from "lucide-react";
import { apiFetch, apiUrl } from "./api/apiClient";

const emptyForm = {
  employeeId: "",
  payrollMonth: "",
  basicSalary: "",
  allowances: "",
  bonus: "",
  overtimePay: "",
  deductions: "",
  providentFund: "",
  professionalTax: "",
  otherDeductions: "",
  status: "DRAFT",
  notes: "",
};

const palette = {
  text: "#0f172a",
  muted: "#64748b",
  border: "#e2e8f0",
  panel: "#ffffff",
  soft: "#f8fafc",
  primary: "#2563eb",
  primarySoft: "#eff6ff",
  success: "#15803d",
  successSoft: "#f0fdf4",
  warning: "#b45309",
  warningSoft: "#fffbeb",
  danger: "#b91c1c",
  dangerSoft: "#fef2f2",
};

const panel = {
  background: palette.panel,
  border: `1px solid ${palette.border}`,
  borderRadius: 18,
  boxShadow: "0 10px 28px rgba(15,23,42,.05)",
};

const inputStyle = {
  width: "100%",
  minHeight: 44,
  boxSizing: "border-box",
  padding: "10px 12px",
  border: `1px solid ${palette.border}`,
  borderRadius: 11,
  background: "#fff",
  color: palette.text,
  outline: "none",
  fontSize: 14,
};

const buttonBase = {
  border: 0,
  borderRadius: 11,
  minHeight: 42,
  padding: "9px 14px",
  fontWeight: 800,
  cursor: "pointer",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  whiteSpace: "nowrap",
};

function asNumber(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

function money(value) {
  return `₹${asNumber(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function monthLabel(value) {
  if (!value) return "-";
  const month = String(value).slice(0, 7);
  const date = new Date(`${month}-01T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", { month: "short", year: "numeric" });
}

function statusAppearance(status) {
  switch (String(status || "").toUpperCase()) {
    case "PAID":
      return { background: "#dcfce7", color: "#166534" };
    case "PROCESSED":
      return { background: "#dbeafe", color: "#1d4ed8" };
    default:
      return { background: "#f1f5f9", color: "#475569" };
  }
}

function Field({ label, hint, children }) {
  return (
    <label style={{ display: "grid", gap: 7 }}>
      <span style={{ fontSize: 12, fontWeight: 800, color: "#334155" }}>{label}</span>
      {children}
      {hint ? <span style={{ fontSize: 11, color: "#94a3b8" }}>{hint}</span> : null}
    </label>
  );
}

function StatCard({ label, value, helper, icon: Icon, tone = "default" }) {
  const tones = {
    default: { bg: "#fff", iconBg: "#f1f5f9", icon: "#475569" },
    primary: { bg: "#fff", iconBg: "#eff6ff", icon: "#2563eb" },
    success: { bg: "#fff", iconBg: "#ecfdf5", icon: "#15803d" },
    warning: { bg: "#fff", iconBg: "#fff7ed", icon: "#c2410c" },
  };
  const t = tones[tone] || tones.default;
  return (
    <div style={{ ...panel, padding: 16, background: t.bg }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
        <div>
          <div style={{ color: palette.muted, fontSize: 12, fontWeight: 700 }}>{label}</div>
          <div style={{ color: palette.text, fontSize: 23, fontWeight: 900, marginTop: 7 }}>{value}</div>
          {helper ? <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 5 }}>{helper}</div> : null}
        </div>
        <div
          style={{
            width: 38,
            height: 38,
            borderRadius: 12,
            background: t.iconBg,
            color: t.icon,
            display: "grid",
            placeItems: "center",
            flex: "0 0 auto",
          }}
        >
          <Icon size={18} />
        </div>
      </div>
    </div>
  );
}

function Modal({ open, title, subtitle, onClose, children, wide = false }) {
  if (!open) return null;
  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 3000,
        background: "rgba(15,23,42,.56)",
        backdropFilter: "none",
        display: "grid",
        placeItems: "center",
        padding: 20,
      }}
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose();
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: wide ? 1040 : 720,
          maxHeight: "90vh",
          overflow: "hidden",
          background: "#fff",
          borderRadius: 20,
          boxShadow: "0 30px 70px rgba(15,23,42,.24)",
          display: "grid",
          gridTemplateRows: "auto minmax(0,1fr)",
        }}
      >
        <div
          style={{
            padding: "18px 20px",
            borderBottom: `1px solid ${palette.border}`,
            display: "flex",
            alignItems: "flex-start",
            justifyContent: "space-between",
            gap: 16,
          }}
        >
          <div>
            <h3 style={{ margin: 0, fontSize: 20, color: palette.text }}>{title}</h3>
            {subtitle ? <p style={{ margin: "5px 0 0", fontSize: 12, color: palette.muted }}>{subtitle}</p> : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            style={{ ...buttonBase, width: 38, minHeight: 38, padding: 0, background: "#f1f5f9", color: "#475569" }}
          >
            <X size={18} />
          </button>
        </div>
        <div style={{ overflowY: "auto" }}>{children}</div>
      </div>
    </div>
  );
}

export default function Payroll() {
  const [rows, setRows] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [summary, setSummary] = useState({});
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [showBatch, setShowBatch] = useState(false);
  const [paymentModal, setPaymentModal] = useState(null);
  const [paymentReference, setPaymentReference] = useState("");
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10));
  const [paymentNotify, setPaymentNotify] = useState(true);
  const [month, setMonth] = useState("");
  const [status, setStatus] = useState("ALL");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [batchMonth, setBatchMonth] = useState(new Date().toISOString().slice(0, 7));
  const [batchProcess, setBatchProcess] = useState(true);
  const [batchNotify, setBatchNotify] = useState(false);

  const role = useMemo(() => {
    try {
      const raw = JSON.parse(localStorage.getItem("nexushr-user") || "{}").role || "";
      return String(raw).replace(/^ROLE_/, "").toUpperCase();
    } catch {
      return "";
    }
  }, []);

  const canManage = ["ADMIN", "HR", "MANAGER"].includes(role);
  const canMarkPaid = role === "ADMIN";
  const canDelete = role === "ADMIN";

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [payrolls, people] = await Promise.all([
        apiFetch("/payroll"),
        apiFetch("/employees"),
      ]);
      setRows(Array.isArray(payrolls) ? payrolls : []);
      setEmployees(Array.isArray(people) ? people : []);
      const summaryPath = month ? `/payroll/summary?month=${month}-01` : "/payroll/summary";
      setSummary(await apiFetch(summaryPath));
    } catch (e) {
      setError(e.message || "Unable to load payroll.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [month]);

  const filtered = useMemo(
    () =>
      rows.filter((p) => {
        const name = `${p.employee?.firstName || ""} ${p.employee?.lastName || ""}`.toLowerCase();
        const query = search.trim().toLowerCase();
        const matchesSearch =
          !query ||
          name.includes(query) ||
          String(p.employee?.id || "").includes(query) ||
          String(p.paymentReference || "").toLowerCase().includes(query);
        const matchesStatus = status === "ALL" || p.status === status;
        const matchesMonth = !month || String(p.payrollMonth || "").startsWith(month);
        return matchesSearch && matchesStatus && matchesMonth;
      }),
    [rows, search, status, month]
  );

  const computed = useMemo(() => {
    const gross =
      asNumber(form.basicSalary) +
      asNumber(form.allowances) +
      asNumber(form.bonus) +
      asNumber(form.overtimePay);
    const tax = gross * 0.1;
    const totalDeductions =
      asNumber(form.deductions) +
      asNumber(form.providentFund) +
      asNumber(form.professionalTax) +
      asNumber(form.otherDeductions) +
      tax;
    return { gross, tax, totalDeductions, net: Math.max(0, gross - totalDeductions) };
  }, [form]);

  const draftCount = rows.filter((row) => row.status === "DRAFT").length;
  const processedCount = rows.filter((row) => row.status === "PROCESSED").length;
  const paidCount = rows.filter((row) => row.status === "PAID").length;

  function setField(name, value) {
    setForm((current) => {
      const next = { ...current, [name]: value };
      if (name === "employeeId") {
        const employee = employees.find((e) => String(e.id) === String(value));
        if (employee?.salary != null) next.basicSalary = employee.salary;
      }
      return next;
    });
  }

  function openCreate() {
    setEditingId(null);
    setForm({ ...emptyForm, payrollMonth: new Date().toISOString().slice(0, 7) });
    setShowForm(true);
    setError("");
    setSuccess("");
  }

  function openEdit(payroll) {
    setEditingId(payroll.id);
    setForm({
      employeeId: String(payroll.employee?.id || ""),
      payrollMonth: String(payroll.payrollMonth || "").slice(0, 7),
      basicSalary: payroll.basicSalary ?? "",
      allowances: payroll.allowances ?? "",
      bonus: payroll.bonus ?? "",
      overtimePay: payroll.overtimePay ?? "",
      deductions: payroll.deductions ?? "",
      providentFund: payroll.providentFund ?? "",
      professionalTax: payroll.professionalTax ?? "",
      otherDeductions: payroll.otherDeductions ?? "",
      status: payroll.status || "DRAFT",
      notes: payroll.notes || "",
    });
    setShowForm(true);
    setError("");
    setSuccess("");
  }

  async function save(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      if (!form.employeeId || !form.payrollMonth) {
        throw new Error("Employee and payroll month are required.");
      }
      const payload = {
        employee: { id: Number(form.employeeId) },
        payrollMonth: `${form.payrollMonth}-01`,
        basicSalary: asNumber(form.basicSalary),
        allowances: asNumber(form.allowances),
        bonus: asNumber(form.bonus),
        overtimePay: asNumber(form.overtimePay),
        deductions: asNumber(form.deductions),
        providentFund: asNumber(form.providentFund),
        professionalTax: asNumber(form.professionalTax),
        otherDeductions: asNumber(form.otherDeductions),
        tax: 0,
        netSalary: computed.net,
        status: form.status,
        notes: form.notes,
      };
      await apiFetch(editingId ? `/payroll/${editingId}` : "/payroll", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      setSuccess(editingId ? "Payroll updated successfully." : "Payroll created successfully.");
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      await load();
    } catch (e) {
      setError(e.message || "Unable to save payroll.");
    } finally {
      setBusy(false);
    }
  }

  async function action(path, body, message) {
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      await apiFetch(path, { method: "POST", body: JSON.stringify(body || {}) });
      setSuccess(message);
      await load();
    } catch (e) {
      setError(e.message || "Payroll action failed.");
    } finally {
      setBusy(false);
    }
  }

  async function runBatch() {
    if (!batchMonth) {
      setError("Select a payroll month.");
      return;
    }
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const result = await apiFetch("/payroll/batch", {
        method: "POST",
        body: JSON.stringify({
          month: `${batchMonth}-01`,
          autoProcess: batchProcess,
          notifyEmployee: batchNotify,
        }),
      });
      setSuccess(
        `Batch complete: ${result.created || 0} created, ${result.skippedExisting || 0} existing skipped.`
      );
      setShowBatch(false);
      await load();
    } catch (e) {
      setError(e.message || "Batch payroll failed.");
    } finally {
      setBusy(false);
    }
  }

  async function remove(id) {
    if (!window.confirm("Delete this payroll record permanently?")) return;
    try {
      await apiFetch(`/payroll/${id}`, { method: "DELETE" });
      setSuccess("Payroll deleted.");
      await load();
    } catch (e) {
      setError(e.message || "Delete failed.");
    }
  }

  function openPayment(payroll) {
    setPaymentModal(payroll);
    setPaymentReference(`PAY-${payroll.employee?.id || payroll.id}-${Date.now()}`);
    setPaymentDate(new Date().toISOString().slice(0, 10));
    setPaymentNotify(true);
  }

  async function markPaid() {
    if (!paymentModal) return;
    if (!paymentReference.trim()) {
      setError("Payment reference is required.");
      return;
    }
    await action(
      `/payroll/${paymentModal.id}/paid`,
      {
        paymentReference: paymentReference.trim(),
        paymentDate,
        notifyEmployee: paymentNotify,
      },
      "Payroll marked paid and employee notification processed."
    );
    setPaymentModal(null);
  }

  async function downloadPdf(payroll) {
    try {
      const saved = JSON.parse(localStorage.getItem("nexushr-user") || "{}");
      const response = await fetch(apiUrl(`/payroll/${payroll.id}/payslip`), {
        headers: {
          Authorization: `Bearer ${saved.token}`,
          Accept: "application/pdf",
        },
      });
      if (!response.ok) throw new Error((await response.text()) || "Payslip download failed.");
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `NexusHR-Payslip-${payroll.employee?.firstName || "employee"}-${payroll.payrollMonth}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.message || "Payslip download failed.");
    }
  }

  function clearFilters() {
    setSearch("");
    setMonth("");
    setStatus("ALL");
  }

  return (
    <div style={{ display: "grid", gap: 18, width: "100%", maxWidth: 1240, margin: "0 auto", paddingBottom: 32 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 16,
          alignItems: "flex-start",
          flexWrap: "wrap",
        }}
      >
        <div>
          <div style={{ color: palette.primary, fontSize: 12, fontWeight: 900, letterSpacing: ".08em", textTransform: "uppercase" }}>
            Finance workspace
          </div>
          <h2 style={{ margin: "5px 0 0", color: palette.text, fontSize: 28 }}>Payroll Management</h2>
          <p style={{ margin: "7px 0 0", color: palette.muted, fontSize: 14 }}>
            Create payroll, review calculations, process payments and deliver employee payslips.
          </p>
        </div>
        <div style={{ display: "flex", gap: 9, flexWrap: "wrap" }}>
          <button type="button" onClick={load} style={{ ...buttonBase, background: "#fff", color: "#334155", border: `1px solid ${palette.border}` }}>
            <RefreshCw size={16} /> Refresh
          </button>
          {canManage ? (
            <button type="button" onClick={() => setShowBatch(true)} style={{ ...buttonBase, background: "#fff", color: "#334155", border: `1px solid ${palette.border}` }}>
              <Users size={16} /> Monthly Batch
            </button>
          ) : null}
          {canManage ? (
            <button type="button" onClick={openCreate} style={{ ...buttonBase, background: palette.primary, color: "#fff", boxShadow: "0 10px 20px rgba(37,99,235,.18)" }}>
              <span style={{ fontSize: 18, lineHeight: 1 }}>+</span> New Payroll
            </button>
          ) : null}
        </div>
      </div>

      {(error || success) && (
        <div
          style={{
            ...panel,
            padding: "13px 16px",
            background: error ? palette.dangerSoft : palette.successSoft,
            borderColor: error ? "#fecaca" : "#bbf7d0",
            color: error ? palette.danger : palette.success,
            fontWeight: 700,
            fontSize: 13,
          }}
        >
          {error || success}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(185px,1fr))", gap: 12 }}>
        <StatCard label="Payroll records" value={summary.records || 0} helper="Current result set" icon={FileText} />
        <StatCard label="Gross payroll" value={money(summary.grossPayroll)} helper="Before deductions" icon={WalletCards} tone="primary" />
        <StatCard label="Net payroll" value={money(summary.netPayroll)} helper="Employee take-home" icon={WalletCards} tone="success" />
        <StatCard label="Total deductions" value={money(summary.totalDeductions)} helper={`Tax ${money(summary.tax)}`} icon={Download} tone="warning" />
        <StatCard label="Awaiting payment" value={processedCount} helper="Processed, not paid" icon={Clock3} tone="warning" />
        <StatCard label="Paid" value={paidCount} helper={`${draftCount} draft record${draftCount === 1 ? "" : "s"}`} icon={CheckCircle2} tone="success" />
      </div>

      <div style={{ ...panel, padding: 16 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <div style={{ fontSize: 14, fontWeight: 900, color: palette.text }}>Payroll cycle</div>
            <div style={{ fontSize: 12, color: palette.muted, marginTop: 3 }}>
              A simple workflow from preparation to payment and payslip delivery.
            </div>
          </div>
          <span style={{ fontSize: 12, fontWeight: 800, color: canManage ? palette.success : palette.muted }}>
            {canManage ? "Management access" : "Read-only access"}
          </span>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,minmax(0,1fr))", gap: 8, marginTop: 14 }}>
          {[
            ["1", "Draft", "Review salary inputs", draftCount, "#f8fafc", "#475569"],
            ["2", "Processed", "Calculation approved", processedCount, "#eff6ff", "#1d4ed8"],
            ["3", "Paid", "Payment completed", paidCount, "#f0fdf4", "#15803d"],
            ["4", "Payslip", "PDF / email delivery", rows.length, "#faf5ff", "#7e22ce"],
          ].map(([step, title, text, count, bg, color]) => (
            <div key={title} style={{ border: `1px solid ${palette.border}`, borderRadius: 14, padding: 13, background: bg }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                <span style={{ width: 25, height: 25, borderRadius: 8, background: "#fff", display: "grid", placeItems: "center", fontSize: 11, fontWeight: 900, color }}>{step}</span>
                <strong style={{ color, fontSize: 16 }}>{count}</strong>
              </div>
              <div style={{ marginTop: 9, fontWeight: 900, color: palette.text, fontSize: 13 }}>{title}</div>
              <div style={{ color: palette.muted, fontSize: 11, marginTop: 3 }}>{text}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ ...panel, overflow: "hidden" }}>
        <div style={{ padding: "16px 16px 14px", borderBottom: `1px solid ${palette.border}` }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
            <div>
              <h3 style={{ margin: 0, color: palette.text, fontSize: 17 }}>Payroll records</h3>
              <p style={{ margin: "4px 0 0", color: palette.muted, fontSize: 12 }}>
                {filtered.length} of {rows.length} records shown
              </p>
            </div>
            {(search || month || status !== "ALL") && (
              <button type="button" onClick={clearFilters} style={{ ...buttonBase, minHeight: 36, padding: "7px 10px", background: "#f8fafc", color: "#475569" }}>
                Clear filters
              </button>
            )}
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "minmax(240px,1.6fr) minmax(170px,.8fr) minmax(170px,.8fr)", gap: 10, marginTop: 13 }}>
            <div style={{ position: "relative" }}>
              <Search size={16} style={{ position: "absolute", left: 12, top: 14, color: "#94a3b8" }} />
              <input
                style={{ ...inputStyle, paddingLeft: 36 }}
                placeholder="Search employee, ID or payment reference"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <input type="month" style={inputStyle} value={month} onChange={(e) => setMonth(e.target.value)} />
            <select style={inputStyle} value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">All statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="PROCESSED">Processed</option>
              <option value="PAID">Paid</option>
            </select>
          </div>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13, minWidth: 980 }}>
            <thead>
              <tr style={{ textAlign: "left", background: "#f8fafc" }}>
                {["Employee", "Payroll", "Earnings", "Deductions", "Take home", "Status", "Payment", "Actions"].map((heading) => (
                  <th key={heading} style={{ padding: "12px 14px", color: "#475569", fontSize: 11, textTransform: "uppercase", letterSpacing: ".04em", borderBottom: `1px solid ${palette.border}` }}>
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8" style={{ padding: 28, color: palette.muted }}>Loading payroll records...</td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="8" style={{ padding: 36, textAlign: "center" }}>
                    <div style={{ width: 48, height: 48, borderRadius: 16, background: "#f1f5f9", display: "grid", placeItems: "center", margin: "0 auto" }}>
                      <WalletCards size={22} color="#64748b" />
                    </div>
                    <div style={{ marginTop: 10, fontWeight: 900, color: palette.text }}>No payroll records found</div>
                    <div style={{ marginTop: 5, color: palette.muted, fontSize: 12 }}>Adjust the filters or create a new payroll record.</div>
                  </td>
                </tr>
              ) : (
                filtered.map((payroll) => {
                  const gross = payroll.grossSalary ??
                    (asNumber(payroll.basicSalary) + asNumber(payroll.allowances) + asNumber(payroll.bonus) + asNumber(payroll.overtimePay));
                  const totalDeductions =
                    asNumber(payroll.deductions) +
                    asNumber(payroll.providentFund) +
                    asNumber(payroll.professionalTax) +
                    asNumber(payroll.otherDeductions) +
                    asNumber(payroll.tax);
                  const statusStyle = statusAppearance(payroll.status);
                  return (
                    <tr key={payroll.id} style={{ borderBottom: "1px solid #f1f5f9" }}>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                          <div style={{ width: 36, height: 36, borderRadius: 11, background: "#eff6ff", color: palette.primary, display: "grid", placeItems: "center", fontWeight: 900, fontSize: 12 }}>
                            {(payroll.employee?.firstName || "E").slice(0, 1)}{(payroll.employee?.lastName || "").slice(0, 1)}
                          </div>
                          <div>
                            <div style={{ fontWeight: 900, color: palette.text }}>{payroll.employee?.firstName} {payroll.employee?.lastName}</div>
                            <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 2 }}>Employee #{payroll.employee?.id}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ fontWeight: 800, color: palette.text }}>{monthLabel(payroll.payrollMonth)}</div>
                        <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 3 }}>Record #{payroll.id}</div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ fontWeight: 900, color: palette.text }}>{money(gross)}</div>
                        <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 3 }}>Gross earnings</div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ fontWeight: 800, color: "#475569" }}>{money(totalDeductions)}</div>
                        <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 3 }}>incl. tax {money(payroll.tax)}</div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ fontWeight: 900, color: "#0f766e", fontSize: 14 }}>{money(payroll.netSalary)}</div>
                        <div style={{ color: "#94a3b8", fontSize: 11, marginTop: 3 }}>Net salary</div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <span style={{ ...statusStyle, display: "inline-flex", padding: "6px 9px", borderRadius: 999, fontWeight: 900, fontSize: 10 }}>{payroll.status}</span>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ fontWeight: 700, color: palette.text }}>{payroll.paymentDate || "Not paid"}</div>
                        <div style={{ color: "#94a3b8", fontSize: 10, marginTop: 3, maxWidth: 130, wordBreak: "break-all" }}>{payroll.paymentReference || "No reference"}</div>
                      </td>
                      <td style={{ padding: 14, verticalAlign: "top" }}>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap", maxWidth: 220 }}>
                          <button title="Download payslip" type="button" style={{ ...buttonBase, minHeight: 34, padding: "6px 9px", background: "#f1f5f9", color: "#334155" }} onClick={() => downloadPdf(payroll)}>
                            <FileText size={14} /> PDF
                          </button>
                          {canManage && payroll.status !== "PAID" ? (
                            <button title="Edit payroll" type="button" style={{ ...buttonBase, minHeight: 34, padding: "6px 9px", background: "#fff", color: "#334155", border: `1px solid ${palette.border}` }} onClick={() => openEdit(payroll)}>
                              <FileText size={14} /> Edit
                            </button>
                          ) : null}
                          {canManage && payroll.status === "DRAFT" ? (
                            <button type="button" disabled={busy} style={{ ...buttonBase, minHeight: 34, padding: "6px 9px", background: palette.primarySoft, color: "#1d4ed8" }} onClick={() => action(`/payroll/${payroll.id}/process`, { notifyEmployee: true }, "Payroll processed and employee notified.")}>
                              <CheckCircle2 size={14} /> Process
                            </button>
                          ) : null}
                          {canMarkPaid && payroll.status === "PROCESSED" ? (
                            <button type="button" disabled={busy} style={{ ...buttonBase, minHeight: 34, padding: "6px 9px", background: "#ecfdf5", color: "#15803d" }} onClick={() => openPayment(payroll)}>
                              <CheckCircle2 size={14} /> Mark paid
                            </button>
                          ) : null}
                          {canManage ? (
                            <button type="button" disabled={busy} style={{ ...buttonBase, minHeight: 34, padding: "6px 9px", background: "#fff7ed", color: "#c2410c" }} onClick={() => action(`/payroll/${payroll.id}/email-payslip`, { subject: `NexusHR Payslip - ${monthLabel(payroll.payrollMonth)}` }, "Payslip email request completed.")}>
                              <Mail size={14} /> Email
                            </button>
                          ) : null}
                          {canDelete && payroll.status !== "PAID" ? (
                            <button title="Delete payroll" type="button" style={{ ...buttonBase, minHeight: 34, width: 34, padding: 0, background: palette.dangerSoft, color: palette.danger }} onClick={() => remove(payroll.id)}>
                              <Trash2 size={14} />
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={showForm}
        wide
        title={editingId ? "Edit payroll record" : "Create payroll record"}
        subtitle="Enter earnings and deductions. NexusHR calculates the estimated tax and take-home salary before you save."
        onClose={() => !busy && setShowForm(false)}
      >
        <form onSubmit={save}>
          <div style={{ display: "grid", gridTemplateColumns: "minmax(0,1.65fr) minmax(280px,.8fr)", gap: 0 }}>
            <div style={{ padding: 20, display: "grid", gap: 18 }}>
              <section>
                <div style={{ fontWeight: 900, color: palette.text, marginBottom: 11 }}>Employee & period</div>
                <div style={{ display: "grid", gridTemplateColumns: "1.4fr .8fr", gap: 12 }}>
                  <Field label="Employee" hint="Basic salary is filled automatically when available.">
                    <select style={inputStyle} value={form.employeeId} onChange={(e) => setField("employeeId", e.target.value)}>
                      <option value="">Select employee</option>
                      {employees.map((employee) => (
                        <option key={employee.id} value={employee.id}>
                          {employee.firstName} {employee.lastName} — {employee.department || "No department"}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Payroll month">
                    <input type="month" style={inputStyle} value={form.payrollMonth} onChange={(e) => setField("payrollMonth", e.target.value)} />
                  </Field>
                </div>
              </section>

              <section>
                <div style={{ fontWeight: 900, color: palette.text, marginBottom: 11 }}>Earnings</div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(2,minmax(0,1fr))", gap: 12 }}>
                  <Field label="Basic salary"><input type="number" min="0" step="0.01" style={inputStyle} value={form.basicSalary} onChange={(e) => setField("basicSalary", e.target.value)} /></Field>
                  <Field label="Allowances"><input type="number" min="0" step="0.01" style={inputStyle} value={form.allowances} onChange={(e) => setField("allowances", e.target.value)} /></Field>
                  <Field label="Bonus"><input type="number" min="0" step="0.01" style={inputStyle} value={form.bonus} onChange={(e) => setField("bonus", e.target.value)} /></Field>
                  <Field label="Overtime pay"><input type="number" min="0" step="0.01" style={inputStyle} value={form.overtimePay} onChange={(e) => setField("overtimePay", e.target.value)} /></Field>
                </div>
              </section>

              <section>
                <div style={{ fontWeight: 900, color: palette.text, marginBottom: 11 }}>Deductions</div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(2,minmax(0,1fr))", gap: 12 }}>
                  <Field label="Other deductions"><input type="number" min="0" step="0.01" style={inputStyle} value={form.deductions} onChange={(e) => setField("deductions", e.target.value)} /></Field>
                  <Field label="Provident fund"><input type="number" min="0" step="0.01" style={inputStyle} value={form.providentFund} onChange={(e) => setField("providentFund", e.target.value)} /></Field>
                  <Field label="Professional tax"><input type="number" min="0" step="0.01" style={inputStyle} value={form.professionalTax} onChange={(e) => setField("professionalTax", e.target.value)} /></Field>
                  <Field label="Additional deductions"><input type="number" min="0" step="0.01" style={inputStyle} value={form.otherDeductions} onChange={(e) => setField("otherDeductions", e.target.value)} /></Field>
                </div>
              </section>

              <section>
                <div style={{ display: "grid", gridTemplateColumns: ".7fr 1.3fr", gap: 12 }}>
                  <Field label="Status">
                    <select style={inputStyle} value={form.status} onChange={(e) => setField("status", e.target.value)}>
                      <option value="DRAFT">Draft</option>
                      <option value="PROCESSED">Processed</option>
                    </select>
                  </Field>
                  <Field label="Notes"><textarea style={{ ...inputStyle, minHeight: 88, resize: "vertical" }} value={form.notes} onChange={(e) => setField("notes", e.target.value)} placeholder="Optional payroll notes" /></Field>
                </div>
              </section>
            </div>

            <aside style={{ background: "#f8fafc", borderLeft: `1px solid ${palette.border}`, padding: 20, display: "grid", alignContent: "start", gap: 12 }}>
              <div>
                <div style={{ fontSize: 12, color: palette.muted, fontWeight: 800 }}>Calculation preview</div>
                <div style={{ fontSize: 11, color: "#94a3b8", marginTop: 3 }}>Updates while you type</div>
              </div>
              {[
                ["Gross earnings", computed.gross, "#0f172a"],
                ["Estimated tax (10%)", computed.tax, "#b45309"],
                ["Total deductions", computed.totalDeductions, "#b45309"],
                ["Estimated take-home", computed.net, "#15803d"],
              ].map(([label, value, color]) => (
                <div key={label} style={{ background: "#fff", border: `1px solid ${palette.border}`, borderRadius: 13, padding: 13 }}>
                  <div style={{ fontSize: 11, color: palette.muted }}>{label}</div>
                  <div style={{ fontSize: 18, fontWeight: 900, color, marginTop: 4 }}>{money(value)}</div>
                </div>
              ))}
              <div style={{ padding: 12, borderRadius: 13, background: "#eff6ff", color: "#1e40af", fontSize: 11, lineHeight: 1.55 }}>
                Tax shown here is the current NexusHR project estimate. Review statutory payroll values before using the record for real filing.
              </div>
            </aside>
          </div>
          <div style={{ padding: "14px 20px", borderTop: `1px solid ${palette.border}`, display: "flex", justifyContent: "flex-end", gap: 9, background: "#fff" }}>
            <button type="button" disabled={busy} style={{ ...buttonBase, background: "#f1f5f9", color: "#334155" }} onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" disabled={busy} style={{ ...buttonBase, background: palette.primary, color: "#fff", minWidth: 140 }}>
              <Send size={15} /> {busy ? "Saving..." : editingId ? "Update Payroll" : "Create Payroll"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={showBatch}
        title="Run monthly batch payroll"
        subtitle="Generate payroll records for eligible employees for a selected month."
        onClose={() => !busy && setShowBatch(false)}
      >
        <div style={{ padding: 20, display: "grid", gap: 16 }}>
          <Field label="Payroll month"><input type="month" style={inputStyle} value={batchMonth} onChange={(e) => setBatchMonth(e.target.value)} /></Field>
          <label style={{ display: "flex", gap: 10, alignItems: "flex-start", padding: 13, border: `1px solid ${palette.border}`, borderRadius: 13 }}>
            <input type="checkbox" checked={batchProcess} onChange={(e) => setBatchProcess(e.target.checked)} style={{ marginTop: 3 }} />
            <span><strong style={{ display: "block", color: palette.text, fontSize: 13 }}>Auto process created payroll</strong><span style={{ color: palette.muted, fontSize: 11 }}>Move generated records directly to PROCESSED after calculation.</span></span>
          </label>
          <label style={{ display: "flex", gap: 10, alignItems: "flex-start", padding: 13, border: `1px solid ${palette.border}`, borderRadius: 13 }}>
            <input type="checkbox" checked={batchNotify} onChange={(e) => setBatchNotify(e.target.checked)} style={{ marginTop: 3 }} />
            <span><strong style={{ display: "block", color: palette.text, fontSize: 13 }}>Notify employees</strong><span style={{ color: palette.muted, fontSize: 11 }}>Create in-app/email communication when supported by your configuration.</span></span>
          </label>
          <div style={{ padding: 13, borderRadius: 13, background: palette.warningSoft, color: "#92400e", fontSize: 11, lineHeight: 1.55 }}>
            Existing payroll records for the same employee/month are skipped to prevent duplicates.
          </div>
        </div>
        <div style={{ padding: "14px 20px", borderTop: `1px solid ${palette.border}`, display: "flex", justifyContent: "flex-end", gap: 9 }}>
          <button type="button" style={{ ...buttonBase, background: "#f1f5f9", color: "#334155" }} onClick={() => setShowBatch(false)}>Cancel</button>
          <button type="button" disabled={busy} style={{ ...buttonBase, background: "#0f172a", color: "#fff" }} onClick={runBatch}><CheckCircle2 size={16} /> {busy ? "Running..." : "Run Batch Payroll"}</button>
        </div>
      </Modal>

      <Modal
        open={Boolean(paymentModal)}
        title="Mark payroll as paid"
        subtitle={paymentModal ? `${paymentModal.employee?.firstName || "Employee"} ${paymentModal.employee?.lastName || ""} · ${monthLabel(paymentModal.payrollMonth)} · ${money(paymentModal.netSalary)}` : ""}
        onClose={() => !busy && setPaymentModal(null)}
      >
        <div style={{ padding: 20, display: "grid", gap: 14 }}>
          <Field label="Payment date"><input type="date" style={inputStyle} value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} /></Field>
          <Field label="Payment reference" hint="Use your bank/transaction reference if available."><input style={inputStyle} value={paymentReference} onChange={(e) => setPaymentReference(e.target.value)} placeholder="e.g. PAY-EMP1-202610" /></Field>
          <label style={{ display: "flex", gap: 9, alignItems: "center", fontSize: 12, color: "#334155" }}>
            <input type="checkbox" checked={paymentNotify} onChange={(e) => setPaymentNotify(e.target.checked)} /> Notify employee about payment
          </label>
        </div>
        <div style={{ padding: "14px 20px", borderTop: `1px solid ${palette.border}`, display: "flex", justifyContent: "flex-end", gap: 9 }}>
          <button type="button" style={{ ...buttonBase, background: "#f1f5f9", color: "#334155" }} onClick={() => setPaymentModal(null)}>Cancel</button>
          <button type="button" disabled={busy} style={{ ...buttonBase, background: "#15803d", color: "#fff" }} onClick={markPaid}><CheckCircle2 size={16} /> {busy ? "Saving..." : "Confirm Payment"}</button>
        </div>
      </Modal>
    </div>
  );
}
