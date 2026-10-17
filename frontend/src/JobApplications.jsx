import React, { useEffect, useMemo, useState } from "react";
import { apiFetch } from "./api/apiClient";

const STATUS_ORDER = ["APPLIED","SCREENING","MANAGER_SHORTLISTED","ADMIN_APPROVED","INTERVIEW","INTERVIEW_COMPLETED","OFFER","HIRED","REJECTED"];
const label = (value) => String(value || "").replaceAll("_", " ").replace(/\b\w/g, (c) => c.toUpperCase());
const roleOf = () => {
  try { return String(JSON.parse(localStorage.getItem("nexushr-user") || "{}")?.role || "").replace(/^ROLE_/, "").toUpperCase(); }
  catch { return ""; }
};

export default function JobApplications() {
  const [applications, setApplications] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [jobId, setJobId] = useState("ALL");
  const [selected, setSelected] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [manualOpen, setManualOpen] = useState(false);
  const [interviewOpen, setInterviewOpen] = useState(false);
  const [interviewCandidate, setInterviewCandidate] = useState(null);
  const [interviewForm, setInterviewForm] = useState({ interviewDate:"", interviewTime:"", location:"", message:"" });
  const [finalOpen, setFinalOpen] = useState(false);
  const [finalCandidate, setFinalCandidate] = useState(null);
  const [finalForm, setFinalForm] = useState({
    finalMessage:"Congratulations! You have successfully cleared the interview and have been selected for the next joining formalities.",
    requiredDocuments:"Aadhaar card, PAN card, updated resume, educational certificates/marksheets, address proof, passport-size photographs, and previous employment documents if applicable.",
    reportingInstructions:"Please keep the original documents and one photocopy set ready for verification. HR will contact you regarding joining date and reporting details."
  });
  const [editingId, setEditingId] = useState(null);
  const [manualForm, setManualForm] = useState({ jobId:"", applicantName:"", email:"", phone:"", qualification:"", experienceYears:"", skills:"", currentCity:"", resumeUrl:"", coverLetter:"", status:"APPLIED", notes:"" });
  const role = roleOf();
  const isAdmin = role === "ADMIN";
  const isManager = role === "MANAGER";

  async function load() {
    try {
      setLoading(true); setError("");
      const [apps, jobRows] = await Promise.all([apiFetch("/applications"), apiFetch("/jobs")]);
      setApplications(Array.isArray(apps) ? apps : []);
      setJobs(Array.isArray(jobRows) ? jobRows : []);
    } catch (e) { setError(e.message || "Unable to load recruitment pipeline."); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return applications.filter((a) => {
      const matchesQ = !q || [a.applicantName,a.email,a.phone,a.job?.title,a.currentCity,a.skills,a.applicationReference].some((v)=>String(v||"").toLowerCase().includes(q));
      return matchesQ && (status === "ALL" || a.status === status) && (jobId === "ALL" || String(a.job?.id) === String(jobId));
    });
  }, [applications, search, status, jobId]);

  const counts = Object.fromEntries(STATUS_ORDER.map((s) => [s, applications.filter((a)=>a.status===s).length]));

  async function action(id, path, body, success) {
    try {
      setBusyId(id); setError(""); setMessage("");
      await apiFetch(`/applications/${id}/${path}`, { method: "PUT", body: JSON.stringify(body || {}) });
      setMessage(success); setSelected(null); await load();
    } catch (e) { setError(e.message || "Unable to update candidate."); }
    finally { setBusyId(null); }
  }

  const ask = (title, placeholder = "Optional note") => {
    const value = window.prompt(title, "");
    if (value === null) return null;
    return value || placeholder === null ? value : value;
  };

  const shortlist = (a) => {
    const note = ask("Manager shortlist note (optional)"); if (note === null) return;
    action(a.id, "manager-shortlist", { note }, "Candidate shortlisted by Manager and sent for Admin approval.");
  };
  const adminApprove = (a) => {
    const note = ask("Admin final approval note (optional)"); if (note === null) return;
    action(a.id, "admin-approve", { note }, "Candidate received final Admin shortlist approval.");
  };
  const adminDirectShortlist = (a) => {
    const note = ask("Admin direct shortlist note (optional)"); if (note === null) return;
    action(a.id, "admin-direct-shortlist", { note }, "Candidate directly shortlisted by Admin. Interview can now be scheduled.");
  };
  const openInterview = async (a) => {
    setInterviewCandidate(a);
    setInterviewForm({ interviewDate:"", interviewTime:"", location:a.job?.location || "", message:`Congratulations! Your application for ${a.job?.title || "this position"} has been shortlisted. Please attend the interview at the scheduled date and time.` });
    setInterviewOpen(true); setSelected(null); setError("");
    try {
      const rows = await apiFetch(`/interviews/application/${a.id}`);
      const latest = Array.isArray(rows) && rows.length ? rows[0] : null;
      if (latest && latest.status !== "CANCELLED") setInterviewForm({ interviewDate:latest.interviewDate || "", interviewTime:String(latest.interviewTime || "").slice(0,5), location:latest.location || a.job?.location || "", message:latest.message || "" });
    } catch { /* first interview has no existing record */ }
  };
  const submitInterview = async (e) => {
    e.preventDefault();
    if (!interviewCandidate || !interviewForm.interviewDate || !interviewForm.interviewTime || !interviewForm.location.trim()) { setError("Interview date, time and location are required."); return; }
    try {
      setBusyId(interviewCandidate.id); setError("");
      await apiFetch(`/interviews/application/${interviewCandidate.id}`, { method:"POST", body:JSON.stringify({ ...interviewForm, location:interviewForm.location.trim(), message:interviewForm.message.trim() }) });
      setMessage("Interview scheduled and candidate message sent successfully."); setInterviewOpen(false); setInterviewCandidate(null); await load();
    } catch(e2) { setError(e2.message || "Unable to schedule interview."); } finally { setBusyId(null); }
  };

  const openFinalApproval = (a) => {
    setFinalCandidate(a);
    setFinalForm({
      finalMessage:a.finalSelectionMessage || `Congratulations ${a.applicantName || ""}! You have successfully cleared the interview for ${a.job?.title || "the position"} and have been selected for the next joining formalities.`,
      requiredDocuments:a.requiredDocuments || "Aadhaar card, PAN card, updated resume, educational certificates/marksheets, address proof, passport-size photographs, and previous employment documents if applicable.",
      reportingInstructions:a.reportingInstructions || "Please keep the original documents and one photocopy set ready for verification. HR will contact you regarding joining date and reporting details."
    });
    setFinalOpen(true); setSelected(null); setError("");
  };

  const submitFinalApproval = async (e) => {
    e.preventDefault();
    if (!finalCandidate || !finalForm.finalMessage.trim() || !finalForm.requiredDocuments.trim()) {
      setError("Final message and required documents are required."); return;
    }
    try {
      setBusyId(finalCandidate.id); setError("");
      await apiFetch(`/applications/${finalCandidate.id}/final-approve`, {
        method:"PUT",
        body:JSON.stringify({
          finalMessage:finalForm.finalMessage.trim(),
          requiredDocuments:finalForm.requiredDocuments.trim(),
          reportingInstructions:finalForm.reportingInstructions.trim()
        })
      });
      setMessage("Candidate finally approved. Selection message and document requirements were sent successfully.");
      setFinalOpen(false); setFinalCandidate(null); await load();
    } catch(e2) { setError(e2.message || "Unable to complete final candidate approval."); }
    finally { setBusyId(null); }
  };
  const markInterviewCompleted = async (a) => {
    if (!window.confirm(`Mark ${a.applicantName}'s interview as completed?`)) return;
    try {
      setBusyId(a.id); setError(""); setMessage("");
      await apiFetch(`/interviews/application/${a.id}/complete`, { method:"PUT" });
      setMessage("Interview marked completed. Admin can now perform final approval and send document instructions.");
      await load();
    } catch(e2) { setError(e2.message || "Unable to mark interview completed."); }
    finally { setBusyId(null); }
  };

  const reject = (a) => {
    const reason = window.prompt("Reason for rejection (optional)", ""); if (reason === null) return;
    action(a.id, "reject", { reason }, "Candidate application rejected.");
  };

  const openManual = (a = null) => {
    setEditingId(a?.id || null);
    setManualForm({
      jobId: a?.job?.id ? String(a.job.id) : (jobs[0]?.id ? String(jobs[0].id) : ""),
      applicantName: a?.applicantName || "", email: a?.email || "", phone: a?.phone || "", qualification: a?.qualification || "",
      experienceYears: a?.experienceYears ?? "", skills: a?.skills || "", currentCity: a?.currentCity || "", resumeUrl: a?.resumeUrl || "",
      coverLetter: a?.coverLetter || "", status: a?.status || "APPLIED", notes: a?.notes || a?.recruiterNotes || ""
    });
    setManualOpen(true); setSelected(null); setError("");
  };

  const submitManual = async (e) => {
    e.preventDefault();
    if (!manualForm.jobId || !manualForm.applicantName.trim() || !manualForm.email.trim()) { setError("Job, applicant name and email are required."); return; }
    try {
      setBusyId(editingId || -1);
      const payload = {
        job:{id:Number(manualForm.jobId)}, applicantName:manualForm.applicantName.trim(), email:manualForm.email.trim(), phone:manualForm.phone.trim(),
        qualification:manualForm.qualification.trim(), experienceYears:manualForm.experienceYears === "" ? null : Number(manualForm.experienceYears), skills:manualForm.skills.trim(),
        currentCity:manualForm.currentCity.trim(), resumeUrl:manualForm.resumeUrl.trim(), coverLetter:manualForm.coverLetter.trim(), status:manualForm.status,
        appliedDate:new Date().toISOString().split("T")[0], notes:manualForm.notes.trim()
      };
      if (editingId) await apiFetch(`/applications/${editingId}`, {method:"PUT", body:JSON.stringify(payload)});
      else await apiFetch("/applications", {method:"POST", body:JSON.stringify(payload)});
      setMessage(editingId ? "Candidate application updated." : "Candidate added manually."); setManualOpen(false); setEditingId(null); await load();
    } catch(e2){ setError(e2.message || "Unable to save candidate."); } finally { setBusyId(null); }
  };

  const deleteApplication = async (a) => {
    if (!isAdmin || !window.confirm(`Delete application for ${a.applicantName}?`)) return;
    try { setBusyId(a.id); await apiFetch(`/applications/${a.id}`, {method:"DELETE"}); setSelected(null); setMessage("Application deleted."); await load(); }
    catch(e2){ setError(e2.message || "Unable to delete application."); } finally { setBusyId(null); }
  };

  return <div className="recruit-pipeline">
    <style>{`
      .recruit-pipeline{padding:24px;max-width:1400px;margin:auto;color:#0f172a}.rp-head{display:flex;justify-content:space-between;align-items:flex-start;gap:16px;margin-bottom:20px}.rp-head h1{margin:0;font-size:28px}.rp-head p{margin:7px 0 0;color:#64748b}.rp-link{border:1px solid #cbd5e1;background:#fff;border-radius:9px;padding:9px 12px;font-weight:700;cursor:pointer}.rp-alert{padding:12px 14px;border-radius:10px;margin-bottom:14px}.rp-ok{background:#f0fdf4;color:#166534;border:1px solid #bbf7d0}.rp-err{background:#fef2f2;color:#b91c1c;border:1px solid #fecaca}.rp-stats{display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:18px}.rp-stat{background:#fff;border:1px solid #e2e8f0;border-radius:13px;padding:15px}.rp-stat small{color:#64748b}.rp-stat strong{display:block;font-size:24px;margin-top:7px}.rp-toolbar{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:14px;display:grid;grid-template-columns:1fr 190px 220px auto;gap:10px;margin-bottom:16px}.rp-toolbar input,.rp-toolbar select{border:1px solid #dbe3ef;border-radius:9px;padding:10px}.rp-tablebox{background:#fff;border:1px solid #e2e8f0;border-radius:14px;overflow:hidden}.rp-table{width:100%;border-collapse:collapse}.rp-table th{padding:12px;text-align:left;font-size:11px;color:#64748b;background:#f8fafc;border-bottom:1px solid #e2e8f0}.rp-table td{padding:12px;border-bottom:1px solid #eef2f7;font-size:13px;vertical-align:top}.rp-person strong{display:block}.rp-person small{display:block;color:#94a3b8;margin-top:3px}.rp-badge{display:inline-block;padding:5px 8px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:10px;font-weight:800}.rp-actions{display:flex;gap:6px;flex-wrap:wrap}.rp-actions button{border:0;border-radius:8px;padding:7px 9px;font-weight:700;cursor:pointer;font-size:11px}.view{background:#f1f5f9}.screen{background:#e0f2fe;color:#075985}.short{background:#fef3c7;color:#92400e}.approve{background:#dcfce7;color:#166534}.interview{background:#ede9fe;color:#5b21b6}.reject{background:#fee2e2;color:#991b1b}.rp-empty{padding:36px;text-align:center;color:#64748b}.rp-modal-bg{position:fixed;inset:0;background:rgba(15,23,42,.45);display:grid;place-items:center;padding:20px;z-index:1000}.rp-modal{background:#fff;width:min(780px,96vw);max-height:88vh;overflow:auto;border-radius:16px;padding:22px}.rp-modal h2{margin-top:0}.rp-detail{display:grid;grid-template-columns:1fr 1fr;gap:12px}.rp-detail>div{padding:12px;border:1px solid #e2e8f0;border-radius:10px}.rp-detail small{display:block;color:#64748b;margin-bottom:4px}.rp-wide{grid-column:1/-1}.rp-close{float:right;border:0;background:#f1f5f9;border-radius:8px;padding:8px 10px;cursor:pointer}@media(max-width:1050px){.rp-stats{grid-template-columns:repeat(3,1fr)}.rp-toolbar{grid-template-columns:1fr 1fr}.rp-tablebox{overflow:auto}}@media(max-width:650px){.rp-stats{grid-template-columns:repeat(2,1fr)}.rp-toolbar{grid-template-columns:1fr}.rp-detail{grid-template-columns:1fr}.rp-wide{grid-column:auto}}
    `}</style>
    <div className="rp-head"><div><h1>Candidate Pipeline</h1><p>External applications, manual candidate entry, manager shortlist, Admin direct shortlist/final approval, and interview scheduling with candidate messages.</p></div><div style={{display:"flex",gap:8,flexWrap:"wrap"}}><button className="rp-link" onClick={()=>window.open("/careers","_blank")}>Open Public Application Form</button><button className="rp-link" style={{background:"#2563eb",color:"#fff",borderColor:"#2563eb"}} onClick={()=>openManual()}>+ Add candidate</button></div></div>
    {message && <div className="rp-alert rp-ok">{message}</div>}{error && <div className="rp-alert rp-err">{error}</div>}
    <div className="rp-stats"><div className="rp-stat"><small>Total</small><strong>{applications.length}</strong></div><div className="rp-stat"><small>New</small><strong>{counts.APPLIED||0}</strong></div><div className="rp-stat"><small>Screening</small><strong>{counts.SCREENING||0}</strong></div><div className="rp-stat"><small>Manager shortlisted</small><strong>{counts.MANAGER_SHORTLISTED||0}</strong></div><div className="rp-stat"><small>Admin approved</small><strong>{counts.ADMIN_APPROVED||0}</strong></div><div className="rp-stat"><small>Interview</small><strong>{(counts.INTERVIEW||0)+(counts.INTERVIEW_COMPLETED||0)}</strong></div></div>
    <div className="rp-toolbar"><input value={search} onChange={(e)=>setSearch(e.target.value)} placeholder="Search candidate, email, skills, reference..."/><select value={jobId} onChange={(e)=>setJobId(e.target.value)}><option value="ALL">All jobs</option>{jobs.map(j=><option key={j.id} value={j.id}>{j.title}</option>)}</select><select value={status} onChange={(e)=>setStatus(e.target.value)}><option value="ALL">All pipeline stages</option>{STATUS_ORDER.map(s=><option key={s} value={s}>{label(s)}</option>)}</select><button className="rp-link" onClick={load}>Refresh</button></div>
    <div className="rp-tablebox">{loading ? <div className="rp-empty">Loading candidates...</div> : filtered.length===0 ? <div className="rp-empty">No candidate applications match the current filters.</div> : <table className="rp-table"><thead><tr><th>Candidate</th><th>Job</th><th>Profile</th><th>Status</th><th>Workflow</th></tr></thead><tbody>{filtered.map(a=><tr key={a.id}><td className="rp-person"><strong>{a.applicantName}</strong><small>{a.email}</small><small>{a.applicationReference || `#${a.id}`}</small></td><td><strong>{a.job?.title || "-"}</strong><div style={{color:"#64748b",marginTop:3}}>{a.job?.department || ""}</div></td><td><div>{a.qualification || "Qualification not provided"}</div><small style={{color:"#64748b"}}>{a.experienceYears != null ? `${a.experienceYears} yrs` : "Experience not provided"} · {a.currentCity || "City not provided"}</small></td><td><span className="rp-badge">{label(a.status)}</span>{a.managerShortlistedBy && <div style={{fontSize:10,color:"#64748b",marginTop:5}}>Manager: @{a.managerShortlistedBy}</div>}{a.adminApprovedBy && <div style={{fontSize:10,color:"#64748b"}}>Admin: @{a.adminApprovedBy}</div>}</td><td><div className="rp-actions"><button className="view" onClick={()=>setSelected(a)}>View</button><button className="view" onClick={()=>openManual(a)}>Edit</button>{a.status==="APPLIED" && <button className="screen" disabled={busyId===a.id} onClick={()=>action(a.id,"screen",{},"Candidate moved to screening.")}>Screen</button>}{isManager && ["APPLIED","SCREENING"].includes(a.status) && <button className="short" disabled={busyId===a.id} onClick={()=>shortlist(a)}>Shortlist</button>}{isAdmin && ["APPLIED","SCREENING"].includes(a.status) && <button className="approve" disabled={busyId===a.id} onClick={()=>adminDirectShortlist(a)}>Direct shortlist</button>}{isAdmin && a.status==="MANAGER_SHORTLISTED" && <button className="approve" disabled={busyId===a.id} onClick={()=>adminApprove(a)}>Approve shortlist</button>}{(isAdmin || isManager) && ["ADMIN_APPROVED","INTERVIEW"].includes(a.status) && <button className="interview" disabled={busyId===a.id} onClick={()=>openInterview(a)}>{a.status==="INTERVIEW" ? "Edit interview" : "Schedule interview"}</button>}{(isAdmin || isManager) && a.status==="INTERVIEW" && <button className="short" disabled={busyId===a.id} onClick={()=>markInterviewCompleted(a)}>Mark interviewed</button>}{isAdmin && a.status==="INTERVIEW_COMPLETED" && <button className="approve" disabled={busyId===a.id} onClick={()=>openFinalApproval(a)}>Final approve</button>}{a.status!=="REJECTED" && a.status!=="HIRED" && <button className="reject" disabled={busyId===a.id} onClick={()=>reject(a)}>Reject</button>}</div></td></tr>)}</tbody></table>}</div>
    {selected && <div className="rp-modal-bg" onMouseDown={(e)=>{if(e.target===e.currentTarget)setSelected(null)}}><div className="rp-modal"><button className="rp-close" onClick={()=>setSelected(null)}>Close</button><h2>{selected.applicantName}</h2><p style={{color:"#64748b"}}>{selected.job?.title} · {selected.applicationReference || `#${selected.id}`}</p><div className="rp-detail"><div><small>Email</small><strong>{selected.email}</strong></div><div><small>Phone</small><strong>{selected.phone||"-"}</strong></div><div><small>Qualification</small><strong>{selected.qualification||"-"}</strong></div><div><small>Experience</small><strong>{selected.experienceYears != null ? `${selected.experienceYears} years` : "-"}</strong></div><div><small>Current city</small><strong>{selected.currentCity||"-"}</strong></div><div><small>Notice period</small><strong>{selected.noticePeriod||"-"}</strong></div><div className="rp-wide"><small>Skills</small><strong>{selected.skills||"-"}</strong></div><div className="rp-wide"><small>Cover letter</small><div>{selected.coverLetter||"-"}</div></div><div><small>Resume</small>{selected.resumeUrl ? <a href={selected.resumeUrl} target="_blank" rel="noreferrer">Open resume</a> : "-"}</div><div><small>LinkedIn / Portfolio</small>{selected.linkedinUrl && <a href={selected.linkedinUrl} target="_blank" rel="noreferrer">LinkedIn </a>}{selected.portfolioUrl && <a href={selected.portfolioUrl} target="_blank" rel="noreferrer">Portfolio</a>}</div><div className="rp-wide"><small>Recruiter notes</small><div style={{whiteSpace:"pre-wrap"}}>{selected.recruiterNotes||selected.notes||"-"}</div></div>{selected.finalSelectionMessage && <><div className="rp-wide"><small>Final selection message</small><div style={{whiteSpace:"pre-wrap"}}>{selected.finalSelectionMessage}</div></div><div className="rp-wide"><small>Required documents</small><div style={{whiteSpace:"pre-wrap"}}>{selected.requiredDocuments||"-"}</div></div><div className="rp-wide"><small>Reporting instructions</small><div style={{whiteSpace:"pre-wrap"}}>{selected.reportingInstructions||"-"}</div></div></>}</div>{isAdmin && <button className="rp-link" style={{marginTop:16,color:"#b91c1c"}} onClick={()=>deleteApplication(selected)}>Delete application</button>}</div></div>}
    {interviewOpen && interviewCandidate && <div className="rp-modal-bg" onMouseDown={(e)=>{if(e.target===e.currentTarget)setInterviewOpen(false)}}><div className="rp-modal"><button className="rp-close" onClick={()=>setInterviewOpen(false)}>Close</button><h2>Schedule Interview</h2><p style={{color:"#64748b"}}><strong>{interviewCandidate.applicantName}</strong> · {interviewCandidate.job?.title}</p><form onSubmit={submitInterview} className="rp-detail"><div><small>Interview date *</small><input type="date" required style={{width:"100%",boxSizing:"border-box",padding:9}} value={interviewForm.interviewDate} onChange={(e)=>setInterviewForm(f=>({...f,interviewDate:e.target.value}))}/></div><div><small>Interview time *</small><input type="time" required style={{width:"100%",boxSizing:"border-box",padding:9}} value={interviewForm.interviewTime} onChange={(e)=>setInterviewForm(f=>({...f,interviewTime:e.target.value}))}/></div><div className="rp-wide"><small>Job / interview location *</small><input required style={{width:"100%",boxSizing:"border-box",padding:9}} value={interviewForm.location} onChange={(e)=>setInterviewForm(f=>({...f,location:e.target.value}))} placeholder="Nashik Office / Online - Google Meet"/></div><div className="rp-wide"><small>Short message to candidate</small><textarea rows="4" style={{width:"100%",boxSizing:"border-box",padding:9}} value={interviewForm.message} onChange={(e)=>setInterviewForm(f=>({...f,message:e.target.value}))} placeholder="Interview instructions, documents to bring, reporting time..."/></div><div className="rp-wide"><button className="rp-link" style={{background:"#7c3aed",color:"#fff",borderColor:"#7c3aed"}} disabled={busyId===interviewCandidate.id}>{busyId===interviewCandidate.id ? "Sending..." : "Save & Send Interview Message"}</button></div></form></div></div>}
    {finalOpen && finalCandidate && <div className="rp-modal-bg" onMouseDown={(e)=>{if(e.target===e.currentTarget)setFinalOpen(false)}}><div className="rp-modal"><button className="rp-close" onClick={()=>setFinalOpen(false)}>Close</button><h2>Final Candidate Approval</h2><p style={{color:"#64748b"}}><strong>{finalCandidate.applicantName}</strong> · {finalCandidate.job?.title}</p><div className="rp-alert rp-ok">Use this only after the interview is completed. This sends the candidate their final selection message and required-document instructions.</div><form onSubmit={submitFinalApproval} className="rp-detail"><div className="rp-wide"><small>Final selection message *</small><textarea rows="4" required style={{width:"100%",boxSizing:"border-box",padding:9}} value={finalForm.finalMessage} onChange={(e)=>setFinalForm(f=>({...f,finalMessage:e.target.value}))}/></div><div className="rp-wide"><small>Required documents *</small><textarea rows="5" required style={{width:"100%",boxSizing:"border-box",padding:9}} value={finalForm.requiredDocuments} onChange={(e)=>setFinalForm(f=>({...f,requiredDocuments:e.target.value}))} placeholder="Aadhaar, PAN, degree certificates, marksheets..."/></div><div className="rp-wide"><small>Other / reporting instructions</small><textarea rows="4" style={{width:"100%",boxSizing:"border-box",padding:9}} value={finalForm.reportingInstructions} onChange={(e)=>setFinalForm(f=>({...f,reportingInstructions:e.target.value}))} placeholder="Document verification, reporting location, joining instructions..."/></div><div className="rp-wide"><button className="rp-link" style={{background:"#16a34a",color:"#fff",borderColor:"#16a34a"}} disabled={busyId===finalCandidate.id}>{busyId===finalCandidate.id ? "Sending..." : "Final Approve & Send Message"}</button></div></form></div></div>}
    {manualOpen && <div className="rp-modal-bg" onMouseDown={(e)=>{if(e.target===e.currentTarget)setManualOpen(false)}}><div className="rp-modal"><button className="rp-close" onClick={()=>setManualOpen(false)}>Close</button><h2>{editingId ? "Edit candidate" : "Add candidate manually"}</h2><form onSubmit={submitManual} className="rp-detail"><div className="rp-wide"><small>Job *</small><select style={{width:"100%",padding:10,border:"1px solid #dbe3ef",borderRadius:8}} value={manualForm.jobId} onChange={(e)=>setManualForm(f=>({...f,jobId:e.target.value}))}><option value="">Select job</option>{jobs.map(j=><option key={j.id} value={j.id}>{j.title}</option>)}</select></div><div><small>Applicant name *</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.applicantName} onChange={(e)=>setManualForm(f=>({...f,applicantName:e.target.value}))}/></div><div><small>Email *</small><input type="email" style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.email} onChange={(e)=>setManualForm(f=>({...f,email:e.target.value}))}/></div><div><small>Phone</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.phone} onChange={(e)=>setManualForm(f=>({...f,phone:e.target.value}))}/></div><div><small>Qualification</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.qualification} onChange={(e)=>setManualForm(f=>({...f,qualification:e.target.value}))}/></div><div><small>Experience years</small><input type="number" step="0.1" style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.experienceYears} onChange={(e)=>setManualForm(f=>({...f,experienceYears:e.target.value}))}/></div><div><small>Current city</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.currentCity} onChange={(e)=>setManualForm(f=>({...f,currentCity:e.target.value}))}/></div><div className="rp-wide"><small>Skills</small><textarea rows="2" style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.skills} onChange={(e)=>setManualForm(f=>({...f,skills:e.target.value}))}/></div><div className="rp-wide"><small>Resume URL</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.resumeUrl} onChange={(e)=>setManualForm(f=>({...f,resumeUrl:e.target.value}))}/></div><div className="rp-wide"><small>Cover letter</small><textarea rows="3" style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.coverLetter} onChange={(e)=>setManualForm(f=>({...f,coverLetter:e.target.value}))}/></div><div><small>Status</small><select style={{width:"100%",padding:9}} value={manualForm.status} onChange={(e)=>setManualForm(f=>({...f,status:e.target.value}))}>{STATUS_ORDER.map(s=><option key={s} value={s}>{label(s)}</option>)}</select></div><div><small>Recruiter notes</small><input style={{width:"100%",boxSizing:"border-box",padding:9}} value={manualForm.notes} onChange={(e)=>setManualForm(f=>({...f,notes:e.target.value}))}/></div><div className="rp-wide"><button className="rp-link" style={{background:"#2563eb",color:"#fff",borderColor:"#2563eb"}} disabled={busyId!==null}>{editingId ? "Save changes" : "Add candidate"}</button></div></form></div></div>}
  </div>;
}
