import React, { useEffect, useMemo, useState } from "react";
import { BriefcaseBusiness, CheckCircle2, Search, Send, ShieldCheck } from "lucide-react";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
const publicApi = async (path, options = {}) => {
  const response = await fetch(`${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`, {
    ...options,
    headers: { Accept: "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}), ...(options.headers || {}) },
  });
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) throw new Error(data?.message || data?.error || raw || `Request failed with status ${response.status}.`);
  return data;
};

const emptyForm = {
  jobId: "", applicantName: "", email: "", phone: "", qualification: "", experienceYears: "",
  skills: "", currentCity: "", linkedinUrl: "", portfolioUrl: "", resumeUrl: "",
  expectedSalary: "", noticePeriod: "", coverLetter: "", source: "PUBLIC_PORTAL",
};

export default function PublicCareers() {
  const [jobs, setJobs] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [selectedJob, setSelectedJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(null);
  const [query, setQuery] = useState("");
  const [track, setTrack] = useState({ reference: "", email: "" });
  const [trackResult, setTrackResult] = useState(null);

  useEffect(() => {
    publicApi("/public/jobs")
      .then((rows) => {
        const list = Array.isArray(rows) ? rows : [];
        setJobs(list);
        const params = new URLSearchParams(window.location.search);
        const requested = params.get("job");
        const initial = list.find((j) => String(j.id) === String(requested)) || null;
        if (initial) { setSelectedJob(initial); setForm((f) => ({ ...f, jobId: String(initial.id) })); }
      })
      .catch((e) => setError(e.message || "Unable to load careers."))
      .finally(() => setLoading(false));
  }, []);

  const filteredJobs = useMemo(() => {
    const q = query.trim().toLowerCase();
    return jobs.filter((j) => !q || [j.title, j.department, j.location, j.employmentType].some((v) => String(v || "").toLowerCase().includes(q)));
  }, [jobs, query]);

  const chooseJob = (job) => {
    setSelectedJob(job);
    setForm((f) => ({ ...f, jobId: String(job.id) }));
    setSuccess(null); setError("");
    setTimeout(() => document.getElementById("apply-form")?.scrollIntoView({ behavior: "smooth", block: "start" }), 20);
  };

  const submit = async (e) => {
    e.preventDefault(); setError(""); setSuccess(null);
    if (!form.jobId || !form.applicantName.trim() || !form.email.trim()) { setError("Job, applicant name and email are required."); return; }
    try {
      setSaving(true);
      const payload = {
        job: { id: Number(form.jobId) }, applicantName: form.applicantName.trim(), email: form.email.trim(), phone: form.phone.trim(),
        qualification: form.qualification.trim(), experienceYears: form.experienceYears === "" ? null : Number(form.experienceYears), skills: form.skills.trim(),
        currentCity: form.currentCity.trim(), linkedinUrl: form.linkedinUrl.trim(), portfolioUrl: form.portfolioUrl.trim(), resumeUrl: form.resumeUrl.trim(),
        expectedSalary: form.expectedSalary === "" ? null : Number(form.expectedSalary), noticePeriod: form.noticePeriod.trim(), coverLetter: form.coverLetter.trim(), source: form.source,
      };
      const result = await publicApi("/public/applications", { method: "POST", body: JSON.stringify(payload) });
      setSuccess(result);
      setTrack({ reference: result.applicationReference || "", email: form.email.trim() });
      setForm((f) => ({ ...emptyForm, jobId: f.jobId }));
    } catch (e2) { setError(e2.message || "Unable to submit application."); }
    finally { setSaving(false); }
  };

  const trackStatus = async (e) => {
    e.preventDefault(); setTrackResult(null); setError("");
    try {
      const result = await publicApi(`/public/applications/status?reference=${encodeURIComponent(track.reference)}&email=${encodeURIComponent(track.email)}`);
      setTrackResult(result);
    } catch (e2) { setError(e2.message || "Unable to track application."); }
  };

  const field = (label, name, type = "text", placeholder = "") => (
    <label className="career-field"><span>{label}</span><input type={type} value={form[name]} placeholder={placeholder} onChange={(e) => setForm((f) => ({ ...f, [name]: e.target.value }))} /></label>
  );

  return <div className="career-page">
    <style>{`
      body{margin:0;background:#f6f8fc;font-family:Inter,system-ui,sans-serif;color:#0f172a}.career-page{min-height:100vh}.career-nav{height:72px;background:#fff;border-bottom:1px solid #e2e8f0;display:flex;align-items:center;justify-content:space-between;padding:0 max(24px,6vw);position:sticky;top:0;z-index:10}.career-brand{display:flex;align-items:center;gap:12px;font-weight:800}.career-logo{width:38px;height:38px;border-radius:11px;background:linear-gradient(135deg,#2563eb,#7c3aed);color:#fff;display:grid;place-items:center}.career-login{border:1px solid #dbe3ef;background:#fff;border-radius:9px;padding:9px 14px;font-weight:700;cursor:pointer}.career-hero{padding:56px max(24px,6vw) 34px;background:linear-gradient(135deg,#eef4ff,#faf7ff)}.career-hero h1{font-size:40px;margin:0 0 12px}.career-hero p{max-width:760px;color:#64748b;line-height:1.7}.career-shell{max-width:1180px;margin:auto;padding:28px 24px 60px}.career-grid{display:grid;grid-template-columns:1fr 1fr;gap:24px}.career-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:20px;box-shadow:0 8px 26px rgba(15,23,42,.04)}.career-search{display:flex;align-items:center;gap:8px;border:1px solid #dbe3ef;border-radius:10px;padding:0 12px;margin-bottom:14px}.career-search input{border:0;outline:0;padding:12px 0;width:100%}.job{border:1px solid #e8edf4;border-radius:12px;padding:16px;margin-bottom:12px}.job h3{margin:0 0 8px}.job-meta{color:#64748b;font-size:13px;margin-bottom:10px}.job button,.career-primary{border:0;border-radius:9px;background:#2563eb;color:#fff;padding:10px 14px;font-weight:800;cursor:pointer}.career-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:13px}.career-field{display:grid;gap:6px;font-size:12px;font-weight:700;color:#475569}.career-field input,.career-field textarea,.career-field select{border:1px solid #dbe3ef;border-radius:9px;padding:10px 11px;font:inherit;color:#0f172a;background:#fff}.career-wide{grid-column:1/-1}.career-alert{padding:12px 14px;border-radius:10px;margin-bottom:14px}.career-error{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.career-success{background:#f0fdf4;color:#166534;border:1px solid #bbf7d0}.career-track{margin-top:24px}.career-track-grid{display:grid;grid-template-columns:1fr 1fr auto;gap:10px}.career-track input{border:1px solid #dbe3ef;border-radius:9px;padding:10px}.career-status{margin-top:12px;padding:14px;border-radius:10px;background:#eff6ff;color:#1d4ed8}.career-empty{padding:24px;text-align:center;color:#64748b}@media(max-width:850px){.career-grid{grid-template-columns:1fr}.career-form-grid{grid-template-columns:1fr}.career-wide{grid-column:auto}.career-track-grid{grid-template-columns:1fr}.career-hero h1{font-size:30px}}
    `}</style>
    <div className="career-nav"><div className="career-brand"><span className="career-logo">NX</span><div>NexusHR Careers<div style={{fontSize:10,color:"#94a3b8",fontWeight:600}}>PUBLIC APPLICATION PORTAL</div></div></div><button className="career-login" onClick={() => window.location.href = "/"}>Employee Login</button></div>
    <section className="career-hero"><div style={{maxWidth:1180,margin:"auto"}}><h1>Build your career with us.</h1><p>Browse open roles and apply directly through the NexusHR public recruitment portal. No NexusHR login is required. After submitting, keep your application reference to track your recruitment status.</p></div></section>
    <main className="career-shell">
      {error && <div className="career-alert career-error">{error}</div>}
      {success && <div className="career-alert career-success"><CheckCircle2 size={18} style={{verticalAlign:"middle",marginRight:8}}/>Application submitted. Reference: <strong>{success.applicationReference}</strong>. Keep this reference for status tracking.</div>}
      <div className="career-grid">
        <section className="career-card"><h2><BriefcaseBusiness size={20}/> Open positions</h2><div className="career-search"><Search size={16}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search role, department or location"/></div>
          {loading ? <div className="career-empty">Loading opportunities...</div> : filteredJobs.length === 0 ? <div className="career-empty">No open roles found.</div> : filteredJobs.map((job)=><div className="job" key={job.id}><h3>{job.title}</h3><div className="job-meta">{job.department} · {job.location} · {String(job.employmentType||"").replaceAll("_"," ")}</div><div style={{fontSize:13,color:"#475569",lineHeight:1.55,marginBottom:10}}>{job.description || "See role details after selecting this position."}</div><button onClick={()=>chooseJob(job)}>Apply now</button></div>)}
        </section>
        <section className="career-card" id="apply-form"><h2>Application form</h2><p style={{color:"#64748b",fontSize:13}}>{selectedJob ? `Applying for ${selectedJob.title}` : "Select an open job to start your application."}</p>
          <form onSubmit={submit} className="career-form-grid">
            <label className="career-field career-wide"><span>Position *</span><select value={form.jobId} onChange={(e)=>{const id=e.target.value;setForm(f=>({...f,jobId:id}));setSelectedJob(jobs.find(j=>String(j.id)===String(id))||null);}}><option value="">Select a role</option>{jobs.map(j=><option key={j.id} value={j.id}>{j.title} — {j.location}</option>)}</select></label>
            {field("Full name *","applicantName","text","Your full name")}{field("Email *","email","email","you@example.com")}{field("Phone","phone","tel","10-digit mobile number")}{field("Current city","currentCity","text","Nashik")}{field("Highest qualification","qualification","text","B.E. Computer Engineering")}{field("Experience (years)","experienceYears","number","0")}{field("Expected salary","expectedSalary","number","Optional")}{field("Notice period","noticePeriod","text","Immediate / 30 days")}{field("LinkedIn URL","linkedinUrl","url","https://linkedin.com/in/...")}{field("Portfolio / GitHub URL","portfolioUrl","url","https://github.com/...")}{field("Resume URL","resumeUrl","url","Google Drive / portfolio resume link")}
            <label className="career-field career-wide"><span>Skills</span><textarea rows="3" value={form.skills} onChange={(e)=>setForm(f=>({...f,skills:e.target.value}))} placeholder="Java, Spring Boot, React, SQL..."/></label>
            <label className="career-field career-wide"><span>Cover letter</span><textarea rows="5" value={form.coverLetter} onChange={(e)=>setForm(f=>({...f,coverLetter:e.target.value}))} placeholder="Tell us why you are interested in this role..."/></label>
            <button className="career-primary career-wide" disabled={saving || !form.jobId}>{saving ? "Submitting..." : <><Send size={16} style={{verticalAlign:"middle",marginRight:7}}/>Submit application</>}</button>
          </form>
        </section>
      </div>
      <section className="career-card career-track"><h2><ShieldCheck size={20}/> Track application</h2><div className="career-track-grid"><input placeholder="Application reference" value={track.reference} onChange={(e)=>setTrack(t=>({...t,reference:e.target.value}))}/><input placeholder="Application email" value={track.email} onChange={(e)=>setTrack(t=>({...t,email:e.target.value}))}/><button className="career-primary" onClick={trackStatus}>Check status</button></div>{trackResult && <div className="career-status"><strong>{trackResult.jobTitle}</strong><br/>Reference: {trackResult.applicationReference}<br/>Current status: <strong>{String(trackResult.status||"").replaceAll("_"," ")}</strong>{trackResult.interview && <div style={{marginTop:14,paddingTop:12,borderTop:"1px solid #bfdbfe",lineHeight:1.7}}><strong>Interview {String(trackResult.interview.status||"SCHEDULED").replaceAll("_"," ")}</strong><br/>Date: <strong>{trackResult.interview.date}</strong><br/>Time: <strong>{String(trackResult.interview.time||"").slice(0,5)}</strong><br/>Location: <strong>{trackResult.interview.location}</strong>{trackResult.interview.message && <><br/><span style={{display:"block",marginTop:7,color:"#334155"}}>{trackResult.interview.message}</span></>}</div>}{trackResult.finalSelection && <div style={{marginTop:14,paddingTop:12,borderTop:"1px solid #86efac",lineHeight:1.7,background:"#f0fdf4",padding:"12px",borderRadius:"10px"}}><strong style={{color:"#166534"}}>Final Selection Approved</strong>{trackResult.finalSelection.message && <><br/><span style={{display:"block",marginTop:7}}>{trackResult.finalSelection.message}</span></>}<br/><strong>Required documents</strong><br/><span style={{whiteSpace:"pre-wrap"}}>{trackResult.finalSelection.requiredDocuments||"Please contact HR for the document checklist."}</span>{trackResult.finalSelection.reportingInstructions && <><br/><strong>Other instructions</strong><br/><span style={{whiteSpace:"pre-wrap"}}>{trackResult.finalSelection.reportingInstructions}</span></>}</div>}</div>}</section>
    </main>
  </div>;
}
