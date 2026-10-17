import React, { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle, CheckCircle2, Database,
  BrainCircuit,
  Building2,
  Download,
  Flame,
  Gauge,
  RefreshCw,
  Search,
  ShieldAlert,
  Sparkles,
  Target,
  TrendingUp,
  Users,
} from "lucide-react";
import { apiFetch } from "./api/apiClient";

const style = {
  card: {
    background: "#fff",
    border: "1px solid #e2e8f0",
    borderRadius: 16,
    boxShadow: "0 8px 24px rgba(15,23,42,.05)",
  },
  button: {
    border: 0,
    borderRadius: 10,
    padding: "10px 14px",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    fontWeight: 700,
    cursor: "pointer",
  },
  input: {
    width: "100%",
    boxSizing: "border-box",
    padding: "10px 12px",
    border: "1px solid #dbe3ef",
    borderRadius: 10,
    fontSize: 13,
    background: "#fff",
    color: "#0f172a",
  },
};

function safeNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function levelColors(level, positive = false) {
  const value = String(level || "").toUpperCase();
  if (positive) {
    if (value === "HIGH" || value === "HEALTHY") return { bg: "#dcfce7", fg: "#166534" };
    if (value === "MEDIUM" || value === "WATCH") return { bg: "#fef3c7", fg: "#92400e" };
    return { bg: "#fee2e2", fg: "#991b1b" };
  }
  if (value === "HIGH" || value === "URGENT" || value === "NEEDS_ATTENTION") return { bg: "#fee2e2", fg: "#991b1b" };
  if (value === "MEDIUM" || value === "WATCH") return { bg: "#fef3c7", fg: "#92400e" };
  return { bg: "#dcfce7", fg: "#166534" };
}

function Badge({ children, level, positive = false }) {
  const colors = levelColors(level, positive);
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        background: colors.bg,
        color: colors.fg,
        padding: "4px 9px",
        borderRadius: 999,
        fontSize: 10,
        fontWeight: 800,
        whiteSpace: "nowrap",
      }}
    >
      {children}
    </span>
  );
}

function Meter({ value, label, tone = "default" }) {
  const safe = Math.max(0, Math.min(100, safeNumber(value)));
  let fill = "#2563eb";
  if (tone === "risk") fill = safe >= 65 ? "#dc2626" : safe >= 35 ? "#f59e0b" : "#16a34a";
  if (tone === "positive") fill = safe >= 75 ? "#16a34a" : safe >= 50 ? "#2563eb" : "#f59e0b";
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, fontSize: 12, color: "#64748b", marginBottom: 6 }}>
        <span>{label}</span>
        <strong style={{ color: "#0f172a" }}>{Math.round(safe)}%</strong>
      </div>
      <div style={{ height: 8, background: "#e2e8f0", borderRadius: 999, overflow: "hidden" }}>
        <div style={{ height: "100%", width: `${safe}%`, background: fill, transition: "width .25s ease" }} />
      </div>
    </div>
  );
}

function StatCard({ label, value, icon: Icon, helper }) {
  return (
    <div style={{ ...style.card, padding: 16 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 10, color: "#64748b", fontSize: 12 }}>
        <span>{label}</span>
        <Icon size={18} />
      </div>
      <div style={{ fontSize: 25, lineHeight: 1.1, fontWeight: 800, color: "#0f172a", marginTop: 9 }}>{value}</div>
      {helper && <div style={{ marginTop: 6, fontSize: 10, color: "#94a3b8" }}>{helper}</div>}
    </div>
  );
}

