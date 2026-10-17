import { useEffect, useMemo, useState } from "react";
import { apiFetch, apiUrl } from "./api/apiClient";

const LEAVE_API = apiUrl("/leaves");

const TYPES = ["CASUAL", "SICK", "ANNUAL", "MATERNITY", "PATERNITY", "UNPAID", "OTHER"];
const STATUSES = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];

function Icon({ children, size = 18 }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{children}</svg>;
}
const Search = () => <Icon><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></Icon>;
const Plus = () => <Icon><path d="M12 5v14M5 12h14"/></Icon>;
const Calendar = () => <Icon><rect x="3.5" y="5" width="17" height="15" rx="2.5"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/></Icon>;
const Check = () => <Icon><path d="m5 12 4.5 4.5L19 7"/></Icon>;
const X = () => <Icon><path d="M6 6l12 12M18 6 6 18"/></Icon>;
const Refresh = () => <Icon><path d="M20 12a8 8 0 1 1-2.3-5.6"/><path d="M20 4v5h-5"/></Icon>;
const User = () => <Icon><circle cx="12" cy="8" r="3"/><path d="M5 20a7 7 0 0 1 14 0"/></Icon>;

function nameOf(employee) {
  if (!employee) return "Unknown employee";
  return `${employee.firstName || ""} ${employee.lastName || ""}`.trim() || employee.email || `Employee #${employee.id}`;
}
function initials(employee) {
  const value = `${employee?.firstName || ""}${employee?.lastName || ""}`.trim();
  return value ? value.slice(0,2).toUpperCase() : "U";
}
function formatDate(value) {
  if (!value) return "-";
  const date = new Date(`${String(value).slice(0,10)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("en-IN",{day:"2-digit",month:"short",year:"numeric"});
}
function daysBetween(start,end) {
  if (!start || !end) return 0;
  const a = new Date(`${start}T00:00:00`);
  const b = new Date(`${end}T00:00:00`);
  const diff = Math.round((b-a)/86400000)+1;
  return diff > 0 ? diff : 0;
}

function currentRole() {
  try {
    const raw = window.localStorage.getItem("nexushr-user");
    const user = raw ? JSON.parse(raw) : null;
    return String(user?.role || "").replace(/^ROLE_/, "").toUpperCase();
  } catch {
    return "";
  }
}

const COMPONENT_STYLES = `
/* NexusHR Leave Management */
.nx-leave { color:var(--text); }
.nx-leave * { box-sizing:border-box; }
.nx-leave-header { display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:22px; }
.nx-leave-kicker { display:flex;align-items:center;gap:7px;text-transform:uppercase;letter-spacing:1px;font-size:9px;font-weight:800;color:#7c3aed;margin-bottom:8px; }
.nx-leave-kicker span { width:6px;height:6px;border-radius:50%;background:#8b5cf6;box-shadow:0 0 8px rgba(139,92,246,.45); }
.nx-leave-header h2 { margin:0;font-size:27px;letter-spacing:-.8px; }
.nx-leave-header p { margin:7px 0 0;color:var(--text-secondary);font-size:11px; }
.nx-leave-header-actions { display:flex;gap:8px; }
.nx-leave-primary,.nx-leave-secondary,.nx-leave-refresh { height:40px;border-radius:9px;padding:0 13px;display:inline-flex;align-items:center;justify-content:center;gap:7px;font-size:10px;font-weight:800;cursor:pointer; }
.nx-leave-primary { border:0;background:var(--primary);color:#fff;box-shadow:0 7px 18px rgba(37,99,235,.16); }
.nx-leave-primary:hover { background:var(--primary-dark); }
.nx-leave-refresh,.nx-leave-secondary { border:1px solid var(--border);background:var(--surface);color:var(--text-secondary); }
.nx-leave-refresh:hover,.nx-leave-secondary:hover { background:var(--surface-hover);color:var(--text); }
.nx-leave-alert { display:flex;align-items:center;gap:10px;padding:12px 14px;background:var(--red-soft);border:1px solid var(--red-border);color:var(--red);border-radius:10px;margin-bottom:16px;font-size:11px; }
.nx-leave-alert span{flex:1}.nx-leave-alert button{border:0;background:transparent;color:inherit;font-size:18px;cursor:pointer}
.nx-leave-stats { display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px; }
.nx-leave-stats > div { min-height:105px;background:var(--surface);border:1px solid var(--border);border-radius:13px;padding:16px;display:flex;align-items:center;gap:12px;box-shadow:0 3px 12px rgba(15,23,42,.03); }
.nx-leave-stat-icon { width:40px;height:40px;border-radius:11px;display:grid;place-items:center;flex:none;font-weight:800; }
.nx-leave-stat-icon.blue{color:#2563eb;background:#eff6ff}.nx-leave-stat-icon.orange{color:#d97706;background:#fffbeb}.nx-leave-stat-icon.green{color:#16a34a;background:#f0fdf4}.nx-leave-stat-icon.red{color:#dc2626;background:#fef2f2}
.nx-leave-stats small,.nx-leave-stats em{display:block;font-size:9px;color:var(--text-light);font-style:normal}
.nx-leave-stats strong{display:block;font-size:22px;line-height:1.2;margin:3px 0}
.nx-leave-toolbar { background:var(--surface);border:1px solid var(--border);border-radius:13px;padding:12px;display:flex;justify-content:space-between;gap:12px;margin-bottom:12px; }
.nx-leave-search { height:38px;width:310px;display:flex;align-items:center;gap:8px;padding:0 11px;border:1px solid var(--border);border-radius:9px;background:var(--surface-alt);color:var(--text-light); }
.nx-leave-search input{flex:1;min-width:0;border:0;outline:0;background:transparent;font-size:11px}
.nx-leave-selects{display:flex;gap:7px}.nx-leave-selects select{height:38px;border:1px solid var(--border);border-radius:9px;background:var(--surface);padding:0 10px;color:var(--text-secondary);font-size:10px;outline:0}
.nx-leave-card{background:var(--surface);border:1px solid var(--border);border-radius:14px;overflow:hidden;box-shadow:0 3px 12px rgba(15,23,42,.03)}
.nx-leave-card-head{min-height:66px;padding:14px 18px;border-bottom:1px solid var(--border-light);display:flex;align-items:center;justify-content:space-between}
.nx-leave-card-head h3{margin:0;font-size:13px}.nx-leave-card-head p{margin:4px 0 0;font-size:9px;color:var(--text-light)}
.nx-leave-policy{font-size:8px;text-transform:uppercase;letter-spacing:.7px;color:#7c3aed;background:#f5f3ff;padding:6px 8px;border-radius:6px;font-weight:800}
.nx-leave-table-wrap{
  width:100%;
  overflow-x:auto;
}

.nx-leave-table{
  width:100%;
  border-collapse:collapse;
  table-layout:fixed;
}

.nx-leave-table th:nth-child(1),
.nx-leave-table td:nth-child(1){width:25%}

.nx-leave-table th:nth-child(2),
.nx-leave-table td:nth-child(2){width:11%}

.nx-leave-table th:nth-child(3),
.nx-leave-table td:nth-child(3){width:25%}

.nx-leave-table th:nth-child(4),
.nx-leave-table td:nth-child(4){width:7%}

.nx-leave-table th:nth-child(5),
.nx-leave-table td:nth-child(5){width:12%}

.nx-leave-table th:nth-child(6),
.nx-leave-table td:nth-child(6){width:10%}

.nx-leave-table th:nth-child(7),
.nx-leave-table td:nth-child(7){width:10%}
.nx-leave-table th{padding:11px 15px;background:var(--surface-alt);border-bottom:1px solid var(--border);color:var(--text-light);font-size:9px;text-transform:uppercase;letter-spacing:.5px;text-align:left;white-space:nowrap}
.nx-leave-table td{padding:12px 15px;border-bottom:1px solid var(--border-light);font-size:10px;color:var(--text-secondary);vertical-align:middle}
.nx-leave-table tbody tr:hover{background:var(--surface-hover)}
.nx-leave-person{display:flex;align-items:center;gap:9px}.nx-leave-person>span{width:32px;height:32px;border-radius:9px;background:#f5f3ff;color:#7c3aed;display:grid;place-items:center;font-size:9px;font-weight:800}
.nx-leave-person strong,.nx-leave-person small{display:block}.nx-leave-person strong{font-size:10px;color:var(--text)}.nx-leave-person small{font-size:8px;color:var(--text-light);margin-top:3px}
.nx-leave-type{font-size:8px;font-weight:800;color:#475569;background:#f1f5f9;padding:6px 8px;border-radius:6px}
.nx-leave-period{display:flex;align-items:center;gap:5px}.nx-leave-period strong{font-size:9px;color:var(--text)}.nx-leave-period span{color:var(--text-light);font-size:8px}
.nx-leave-days{color:var(--text);font-size:12px}.nx-leave-reason{display:block;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.nx-leave-status{display:inline-flex;align-items:center;gap:5px;padding:5px 8px;border-radius:20px;font-size:8px;font-weight:800}
.nx-leave-status i{width:5px;height:5px;border-radius:50%;background:currentColor}
.nx-leave-status.pending{color:#b45309;background:#fffbeb}.nx-leave-status.approved{color:#15803d;background:#f0fdf4}.nx-leave-status.rejected{color:#b91c1c;background:#fef2f2}.nx-leave-status.cancelled{color:#64748b;background:#f1f5f9}
.nx-leave-actions{display:flex;gap:5px;align-items:center;white-space:nowrap;justify-content:flex-start}.nx-leave-actions button{border:0;border-radius:7px;padding:7px 9px;font-size:8px;font-weight:800;cursor:pointer}
.nx-leave-actions .approve{background:#eff6ff;color:#2563eb}.nx-leave-actions .cancel{background:#f8fafc;color:#475569;border:1px solid var(--border);min-width:58px;text-align:center}
.nx-leave-loading{min-height:270px;display:flex;align-items:center;justify-content:center;gap:5px;color:var(--text-light);font-size:10px}
.nx-leave-loading span{width:6px;height:6px;border-radius:50%;background:#7c3aed;animation:nx-leave-pulse 1s infinite}.nx-leave-loading span:nth-child(2){animation-delay:.15s}.nx-leave-loading span:nth-child(3){animation-delay:.3s}
@keyframes nx-leave-pulse{0%,80%,100%{opacity:.25;transform:scale(.8)}40%{opacity:1;transform:scale(1)}}
.nx-leave-empty{min-height:270px;display:grid;place-items:center;align-content:center;gap:6px;text-align:center;color:var(--text-light)}.nx-leave-empty h4{margin:2px 0 0;color:var(--text);font-size:12px}.nx-leave-empty p{margin:0;font-size:10px}
.nx-leave-overlay{position:fixed;inset:0;background:rgba(15,23,42,.48);backdrop-filter:none;display:grid;place-items:center;padding:20px;z-index:900}
.nx-leave-modal{width:min(620px,100%);max-height:90vh;overflow:auto;background:var(--surface);border:1px solid var(--border);border-radius:18px;box-shadow:0 30px 80px rgba(15,23,42,.25);padding:22px}
.nx-leave-modal-head{display:flex;align-items:flex-start;justify-content:space-between;gap:15px;margin-bottom:22px}.nx-leave-modal-head>div{display:flex;gap:11px}.nx-leave-modal-icon{width:40px;height:40px;border-radius:10px;background:var(--primary-soft);color:var(--primary);display:grid;place-items:center;flex:none}
.nx-leave-modal-head h3{margin:2px 0 4px;font-size:16px}.nx-leave-modal-head p{margin:0;font-size:9px;color:var(--text-light)}
.nx-leave-modal-head>button{border:0;background:var(--surface-hover);color:var(--text-secondary);width:32px;height:32px;border-radius:8px;display:grid;place-items:center;cursor:pointer}
.nx-leave-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.nx-leave-form-grid label,.nx-leave-textarea-label{display:grid;gap:7px;color:var(--text-secondary);font-size:10px;font-weight:700}
.nx-leave-form-grid input,.nx-leave-form-grid select,.nx-leave-textarea-label textarea{width:100%;border:1px solid var(--border);border-radius:9px;background:var(--surface-alt);outline:0;padding:0 11px;height:42px;font-size:11px;color:var(--text)}
.nx-leave-form-grid input:focus,.nx-leave-form-grid select:focus,.nx-leave-textarea-label textarea:focus{border-color:var(--primary);box-shadow:0 0 0 3px rgba(37,99,235,.08)}
.nx-leave-textarea-label textarea{height:auto;padding:10px 11px;resize:vertical;line-height:1.5}
.nx-leave-duration{margin:16px 0;padding:11px 12px;border-radius:9px;background:var(--primary-soft);color:var(--text-secondary);font-size:10px;display:flex;justify-content:space-between}.nx-leave-duration strong{color:var(--primary)}
.nx-leave-modal-actions{display:flex;justify-content:flex-end;gap:7px;margin-top:20px}
.nx-leave-modal-actions button{height:40px;border-radius:9px;padding:0 13px;font-size:10px;font-weight:800;cursor:pointer}
.nx-leave-modal-actions .nx-leave-primary{border:0}.nx-leave-reject{border:0;background:#fef2f2;color:#b91c1c}.nx-leave-approve{border:0;background:#16a34a;color:#fff}.nx-leave-review-summary{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:18px}.nx-leave-review-summary>div{border:1px solid var(--border);background:var(--surface-alt);border-radius:9px;padding:11px}.nx-leave-review-summary small{display:block;color:var(--text-light);font-size:8px}.nx-leave-review-summary strong{display:block;margin-top:5px;font-size:10px}
.nx-leave-balance{margin:12px 0 0;padding:12px;border-radius:10px;border:1px solid var(--border);background:#f8fafc;display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.nx-leave-balance div{padding:8px;border-radius:8px;background:#fff;border:1px solid #e2e8f0}.nx-leave-balance small{display:block;font-size:8px;color:#64748b}.nx-leave-balance strong{display:block;margin-top:4px;font-size:13px;color:#0f172a}.nx-leave-balance-warning{margin-top:10px;padding:10px 12px;border-radius:9px;background:#fff7ed;color:#9a3412;font-size:10px;font-weight:700}.nx-leave-balance-ok{margin-top:10px;padding:10px 12px;border-radius:9px;background:#f0fdf4;color:#166534;font-size:10px;font-weight:700}
@media(max-width:1000px){.nx-leave-stats{grid-template-columns:repeat(2,1fr)}.nx-leave-toolbar{flex-direction:column}.nx-leave-search{width:100%}.nx-leave-selects{justify-content:flex-start}}
@media(max-width:650px){.nx-leave-balance{grid-template-columns:1fr 1fr}.nx-leave-header{flex-direction:column;align-items:flex-start}.nx-leave-header-actions{width:100%}.nx-leave-header-actions button{flex:1}.nx-leave-stats{grid-template-columns:1fr}.nx-leave-selects{display:grid;grid-template-columns:1fr 1fr}.nx-leave-selects select{width:100%}.nx-leave-form-grid{grid-template-columns:1fr}.nx-leave-modal{padding:17px}.nx-leave-modal-actions{flex-wrap:wrap}.nx-leave-modal-actions button{flex:1}}

`;

function Leave({ employees = [] }) {
  const [leaves,setLeaves] = useState([]);
  const [search,setSearch] = useState("");
  const [statusFilter,setStatusFilter] = useState("ALL");
  const [typeFilter,setTypeFilter] = useState("ALL");
  const [loading,setLoading] = useState(true);
  const [error,setError] = useState("");
  const [modalOpen,setModalOpen] = useState(false);
  const [review,setReview] = useState(null);
  const [reviewBalance,setReviewBalance] = useState(null);
  const [balanceLoading,setBalanceLoading] = useState(false);
  const [saving,setSaving] = useState(false);
  const [success,setSuccess] = useState("");
  const [form,setForm] = useState({employeeId:"",leaveType:"CASUAL",startDate:"",endDate:"",reason:""});
  const [reviewComment,setReviewComment] = useState("");
  const role = currentRole();
  const canCreateForEmployee = role === "ADMIN" || role === "HR";
  const canReview = role === "ADMIN" || role === "HR" || role === "MANAGER";
  const canCancelManagedLeave = role === "ADMIN" || role === "HR";
  const isManager = role === "MANAGER";

  const employeeMap = useMemo(() => {
    const map = new Map();
    employees.forEach(e => map.set(Number(e.id),e));
    return map;
  },[employees]);

  async function loadLeaves() {
    try {
      setLoading(true);
      setError("");
      const data = await apiFetch("/leaves");
      setLeaves(Array.isArray(data) ? data : []);
    } catch(err) {
      setError(err.message || "Unable to load leave requests.");
      setLeaves([]);
    } finally { setLoading(false); }
  }

  useEffect(()=>{ loadLeaves(); },[]);

  const filtered = useMemo(()=>{
    const q=search.trim().toLowerCase();
    return leaves.filter(leave=>{
      const employee=employeeMap.get(Number(leave.employeeId));
      const name=nameOf(employee).toLowerCase();
      const matchesSearch=!q || name.includes(q) || String(employee?.email||"").toLowerCase().includes(q) || String(leave.reason||"").toLowerCase().includes(q);
      const matchesStatus=statusFilter==="ALL" || String(leave.status||"").toUpperCase()===statusFilter;
      const matchesType=typeFilter==="ALL" || String(leave.leaveType||"").toUpperCase()===typeFilter;
      return matchesSearch && matchesStatus && matchesType;
    });
  },[leaves,employeeMap,search,statusFilter,typeFilter]);

  const stats=useMemo(()=>{
    const count=s=>leaves.filter(l=>String(l.status||"").toUpperCase()===s).length;
    const days=leaves.filter(l=>String(l.status||"").toUpperCase()==="APPROVED").reduce((sum,l)=>sum+Number(l.totalDays||daysBetween(l.startDate,l.endDate)||0),0);
    return {total:leaves.length,pending:count("PENDING"),approved:count("APPROVED"),rejected:count("REJECTED"),days};
  },[leaves]);

  function openApply() {
    setForm({employeeId:employees[0]?.id ? String(employees[0].id) : "",leaveType:"CASUAL",startDate:"",endDate:"",reason:""});
    setError("");
    setModalOpen(true);
  }

  async function submitLeave(e) {
    e.preventDefault();
    if(!form.employeeId || !form.startDate || !form.endDate) {
      setError("Employee, start date and end date are required.");
      return;
    }
    if(form.endDate < form.startDate) {
      setError("End date cannot be before start date.");
      return;
    }
    try {
      setSaving(true);
      setError("");
      const payload={
        employeeId:Number(form.employeeId),
        leaveType:form.leaveType,
        startDate:form.startDate,
        endDate:form.endDate,
        totalDays:daysBetween(form.startDate,form.endDate),
        reason:form.reason.trim()
      };
      await apiFetch("/leaves",{method:"POST",body:JSON.stringify(payload)});
      setModalOpen(false);
      await loadLeaves();
    } catch(err){setError(err.message||"Unable to submit leave request.");}
    finally{setSaving(false);}
  }

  async function openReview(leave) {
    setReview(leave);
    setReviewComment("");
    setReviewBalance(null);
    setError("");

    if (!leave?.employeeId || !leave?.leaveType || !leave?.startDate) return;

    try {
      setBalanceLoading(true);
      const year = String(leave.startDate).slice(0, 4);
      const balance = await apiFetch(
        `/leaves/employee/${leave.employeeId}/balance/${encodeURIComponent(leave.leaveType)}?year=${year}`
      );
      setReviewBalance(balance || null);
    } catch (err) {
      setError(err.message || "Unable to load leave balance.");
    } finally {
      setBalanceLoading(false);
    }
  }

  async function reviewLeave(action) {
    if (!review?.id) return;

    try {
      setSaving(true);
      setError("");

      const endpoint =
        action === "approve"
          ? `/leaves/${review.id}/approve?reviewComment=${encodeURIComponent(reviewComment || "")}`
          : `/leaves/${review.id}/reject?reviewComment=${encodeURIComponent(reviewComment || "")}`;

      await apiFetch(endpoint, {
        method: "PUT",
      });

      setReview(null);
      setReviewBalance(null);
      setReviewComment("");
      setSuccess(`Leave request ${action === "approve" ? "approved" : "rejected"} successfully.`);

      await loadLeaves();
    } catch (err) {
      console.error("Leave review error:", err);
      setError(
        err?.message ||
          `Unable to ${action} leave request.`
      );
    } finally {
      setSaving(false);
    }
  }

  async function cancelLeave(leave) {
    if(!window.confirm("Cancel this leave request?")) return;
    try{
      setSaving(true);
      await apiFetch(`/leaves/${leave.id}/cancel`,{method:"PUT"});
      await loadLeaves();
    }catch(err){setError(err.message||"Unable to cancel leave request.");}
    finally{setSaving(false);}
  }

  return (
    <>
      <style>{COMPONENT_STYLES}</style>
      <section className="nx-leave">
      <div className="nx-leave-header">
        <div>
          <div className="nx-leave-kicker"><span/> People Operations</div>
          <h2>Leave Management</h2>
          <p>Review requests, track approvals and maintain a clear leave workflow.{isManager ? " Use My Leave in the sidebar to raise your own request; your request requires Admin approval." : ""}</p>
        </div>
        <div className="nx-leave-header-actions">
          <button className="nx-leave-refresh" onClick={loadLeaves} disabled={loading}><Refresh/> Refresh</button>
          {canCreateForEmployee && <button className="nx-leave-primary" onClick={openApply}><Plus/> Create for employee</button>}
        </div>
      </div>

      {error && <div className="nx-leave-alert"><strong>Attention</strong><span>{error}</span><button onClick={()=>setError("")}>×</button></div>}
      {success && <div className="nx-leave-alert" style={{background:"#f0fdf4",borderColor:"#bbf7d0",color:"#166534"}}><strong>Done</strong><span>{success}</span><button onClick={()=>setSuccess("")}>×</button></div>}

      <div className="nx-leave-stats">
        <div><span className="nx-leave-stat-icon blue"><Calendar/></span><div><small>Total requests</small><strong>{stats.total}</strong><em>All submitted requests</em></div></div>
        <div><span className="nx-leave-stat-icon orange">&#8226;</span><div><small>Pending review</small><strong>{stats.pending}</strong><em>Awaiting management action</em></div></div>
        <div><span className="nx-leave-stat-icon green"><Check/></span><div><small>Approved</small><strong>{stats.approved}</strong><em>{stats.days} approved days</em></div></div>
        <div><span className="nx-leave-stat-icon red"><X/></span><div><small>Rejected</small><strong>{stats.rejected}</strong><em>Requests declined</em></div></div>
      </div>

      <div className="nx-leave-toolbar">
        <div className="nx-leave-search"><Search/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search employee, email or reason..."/></div>
        <div className="nx-leave-selects">
          <select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}><option value="ALL">All statuses</option>{STATUSES.map(s=><option key={s} value={s}>{s[0]+s.slice(1).toLowerCase()}</option>)}</select>
          <select value={typeFilter} onChange={e=>setTypeFilter(e.target.value)}><option value="ALL">All leave types</option>{TYPES.map(t=><option key={t} value={t}>{t[0]+t.slice(1).toLowerCase()}</option>)}</select>
        </div>
      </div>

      <div className="nx-leave-card">
        <div className="nx-leave-card-head"><div><h3>Leave requests</h3><p>{filtered.length} requests match your current filters</p></div><span className="nx-leave-policy">HR workflow</span></div>
        {loading ? <div className="nx-leave-loading"><span/><span/><span/><p>Loading requests...</p></div> :
        filtered.length===0 ? <div className="nx-leave-empty"><Calendar/><h4>No leave requests found</h4><p>Adjust your filters or create a new request.</p></div> :
        <div className="nx-leave-table-wrap"><table className="nx-leave-table">
          <thead><tr><th>Employee</th><th>Type</th><th>Period</th><th>Days</th><th>Reason</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>{filtered.map(leave=>{
            const employee=employeeMap.get(Number(leave.employeeId));
            const status=String(leave.status||"PENDING").toUpperCase();
            const days=Number(leave.totalDays||daysBetween(leave.startDate,leave.endDate)||0);
            return <tr key={leave.id}>
              <td><div className="nx-leave-person"><span>{initials(employee)}</span><div><strong>{nameOf(employee)}</strong><small>{employee?.department||employee?.email||"-"}</small></div></div></td>
              <td><span className="nx-leave-type">{String(leave.leaveType||"OTHER").replace("_"," ")}</span></td>
              <td><div className="nx-leave-period"><strong>{formatDate(leave.startDate)}</strong><span>to</span><strong>{formatDate(leave.endDate)}</strong></div></td>
              <td><strong className="nx-leave-days">{days}</strong></td>
              <td><span className="nx-leave-reason" title={leave.reason||""}>{leave.reason||"-"}</span></td>
              <td><span className={`nx-leave-status ${status.toLowerCase()}`}><i/>{status[0]+status.slice(1).toLowerCase()}</span></td>
              <td><div className="nx-leave-actions">
                {status==="PENDING" && canReview && <button className="approve" onClick={()=>openReview(leave)}><Check size={14}/> Review</button>}
                {(status==="PENDING"||status==="APPROVED") && canCancelManagedLeave && <button className="cancel" disabled={saving} onClick={()=>cancelLeave(leave)}>Cancel</button>}
              </div></td>
            </tr>
          })}</tbody>
        </table></div>}
      </div>

      {modalOpen && <div className="nx-leave-overlay" onMouseDown={e=>e.target===e.currentTarget&&setModalOpen(false)}>
        <div className="nx-leave-modal">
          <div className="nx-leave-modal-head"><div><span className="nx-leave-modal-icon"><Plus/></span><div><h3>Create leave request</h3><p>Create a request on behalf of an employee.</p></div></div><button onClick={()=>setModalOpen(false)}><X/></button></div>
          <form onSubmit={submitLeave}>
            <div className="nx-leave-form-grid">
              <label>Employee<select value={form.employeeId} onChange={e=>setForm({...form,employeeId:e.target.value})}><option value="">Select employee</option>{employees.map(e=><option key={e.id} value={e.id}>{nameOf(e)}</option>)}</select></label>
              <label>Leave type<select value={form.leaveType} onChange={e=>setForm({...form,leaveType:e.target.value})}>{TYPES.map(t=><option key={t}>{t}</option>)}</select></label>
              <label>Start date<input type="date" value={form.startDate} onChange={e=>setForm({...form,startDate:e.target.value})}/></label>
              <label>End date<input type="date" value={form.endDate} onChange={e=>setForm({...form,endDate:e.target.value})}/></label>
            </div>
            <div className="nx-leave-duration">Requested duration <strong>{daysBetween(form.startDate,form.endDate) || 0} day(s)</strong></div>
            <label className="nx-leave-textarea-label">Reason<textarea rows="4" value={form.reason} onChange={e=>setForm({...form,reason:e.target.value})} placeholder="Add a short reason for the leave request..."/></label>
            <div className="nx-leave-modal-actions"><button type="button" className="nx-leave-secondary" onClick={()=>setModalOpen(false)}>Cancel</button><button type="submit" className="nx-leave-primary" disabled={saving}>{saving?"Submitting...":"Submit request"}</button></div>
          </form>
        </div>
      </div>}

      {review && <div className="nx-leave-overlay" onMouseDown={e=>e.target===e.currentTarget&&setReview(null)}>
        <div className="nx-leave-modal nx-leave-review-modal">
          <div className="nx-leave-modal-head"><div><span className="nx-leave-modal-icon"><Check/></span><div><h3>Review leave request</h3><p>{nameOf(employeeMap.get(Number(review.employeeId)))} · {String(review.leaveType||"OTHER").replace("_"," ")}</p></div></div><button onClick={()=>setReview(null)}><X/></button></div>
          <div className="nx-leave-review-summary">
            <div><small>Period</small><strong>{formatDate(review.startDate)} - {formatDate(review.endDate)}</strong></div>
            <div><small>Duration</small><strong>{Number(review.totalDays||daysBetween(review.startDate,review.endDate))} day(s)</strong></div>
          </div>
          {balanceLoading ? (
            <div className="nx-leave-balance-ok">Checking employee leave balance…</div>
          ) : reviewBalance ? (
            <>
              <div className="nx-leave-balance">
                <div><small>Entitlement</small><strong>{reviewBalance.annualEntitlement} days</strong></div>
                <div><small>Approved</small><strong>{reviewBalance.approvedDays} days</strong></div>
                <div><small>Pending</small><strong>{reviewBalance.pendingDays} days</strong></div>
                <div><small>Remaining</small><strong>{reviewBalance.remainingDays} days</strong></div>
              </div>
              {Number(review.totalDays||daysBetween(review.startDate,review.endDate)) > Number(reviewBalance.remainingDays||0) ? (
                <div className="nx-leave-balance-warning">Cannot approve this request: it exceeds the remaining {review.leaveType} balance by {Number(review.totalDays||daysBetween(review.startDate,review.endDate))-Number(reviewBalance.remainingDays||0)} day(s). You can reject the request or ask the employee to shorten it.</div>
              ) : (
                <div className="nx-leave-balance-ok">Balance check passed. This request can be approved.</div>
              )}
            </>
          ) : null}
          <label className="nx-leave-textarea-label">Review comment<textarea rows="4" value={reviewComment} onChange={e=>setReviewComment(e.target.value)} placeholder="Optional comment for the employee..."/></label>
          <div className="nx-leave-modal-actions"><button className="nx-leave-secondary" onClick={()=>setReview(null)}>Cancel</button><button className="nx-leave-reject" disabled={saving} onClick={()=>reviewLeave("reject")}><X size={15}/> Reject</button><button className="nx-leave-approve" disabled={saving || balanceLoading || (reviewBalance && Number(review.totalDays||daysBetween(review.startDate,review.endDate)) > Number(reviewBalance.remainingDays||0))} onClick={()=>reviewLeave("approve")}><Check size={15}/> Approve</button></div>
        </div>
      </div>}
      </section>
    </>
  );
}

export default Leave;