function InsightList({ title, items, icon: Icon, emptyText = "No items available." }) {
  const rows = Array.isArray(items) ? items : [];
  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: 7, fontSize: 12, fontWeight: 800, color: "#334155", marginBottom: 8 }}>
        <Icon size={14} /> {title}
      </div>
      <div style={{ display: "grid", gap: 6 }}>
        {rows.length === 0 ? (
          <div style={{ fontSize: 12, color: "#94a3b8" }}>{emptyText}</div>
        ) : (
          rows.map((item, index) => (
            <div key={`${item}-${index}`} style={{ fontSize: 12, color: "#64748b", paddingLeft: 12, position: "relative", lineHeight: 1.5 }}>
              <span style={{ position: "absolute", left: 0 }}>•</span>
              {item}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function Distribution({ title, values = {}, positive = false }) {
  const entries = Object.entries(values || {});
  const total = Math.max(1, entries.reduce((sum, [, count]) => sum + safeNumber(count), 0));
  return (
    <div style={{ ...style.card, padding: 18 }}>
      <div style={{ fontWeight: 800, color: "#0f172a", marginBottom: 13 }}>{title}</div>
      <div style={{ display: "grid", gap: 10 }}>
        {entries.map(([name, count]) => {
          const pct = Math.round((safeNumber(count) * 100) / total);
          const colors = levelColors(name, positive);
          return (
            <div key={name}>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: "#475569" }}>
                <span>{name}</span>
                <span>{count} · {pct}%</span>
              </div>
              <div style={{ height: 7, background: "#e2e8f0", borderRadius: 999, overflow: "hidden", marginTop: 5 }}>
                <div style={{ height: "100%", width: `${pct}%`, background: colors.fg }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function WorkforceIntelligence() {
  const [records, setRecords] = useState([]);
  const [summary, setSummary] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [search, setSearch] = useState("");
  const [department, setDepartment] = useState("ALL");
  const [risk, setRisk] = useState("ALL");
  const [priority, setPriority] = useState("ALL");
  const [sort, setSort] = useState("RISK_DESC");
  const [activeView, setActiveView] = useState("EMPLOYEES");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function load() {
    try {
      setLoading(true);
      setError("");
      const [rows, stats, departmentRows] = await Promise.all([
        apiFetch("/workforce-intelligence/employees"),
        apiFetch("/workforce-intelligence/summary"),
        apiFetch("/workforce-intelligence/departments"),
      ]);
      const list = Array.isArray(rows) ? rows : [];
      setRecords(list);
      setSummary(stats || null);
      setDepartments(Array.isArray(departmentRows) ? departmentRows : []);
      setSelectedId((current) => current || (list.length ? String(list[0].employeeId) : ""));
    } catch (err) {
      setError(err?.message || "Unable to load workforce intelligence.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const departmentOptions = useMemo(
    () => [...new Set(records.map((row) => row.department).filter(Boolean))].sort(),
    [records]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const rows = records.filter((row) => {
      const haystack = `${row.employeeName || ""} ${row.department || ""} ${row.jobTitle || ""}`.toLowerCase();
      const matchesSearch = !q || haystack.includes(q);
      const matchesDepartment = department === "ALL" || row.department === department;
      const matchesRisk = risk === "ALL" || String(row.attritionRiskLevel || "").toUpperCase() === risk;
      const matchesPriority = priority === "ALL" || String(row.priority || "").toUpperCase() === priority;
      return matchesSearch && matchesDepartment && matchesRisk && matchesPriority;
    });

    return [...rows].sort((a, b) => {
      if (sort === "RISK_ASC") return safeNumber(a.attritionRiskScore) - safeNumber(b.attritionRiskScore);
      if (sort === "ENGAGEMENT_DESC") return safeNumber(b.engagementScore) - safeNumber(a.engagementScore);
      if (sort === "POTENTIAL_DESC") return safeNumber(b.growthPotentialScore) - safeNumber(a.growthPotentialScore);
      if (sort === "BURNOUT_DESC") return safeNumber(b.burnoutRiskScore) - safeNumber(a.burnoutRiskScore);
      if (sort === "NAME") return String(a.employeeName || "").localeCompare(String(b.employeeName || ""));
      return safeNumber(b.attritionRiskScore) - safeNumber(a.attritionRiskScore);
    });
  }, [records, search, department, risk, priority, sort]);

  const selected = useMemo(
    () => records.find((row) => String(row.employeeId) === String(selectedId)) || null,
    [records, selectedId]
  );

  async function refreshEmployee() {
    if (!selectedId) return;
    try {
      setError("");
      const updated = await apiFetch(`/workforce-intelligence/employee/${selectedId}`);
      setRecords((current) => current.map((row) => (String(row.employeeId) === String(selectedId) ? updated : row)));
    } catch (err) {
      setError(err?.message || "Unable to refresh employee insight.");
    }
  }

  function exportCsv() {
    const headers = [
      "Employee ID",
      "Employee",
      "Department",
      "Job Title",
      "Attrition Risk",
      "Risk Level",
      "Engagement",
      "Burnout Risk",
      "Growth Potential",
      "Skill Gap",
      "Attendance Rate",
      "Performance Rating",
      "Priority",
      "Confidence",
    ];
    const rows = filtered.map((row) => [
      row.employeeId,
      row.employeeName,
      row.department || "",
      row.jobTitle || "",
      row.attritionRiskScore,
      row.attritionRiskLevel,
      row.engagementScore,
      row.burnoutRiskScore,
      row.growthPotentialScore,
      row.skillGapScore,
      row.attendanceRate,
      row.averagePerformanceRating,
      row.priority,
      row.confidenceScore,
    ]);
    const escape = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;
    const csv = [headers, ...rows].map((row) => row.map(escape).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `nexushr-workforce-insights-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  const cards = [
    { label: "Workforce health", value: `${summary?.workforceHealth ?? 0}%`, icon: Gauge, helper: "Combined engagement, retention and burnout health" },
    { label: "Active workforce", value: summary?.totalEmployees ?? records.length, icon: Users, helper: "Employees included in analysis" },
    { label: "Avg engagement", value: `${summary?.averageEngagement ?? 0}%`, icon: Activity, helper: "Higher is better" },
    { label: "Avg attendance", value: `${safeNumber(summary?.averageAttendance).toFixed(1)}%`, icon: Target, helper: "Attendance signal across active employees" },
    { label: "High attrition risk", value: summary?.highRisk ?? 0, icon: ShieldAlert, helper: "Employees needing retention review" },
    { label: "High burnout risk", value: summary?.highBurnoutRisk ?? 0, icon: Flame, helper: "Workload and recovery warning" },
    { label: "High potential", value: summary?.highPotential ?? 0, icon: TrendingUp, helper: "Employees with strong growth potential" },
    { label: "Avg skill gap", value: `${summary?.averageSkillGap ?? 0}%`, icon: BrainCircuit, helper: "Lower means fewer identified gaps" },
    { label: "Avg performance", value: `${safeNumber(summary?.averagePerformanceRating).toFixed(1)}/5`, icon: Sparkles, helper: "Based on available performance reviews" },
    { label: "Urgent actions", value: summary?.urgentActions ?? 0, icon: AlertTriangle, helper: "Requires human review first" },
    { label: "Decision readiness", value: `${summary?.averageConfidence ?? 0}%`, icon: CheckCircle2, helper: "Average confidence in available workforce signals" },
    { label: "Data gaps", value: Math.max(0, Number(summary?.totalEmployees ?? records.length) - Number(summary?.employeesWithPerformanceData ?? 0)), icon: Database, helper: "Employees without performance history" },
  ];

  return (
    <div style={{ display: "grid", gap: 20 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 14, flexWrap: "wrap" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 26, color: "#0f172a", display: "flex", alignItems: "center", gap: 10 }}>
            <BrainCircuit size={26} color="#2563eb" /> AI Workforce Insights
          </h2>
          <div style={{ color: "#64748b", marginTop: 7, fontSize: 13, maxWidth: 850, lineHeight: 1.65 }}>
            Explainable predictive workforce intelligence for attrition risk, engagement, burnout, skill gaps and growth potential. The engine uses existing HR data locally and provides decision-support recommendations for HR and managers.
          </div>
          <div style={{ marginTop: 7, fontSize: 10, color: "#94a3b8" }}>
            {summary?.modelVersion || "NexusHR Explainable Workforce Engine"} · Human review required before employment decisions.
          </div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button onClick={exportCsv} style={{ ...style.button, background: "#f1f5f9", color: "#334155" }}>
            <Download size={16} /> Export CSV
          </button>
          <button onClick={load} style={{ ...style.button, background: "#eff6ff", color: "#1d4ed8" }}>
            <RefreshCw size={16} /> Refresh intelligence
          </button>
        </div>
      </div>

      {error && <div style={{ ...style.card, padding: 14, background: "#fef2f2", borderColor: "#fecaca", color: "#991b1b" }}>{error}</div>}

      <div style={{ ...style.card, padding: 18, display: "grid", gridTemplateColumns: "minmax(0,1.5fr) minmax(260px,1fr)", gap: 18 }}>
        <div>
          <div style={{ fontWeight: 800, color: "#0f172a", marginBottom: 8 }}>What this page tells you</div>
          <div style={{ color: "#475569", fontSize: 13, lineHeight: 1.7 }}>
            This page turns attendance, leave, performance and employee-profile data into understandable workforce signals.
            <strong> Attrition risk</strong> estimates retention concern, <strong>engagement</strong> shows participation/consistency,
            <strong> burnout risk</strong> highlights workload or recovery warning, <strong>skill gap</strong> shows development need, and
            <strong>growth potential</strong> highlights employees who may benefit from stretch assignments or development plans.
          </div>
        </div>
        <div style={{ display: "grid", gap: 8 }}>
          {[
            ["Green / Low", "Healthy signal — continue normal monitoring."],
            ["Amber / Watch", "Review the employee context before taking action."],
            ["Red / High", "Prioritize a human conversation and verify the data."],
            ["Confidence", `${summary?.averageConfidence ?? 0}% average confidence — stronger when attendance and performance history exist.`],
          ].map(([k,v]) => <div key={k} style={{ padding: 10, border: "1px solid #e2e8f0", borderRadius: 10 }}><div style={{ fontSize: 11, fontWeight: 800, color: "#334155" }}>{k}</div><div style={{ fontSize: 11, color: "#64748b", marginTop: 3 }}>{v}</div></div>)}
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(165px,1fr))", gap: 14 }}>
        {cards.map((card) => <StatCard key={card.label} {...card} />)}
      </div>

      <div style={{ ...style.card, padding: 16 }}>
        <div style={{ display:"flex", justifyContent:"space-between", gap:12, flexWrap:"wrap", alignItems:"center" }}>
          <div><div style={{fontWeight:800,color:"#0f172a"}}>Data coverage</div><div style={{fontSize:11,color:"#64748b",marginTop:3}}>Use confidence and coverage before interpreting a score. Missing history lowers reliability.</div></div>
          <div style={{display:"flex",gap:8,flexWrap:"wrap"}}>
            <span style={{padding:"7px 10px",borderRadius:999,background:"#eff6ff",color:"#1d4ed8",fontSize:11,fontWeight:800}}>Attendance history: {summary?.employeesWithAttendanceData ?? 0}/{summary?.totalEmployees ?? records.length}</span>
            <span style={{padding:"7px 10px",borderRadius:999,background:"#f5f3ff",color:"#6d28d9",fontSize:11,fontWeight:800}}>Performance history: {summary?.employeesWithPerformanceData ?? 0}/{summary?.totalEmployees ?? records.length}</span>
            <span style={{padding:"7px 10px",borderRadius:999,background:"#f8fafc",color:"#334155",fontSize:11,fontWeight:800}}>Average confidence: {summary?.averageConfidence ?? 0}%</span>
          </div>
        </div>
      </div>

      <div style={{ ...style.card, padding: 6, display: "inline-flex", width: "fit-content", gap: 4 }}>
        {[
          ["EMPLOYEES", "Employee intelligence", Users],
          ["DEPARTMENTS", "Department intelligence", Building2],
        ].map(([value, label, Icon]) => (
          <button
            key={value}
            onClick={() => setActiveView(value)}
            style={{
              ...style.button,
              padding: "9px 12px",
              background: activeView === value ? "#2563eb" : "transparent",
              color: activeView === value ? "#fff" : "#475569",
            }}
          >
            <Icon size={15} /> {label}
          </button>
        ))}
      </div>

      {activeView === "EMPLOYEES" ? (
        <>
          <div style={{ ...style.card, padding: 14, display: "grid", gridTemplateColumns: "minmax(220px,1.5fr) repeat(4,minmax(135px,.7fr))", gap: 10 }}>
            <div style={{ position: "relative" }}>
              <Search size={15} style={{ position: "absolute", left: 11, top: 12, color: "#94a3b8" }} />
              <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search employee, department or role" style={{ ...style.input, paddingLeft: 34 }} />
            </div>
            <select value={department} onChange={(e) => setDepartment(e.target.value)} style={style.input}>
              <option value="ALL">All departments</option>
              {departmentOptions.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
            <select value={risk} onChange={(e) => setRisk(e.target.value)} style={style.input}>
              <option value="ALL">All risk levels</option>
              <option value="HIGH">High risk</option>
              <option value="MEDIUM">Medium risk</option>
              <option value="LOW">Low risk</option>
            </select>
            <select value={priority} onChange={(e) => setPriority(e.target.value)} style={style.input}>
              <option value="ALL">All priorities</option>
              <option value="URGENT">Urgent</option>
              <option value="WATCH">Watch</option>
              <option value="STABLE">Stable</option>
            </select>
            <select value={sort} onChange={(e) => setSort(e.target.value)} style={style.input}>
              <option value="RISK_DESC">Highest risk first</option>
              <option value="RISK_ASC">Lowest risk first</option>
              <option value="ENGAGEMENT_DESC">Highest engagement</option>
              <option value="POTENTIAL_DESC">Highest potential</option>
              <option value="BURNOUT_DESC">Highest burnout risk</option>
              <option value="NAME">Employee name</option>
            </select>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "minmax(0,1.55fr) minmax(330px,.85fr)", gap: 18, alignItems: "start" }}>
            <div style={{ ...style.card, overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 1080 }}>
                <thead>
                  <tr>
                    {["Employee", "Department", "Attrition", "Engagement", "Burnout", "Potential", "Skill gap", "Attendance", "Confidence", "Priority"].map((heading) => (
                      <th key={heading} style={{ textAlign: "left", padding: 13, fontSize: 10, color: "#64748b", background: "#f8fafc", borderBottom: "1px solid #e2e8f0", textTransform: "uppercase", letterSpacing: ".04em" }}>{heading}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan="10" style={{ padding: 30, color: "#64748b" }}>Loading workforce intelligence...</td></tr>
                  ) : filtered.length === 0 ? (
                    <tr><td colSpan="10" style={{ padding: 30, color: "#64748b" }}>No matching workforce records.</td></tr>
                  ) : filtered.map((row) => (
                    <tr
                      key={row.employeeId}
                      onClick={() => setSelectedId(String(row.employeeId))}
                      style={{ cursor: "pointer", background: String(row.employeeId) === String(selectedId) ? "#f8fbff" : "#fff" }}
                    >
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>
                        <strong style={{ color: "#0f172a" }}>{row.employeeName}</strong>
                        <div style={{ color: "#94a3b8", fontSize: 10 }}>{row.jobTitle || "Role not set"} · ID {row.employeeId}</div>
                      </td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9", color: "#475569" }}>{row.department || "Unassigned"}</td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.attritionRiskLevel}>{row.attritionRiskLevel} · {row.attritionRiskScore}%</Badge></td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.engagementLevel} positive>{row.engagementLevel} · {row.engagementScore}%</Badge></td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.burnoutRiskLevel}>{row.burnoutRiskLevel} · {row.burnoutRiskScore}%</Badge></td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.growthPotentialLevel} positive>{row.growthPotentialLevel} · {row.growthPotentialScore}%</Badge></td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9", color: "#334155" }}>{row.skillGapScore ?? 0}%</td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9", color: "#334155" }}>{safeNumber(row.attendanceRate).toFixed(1)}%</td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9", color: "#334155" }}>{row.confidenceScore ?? 0}%<div style={{ fontSize: 10, color: "#94a3b8" }}>{row.dataQuality || "LIMITED"}</div></td>
                      <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.priority}>{row.priority || "STABLE"}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ ...style.card, padding: 18, display: "grid", gap: 16, position: "sticky", top: 16 }}>
              {!selected ? (
                <div style={{ color: "#64748b" }}>Select an employee to view detailed AI insight.</div>
              ) : (
                <>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "flex-start" }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: 18, color: "#0f172a" }}>{selected.employeeName}</div>
                      <div style={{ color: "#64748b", fontSize: 12 }}>{selected.jobTitle || "Role not set"} · {selected.department || "Unassigned"}</div>
                      <div style={{ color: "#94a3b8", fontSize: 10, marginTop: 3 }}>{selected.tenureMonths ?? 0} months tenure · {selected.performanceReviewCount ?? 0} review(s)</div>
                    </div>
                    <button onClick={refreshEmployee} title="Re-run analysis" style={{ ...style.button, padding: 8, background: "#f1f5f9", color: "#334155" }}><RefreshCw size={15} /></button>
                  </div>

                  <div style={{ padding: 13, borderRadius: 12, background: "#f8fafc", color: "#334155", fontSize: 13, lineHeight: 1.6 }}>
                    <Sparkles size={15} style={{ verticalAlign: "middle", marginRight: 7 }} />
                    {selected.insightSummary || "No narrative available."}
                  </div>

                  <div style={{ display: "grid", gap: 12 }}>
                    <Meter value={selected.attritionRiskScore} label="Predicted attrition risk" tone="risk" />
                    <Meter value={selected.engagementScore} label="Engagement" tone="positive" />
                    <Meter value={selected.burnoutRiskScore} label="Burnout risk" tone="risk" />
                    <Meter value={selected.growthPotentialScore} label="Growth potential" tone="positive" />
                    <Meter value={selected.skillGapScore} label="Skill-gap severity" tone="risk" />
                    <Meter value={selected.confidenceScore} label="Data confidence" tone="positive" />
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                    {[
                      ["Attendance", `${safeNumber(selected.attendanceRate).toFixed(1)}%`],
                      ["Avg. workday", selected.averageWorkingHours ? `${safeNumber(selected.averageWorkingHours).toFixed(1)}h` : "No data"],
                      ["Absent days", selected.absentDays ?? 0],
                      ["Pending leave", selected.pendingLeaveRequests ?? 0],
                      ["Perf. rating", selected.averagePerformanceRating ? `${selected.averagePerformanceRating}/5` : "No data"],
                      ["Perf. trend", String(selected.performanceTrend || "INSUFFICIENT_DATA").replaceAll("_", " ")],
                      ["Retention", `${selected.retentionScore ?? 0}%`],
                      ["Data quality", selected.dataQuality || "LIMITED"],
                    ].map(([label, value]) => (
                      <div key={label} style={{ padding: 10, border: "1px solid #e2e8f0", borderRadius: 10 }}>
                        <div style={{ fontSize: 10, color: "#94a3b8" }}>{label}</div>
                        <strong style={{ color: "#0f172a", fontSize: 12 }}>{value}</strong>
                      </div>
                    ))}
                  </div>

                  <InsightList title="Risk signals" items={selected.riskFactors} icon={ShieldAlert} />
                  <InsightList title="Skill gaps" items={selected.skillGapIndicators} icon={Target} />
                  <InsightList title="Strengths" items={selected.strengths} icon={TrendingUp} />
                  <InsightList title="Recommended HR actions" items={selected.recommendations} icon={Sparkles} />
                </>
              )}
            </div>
          </div>
        </>
      ) : (
        <div style={{ ...style.card, overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 900 }}>
            <thead>
              <tr>
                {["Department", "Employees", "Avg risk", "Avg engagement", "Avg attendance", "Burnout", "High risk", "High potential", "Urgent", "Health"].map((heading) => (
                  <th key={heading} style={{ textAlign: "left", padding: 13, fontSize: 10, color: "#64748b", background: "#f8fafc", borderBottom: "1px solid #e2e8f0", textTransform: "uppercase", letterSpacing: ".04em" }}>{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {departments.length === 0 ? (
                <tr><td colSpan="10" style={{ padding: 30, color: "#64748b" }}>No department intelligence available.</td></tr>
              ) : departments.map((row) => (
                <tr key={row.department}>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9", fontWeight: 800, color: "#0f172a" }}>{row.department}</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.employees}</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.averageRisk}%</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.averageEngagement}%</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{safeNumber(row.averageAttendance).toFixed(1)}%</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.averageBurnoutRisk}%</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.highRiskEmployees}</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.highPotentialEmployees}</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}>{row.urgentActions}</td>
                  <td style={{ padding: 13, borderBottom: "1px solid #f1f5f9" }}><Badge level={row.health} positive={row.health === "HEALTHY"}>{String(row.health || "WATCH").replaceAll("_", " ")}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(280px,1fr))", gap: 14 }}>
        <Distribution title="Attrition risk distribution" values={summary?.riskDistribution || {}} />
        <Distribution title="Engagement distribution" values={summary?.engagementDistribution || {}} positive />
        {summary?.departmentPopulation && (
          <div style={{ ...style.card, padding: 18 }}>
            <div style={{ fontWeight: 800, color: "#0f172a", marginBottom: 13 }}>Department workforce distribution</div>
            <div style={{ display: "grid", gap: 10 }}>
              {Object.entries(summary.departmentPopulation).map(([name, count]) => {
                const total = Math.max(1, safeNumber(summary.totalEmployees || records.length || 1));
                const pct = Math.round((safeNumber(count) * 100) / total);
                return (
                  <div key={name}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: "#475569" }}><span>{name}</span><span>{count} · {pct}%</span></div>
                    <div style={{ height: 7, background: "#e2e8f0", borderRadius: 999, overflow: "hidden", marginTop: 5 }}><div style={{ height: "100%", width: `${pct}%`, background: "#2563eb" }} /></div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {summary?.topRiskSignals && Object.keys(summary.topRiskSignals).length > 0 && (
        <div style={{ ...style.card, padding: 18 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, fontWeight: 800, color: "#0f172a", marginBottom: 13 }}><AlertTriangle size={17} /> Workforce risk-signal frequency</div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {Object.entries(summary.topRiskSignals).sort((a, b) => safeNumber(b[1]) - safeNumber(a[1])).map(([signal, count]) => (
              <span key={signal} style={{ border: "1px solid #fed7aa", background: "#fff7ed", color: "#9a3412", borderRadius: 999, padding: "7px 10px", fontSize: 11, fontWeight: 700 }}>{signal} · {count}</span>
            ))}
          </div>
        </div>
      )}

      <div style={{ fontSize: 10, color: "#94a3b8", lineHeight: 1.6 }}>
        Methodology: {summary?.methodology || "This module is an explainable decision-support system. Human review is required."}
      </div>
    </div>
  );
}
