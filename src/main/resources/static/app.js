let alerts = [], notes = [], metrics = {}, activeAlert = null, currentUser = null;
const $ = selector => document.querySelector(selector);
const hasRole = role => currentUser?.roles?.includes(`ROLE_${role}`);
const hasL2Access = () => hasRole('ANALYST_L2') || hasRole('ADMIN');
const severityClass = severity => severity.toLowerCase();
const statusLabel = status => status === 'UNDER_INVESTIGATION' ? 'INVESTIGATING' : status;
const statusClass = status => statusLabel(status).toLowerCase();
const categoryLabel = category => (category || 'SECURITY').replaceAll('_', ' ');
const api = async (url, options = {}) => {
  const response = await fetch(url, {headers: {'Content-Type': 'application/json'}, ...options});
  if (!response.ok) throw new Error((await response.json().catch(() => ({}))).error || 'Request failed');
  return response.status === 204 ? null : response.json();
};
const displayTime = value => { const age = Math.max(0, Date.now() - new Date(value)); const mins = Math.round(age / 60000); return mins < 1 ? 'just now' : mins < 60 ? `${mins} min ago` : `${Math.round(mins / 60)} hr ago`; };

function renderMetrics() {
  $('#totalLogsMetric').textContent = metrics.totalLogs ?? 0;
  $('#totalMetric').textContent = metrics.totalAlerts ?? alerts.length;
  $('#criticalMetric').textContent = metrics.critical ?? 0;
  $('#highMetric').textContent = metrics.high ?? 0;
  $('#mediumMetric').textContent = metrics.medium ?? 0;
  $('#lowMetric').textContent = metrics.low ?? 0;
  $('#activeAlertsMetric').textContent = metrics.activeAlerts ?? alerts.filter(a => a.status !== 'RESOLVED').length;
  $('#investigatingStatusMetric').textContent = metrics.investigating ?? alerts.filter(a => statusLabel(a.status) === 'INVESTIGATING').length;
  $('#resolvedStatusMetric').textContent = metrics.resolved ?? alerts.filter(a => a.status === 'RESOLVED').length;
  $('#queueCount').textContent = alerts.filter(a => !['RESOLVED', 'CLOSED'].includes(statusLabel(a.status))).length;
  renderStatusCategoryGrid();
}

function renderStatusCategoryGrid() {
  const statuses = ['OPEN', 'INVESTIGATING', 'ESCALATED', 'RESOLVED', 'CLOSED'];
  const categories = ['AUTHENTICATION', 'AUTHORIZATION', 'APPLICATION', 'NETWORK', 'SYSTEM', 'SECURITY'];
  const counts = metrics.statusCategories || {};
  $('#statusCategoryGrid').innerHTML = statuses.map(status => `<article class="status-category-card"><div class="status-category-title"><h3>${status}</h3><span>${Object.values(counts[status] || {}).reduce((sum, value) => sum + value, 0)}</span></div>${categories.map(category => `<div class="category-count"><span>${categoryLabel(category)}</span><b>${counts[status]?.[category] ?? 0}</b></div>`).join('')}</article>`).join('');
}

function renderPriorityList() {
  const severityRank = {CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1};
  const statusRank = {OPEN: 2, INVESTIGATING: 2, UNDER_INVESTIGATION: 2, RESOLVED: 1, CLOSED: 0};
  $('#priorityList').innerHTML = alerts.filter(a => !['RESOLVED', 'CLOSED'].includes(statusLabel(a.status))).sort((a, b) => {
    const severityOrder = (severityRank[b.severity] || 0) - (severityRank[a.severity] || 0);
    const scoreOrder = (b.priorityScore || 0) - (a.priorityScore || 0);
    return severityOrder || scoreOrder || (statusRank[statusLabel(b.status)] || 0) - (statusRank[statusLabel(a.status)] || 0);
  }).map(a => `<div class="priority-item" data-alert="${a.id}"><span class="priority-line ${severityClass(a.severity)}"></span><div><div class="priority-name">${a.attackType.replaceAll('_', ' ')} · ${a.title}</div><div class="priority-meta">${categoryLabel(a.category)} · ${a.sourceIP || 'N/A'} · ${new Date(a.createdAt).toLocaleString()} · Risk ${a.priorityScore ?? 0} · ${statusLabel(a.status)}</div></div><span class="severity ${severityClass(a.severity)}">${a.severity}</span></div>`).join('') || '<div class="ai-placeholder">No active alerts.</div>';
}

function renderRecentList() {
  const sorted = [...alerts].sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0,4);
  $('#recentList').innerHTML = sorted.map(a => `<div class="priority-item" data-alert="${a.id}"><span class="priority-line ${severityClass(a.severity)}"></span><div><div class="priority-name">${a.title}</div><div class="priority-meta">${a.referenceCode} · ${a.sourceIP || 'N/A'} · ${displayTime(a.createdAt)}</div></div><span class="severity ${severityClass(a.severity)}">${a.severity}</span></div>`).join('') || '<div class="ai-placeholder">No alerts yet.</div>';
}

function renderEscalatedAlerts() {
  const escalated = alerts.filter(alert => alert.status === 'ESCALATED' || alert.status === 'UNDER_INVESTIGATION');
  $('#escalatedAlertList').innerHTML = escalated.map(alert => `<div class="priority-item" data-alert="${alert.id}"><span class="priority-line ${severityClass(alert.severity)}"></span><div><div class="priority-name">${alert.referenceCode} · ${alert.title}</div><div class="priority-meta">${alert.severity} · Assigned to analyst2 (ANALYST_L2) · ${statusLabel(alert.status)}</div></div><span class="severity ${severityClass(alert.severity)}">${alert.severity}</span></div>`).join('') || '<p class="ai-placeholder">No escalated alerts.</p>';
}

function renderAttackDistribution() {
  const counts = {};
  alerts.forEach(a => {
    counts[a.attackType] = (counts[a.attackType] || 0) + 1;
  });
  const sorted = Object.entries(counts).sort((a,b) => b[1] - a[1]);
  const total = alerts.length || 1;
  $('#attackDistribution').innerHTML = sorted.map(([type, count]) => {
    const pct = Math.round((count / total) * 100);
    return `<div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-size: 12px; border-bottom: 1px solid var(--line); padding-bottom: 6px;">
      <span style="font-weight: 600; width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${type.replaceAll('_', ' ')}</span>
      <div style="flex: 1; background: #edf2f2; height: 6px; border-radius: 3px; overflow: hidden; margin: 0 15px;">
        <div style="background: var(--teal); height: 100%; width: ${pct}%;"></div>
      </div>
      <span style="font-family: 'DM Mono', monospace; width: 80px; text-align: right; color: var(--muted);">${count} (${pct}%)</span>
    </div>`;
  }).join('') || '<div class="ai-placeholder">No alerts yet.</div>';
}

function renderTable() {
  const filter = document.querySelector('.filter.active')?.dataset.filter || 'all'; const query = $('#alertSearch').value.toLowerCase();
  const visible = alerts.filter(a => {
    const matchFilter = filter === 'all' || 
                        (filter === 'critical' && a.priority === 'P1') ||
                        (filter === 'high' && a.priority === 'P2') ||
                        (filter === 'medium' && a.priority === 'P3');
    const matchQuery = `${a.title} ${a.asset} ${a.referenceCode} ${a.sourceIP || ''} ${a.username || ''}`.toLowerCase().includes(query);
    return matchFilter && matchQuery;
  });
  $('#alertTable').innerHTML = visible.map(a => `<tr data-alert="${a.id}"><td><span>${a.title}</span><small>${a.referenceCode} · ${displayTime(a.createdAt)}</small></td><td>${a.attackType.replaceAll('_', ' ')}</td><td>${categoryLabel(a.category)}</td><td><span class="severity ${severityClass(a.severity)}">${a.severity}</span></td><td>${a.asset}</td><td><span class="badge ${a.priority.toLowerCase()}">${a.priority} · ${a.priorityScore ?? 0}</span></td><td><span class="status-pill ${statusClass(a.status)}">${statusLabel(a.status)}</span></td><td><button class="row-button">View →</button></td></tr>`).join('') || '<tr><td colspan="8">No alerts match the selected filter.</td></tr>';
}

function showDetail(id) {
  activeAlert = alerts.find(a => a.id === Number(id)) || activeAlert; if (!activeAlert) return; const a = activeAlert;
  const escalationAction = hasRole('ANALYST_L1')
    ? '<button class="button button-primary" id="escalateButton" style="width: 100%; margin-bottom: 8px;">Escalate to L2</button>'
    : '';
  const statusOptions = hasL2Access()
    ? '<option value="OPEN" ' + (a.status === 'OPEN' ? 'selected' : '') + '>OPEN</option><option value="INVESTIGATING" ' + (['INVESTIGATING', 'UNDER_INVESTIGATION'].includes(a.status) ? 'selected' : '') + '>INVESTIGATING</option><option value="RESOLVED" ' + (a.status === 'RESOLVED' ? 'selected' : '') + '>RESOLVED</option><option value="CLOSED" ' + (a.status === 'CLOSED' ? 'selected' : '') + '>CLOSED</option>'
    : '<option value="OPEN" ' + (a.status === 'OPEN' ? 'selected' : '') + '>OPEN</option><option value="INVESTIGATING" ' + (['INVESTIGATING', 'UNDER_INVESTIGATION'].includes(a.status) ? 'selected' : '') + '>INVESTIGATING</option>';
  $('#alertDetail').innerHTML = `
    <p class="eyebrow">${a.referenceCode} · ${new Date(a.createdAt).toLocaleString().toUpperCase()}</p>
    <div style="display: flex; gap: 8px; align-items: center; margin-bottom: 12px;">
      <span class="severity ${severityClass(a.severity)}">${a.severity}</span>
      <span class="badge ${a.priority.toLowerCase()}" style="background: var(--navy); color: #fff; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: bold;">${a.priority}</span>
    </div>
    <h2 class="detail-title">${a.title}</h2>
    
    <div class="detail-meta" style="grid-template-columns: 1fr 1fr; gap: 10px; margin: 15px 0; display: grid;">
      <div style="padding: 8px; background: #f7f9f9; border-radius: 5px;"><span>ATTACK TYPE</span><b style="font-size: 11px;">${a.attackType.replaceAll('_',' ')}</b></div>
      <div style="padding: 8px; background: #f7f9f9; border-radius: 5px;"><span>CATEGORY</span><b style="font-size: 11px;">${categoryLabel(a.category)}</b></div>
      <div style="padding: 8px; background: #f7f9f9; border-radius: 5px;"><span>SOURCE IP</span><b>${a.sourceIP || 'N/A'}</b></div>
      <div style="padding: 8px; background: #f7f9f9; border-radius: 5px;"><span>USERNAME</span><b>${a.username || 'N/A'}</b></div>
      <div style="padding: 8px; background: #f7f9f9; border-radius: 5px;"><span>AFFECTED ASSET</span><b>${a.asset}</b></div>
    </div>
    
    <div class="detail-copy" style="margin-bottom: 10px;">
      <strong>Description</strong>
      <p style="margin-top: 4px;">${a.description}</p>
    </div>
    
    <div class="detail-copy" style="margin-bottom: 10px;">
      <strong>Recommendation</strong>
      <p style="margin-top: 4px; color: var(--teal); font-weight: 600;">${a.recommendation || 'No recommendation available.'}</p>
    </div>
    
    <div class="detail-copy" style="margin-bottom: 15px;">
      <strong>Matched Log Entry</strong>
      <pre style="background: #f7f9f9; padding: 8px; border-radius: 4px; font-size: 11px; max-height: 100px; overflow: auto; margin-top: 4px; font-family: 'DM Mono', monospace; white-space: pre-wrap;">${a.matchedLogEntry || a.logDetails || 'No log details provided.'}</pre>
    </div>
    
    <div class="investigation-form-card" style="border-top: 1px solid var(--line); padding-top: 15px;">
      <strong style="font-size: 12px; display: block; margin-bottom: 10px;">Triage & Investigation Case File</strong>
      
      <label for="invStatus" style="font-size: 11px; font-weight: bold; color: var(--muted); display: block; margin-bottom: 4px;">CASE STATUS</label>
      <select id="invStatus" style="width: 100%; padding: 8px; border: 1px solid #dce4e6; border-radius: 5px; background: #fff; font: 12px Manrope; margin-bottom: 10px;">
        ${statusOptions}
      </select>
      
      <label for="invNote" style="font-size: 11px; font-weight: bold; color: var(--muted); display: block; margin-bottom: 4px;">ADD INVESTIGATION NOTE</label>
      <textarea id="invNote" placeholder="Investigation notes..." style="width: 100%; height: 60px; padding: 8px; border: 1px solid #dce4e6; border-radius: 5px; font: 12px Manrope; resize: none; margin-bottom: 10px;"></textarea>
      <textarea id="invActions" placeholder="Actions performed..." style="width: 100%; height: 48px; padding: 8px; border: 1px solid #dce4e6; border-radius: 5px; font: 12px Manrope; resize: none; margin-bottom: 10px;"></textarea>
      <textarea id="invFindings" placeholder="Findings..." style="width: 100%; height: 48px; padding: 8px; border: 1px solid #dce4e6; border-radius: 5px; font: 12px Manrope; resize: none; margin-bottom: 10px;"></textarea>
      <textarea id="invConclusion" placeholder="Conclusion..." style="width: 100%; height: 48px; padding: 8px; border: 1px solid #dce4e6; border-radius: 5px; font: 12px Manrope; resize: none; margin-bottom: 10px;"></textarea>
      
      ${escalationAction}
      <button class="button button-primary" id="saveInvestigationBtn" style="width: 100%;">Save Investigation</button>
    </div>
    <div class="detail-copy" style="border-top: 1px solid var(--line); margin-top: 15px; padding-top: 15px;">
      <strong>Investigation timeline</strong>
      <div id="investigationTimeline" class="notes-list"><p class="ai-placeholder">Loading history...</p></div>
    </div>
  `;
  loadInvestigationTimeline(a.id);
}

async function loadInvestigationTimeline(alertId) {
  try {
    const history = await api(`/api/alerts/${alertId}/timeline`);
    $('#investigationTimeline').innerHTML = history.map(event => `
      <article class="note"><small>${new Date(event.occurredAt).toLocaleString()} · ${event.eventType} · ${event.actorUsername} (${event.actorRole || 'UNKNOWN'})</small>
      <p>${event.reason || event.notes || event.findings || event.conclusion || (event.assignedTo ? `Assigned to ${event.assignedTo} (${event.assignedRole})` : `${event.oldStatus || ''} → ${event.newStatus || ''}`)}</p>
      ${event.actionsPerformed ? `<small>Actions: ${event.actionsPerformed}</small>` : ''}
      ${event.finalDecision ? `<small>Decision: ${event.finalDecision}</small>` : ''}</article>
    `).join('') || '<p class="ai-placeholder">No investigation history yet.</p>';
  } catch (error) {
    $('#investigationTimeline').innerHTML = '<p class="ai-placeholder">Investigation history unavailable.</p>';
  }
}

function renderNotes() { $('#notesList').innerHTML = notes.length ? notes.map(n => `<article class="note"><small>${new Date(n.createdAt).toLocaleString()} · ${n.referenceCode} · ${n.alertTitle}</small><p>${n.text}</p></article>`).join('') : '<p class="ai-placeholder">No notes have been saved yet.</p>'; }
function fillSelects() { const options = alerts.map(a => `<option value="${a.id}">${a.referenceCode} — ${a.title}</option>`).join(''); $('#noteAlert').innerHTML = options; $('#aiAlert').innerHTML = options; }

function switchView(id) { 
  document.querySelectorAll('.view').forEach(v => v.classList.toggle('active-view',v.id===id));
  document.querySelectorAll('.nav-link').forEach(a=>a.classList.toggle('active',a.dataset.view===id));
  const labels={
    dashboard:['OVERVIEW','Security overview'],
    alerts:['ALERT MANAGEMENT','Alert queue'],
    upload:['LOG UPLOAD','Log analysis portal'],
    investigations:['INVESTIGATION NOTES','Investigations'],
    reports:['REPORTING','Incident reports'],
    'login-audit':['SECURITY AUDIT','Login activity']
  };
  $('#pageKicker').textContent=labels[id][0];
  $('#pageTitle').textContent=labels[id][1];
  if(id==='alerts')renderTable();
  window.scrollTo({top:0,behavior:'smooth'}); 
}

const formatAuditDate = value => value ? new Date(value).toLocaleDateString() : '—';
const formatAuditTime = value => value ? new Date(value).toLocaleTimeString() : '—';
const formatSessionDuration = value => value == null ? '—' : `${value}s`;
const escapeAuditValue = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));

async function loadLoginActivity() {
  if (!currentUser?.roles?.includes('ROLE_ADMIN')) return;
  const audit = await api('/api/admin/login-activity');
  $('#loginCountList').innerHTML = Object.entries(audit.counts).map(([username, count]) => `
    <article class="metric-card"><p>${username}</p><strong>${count.attempts}</strong><span>${count.successful} successful login${count.successful === 1 ? '' : 's'}</span></article>
  `).join('');
  $('#loginActivityTable').innerHTML = audit.activities.map(activity => `
    <tr>
      <td>${escapeAuditValue(activity.username)}</td><td>${escapeAuditValue(activity.role || 'Unknown')}</td>
      <td>${formatAuditDate(activity.loginAt)}</td><td>${formatAuditTime(activity.loginAt)}</td>
      <td>${formatAuditTime(activity.logoutAt)}</td><td>${escapeAuditValue(activity.status)}</td>
      <td>${escapeAuditValue(activity.ipAddress || 'Unavailable')}</td><td>${formatSessionDuration(activity.sessionDurationSeconds)}</td>
    </tr>
  `).join('') || '<tr><td colspan="8" style="text-align: center; padding: 20px;">No login activity recorded.</td></tr>';
}

function renderReportStats() {
  $('#repTotal').textContent = alerts.length;
  const resolved = alerts.filter(a => a.status === 'RESOLVED').length;
  $('#repResolved').textContent = resolved;
  $('#repPending').textContent = alerts.length - resolved;
  
  // Priority breakdown
  const priorities = { P1: 0, P2: 0, P3: 0, P4: 0 };
  alerts.forEach(a => {
    if (priorities[a.priority] !== undefined) priorities[a.priority]++;
  });
  $('#repPriorityList').innerHTML = Object.entries(priorities).map(([p, count]) => `
    <div style="display:flex; justify-content:space-between; margin-bottom:6px; font-size:12px; border-bottom:1px solid #f0f0f0; padding-bottom:4px;">
      <span>Priority ${p}</span><b>${count} alerts</b>
    </div>
  `).join('');
  
  // Attack breakdown
  const attacks = {};
  alerts.forEach(a => {
    attacks[a.attackType] = (attacks[a.attackType] || 0) + 1;
  });
  $('#repAttackList').innerHTML = Object.entries(attacks).map(([type, count]) => `
    <div style="display:flex; justify-content:space-between; margin-bottom:6px; font-size:12px; border-bottom:1px solid #f0f0f0; padding-bottom:4px;">
      <span>${type.replaceAll('_', ' ')}</span><b>${count}</b>
    </div>
  `).join('') || '<div class="ai-placeholder">No alerts yet.</div>';
}

async function loadUploadHistory() {
  try {
    const history = await api('/api/logs/uploads');
    $('#uploadHistoryTable').innerHTML = history.map(h => `
      <tr>
        <td><b>${h.fileName}</b></td>
        <td>${new Date(h.uploadTime).toLocaleString()}</td>
        <td>${(h.fileSize / 1024).toFixed(2)} KB</td>
        <td>${h.totalLines} lines</td>
        <td><span class="severity ${h.alertCount > 0 ? 'critical' : 'low'}" style="padding: 2px 6px; border-radius: 4px; font-size:11px; font-weight:bold;">${h.alertCount} alerts</span></td>
        <td><span class="status-pill resolved">PROCESSED</span></td>
      </tr>
    `).join('') || '<tr><td colspan="6" style="text-align: center; padding: 20px;">No files uploaded yet.</td></tr>';
  } catch (err) {
    console.error(err);
  }
}

async function refresh() { 
  currentUser = await api('/api/user'); 
  $('#userName').textContent = currentUser.username; 
  $('#userRole').textContent = currentUser.roles.join(', ').replace('ROLE_',''); 
  $('#userAvatar').textContent = currentUser.username.slice(0,2).toUpperCase(); 
  document.querySelectorAll('.admin-only').forEach(element => element.hidden = !currentUser.roles.includes('ROLE_ADMIN'));
  document.querySelectorAll('.l2-plus').forEach(element => element.hidden = !hasL2Access());
  [alerts, notes, metrics] = await Promise.all([api('/api/alerts'),api('/api/notes'),api('/api/metrics')]);
  renderMetrics();
  renderPriorityList();
  renderRecentList();
  renderEscalatedAlerts();
  renderAttackDistribution();
  renderTable();
  renderNotes();
  fillSelects();
  renderReportStats();
  await loadUploadHistory();
  await loadLoginActivity();
  showDetail(activeAlert?.id || alerts[0]?.id); 
}

async function updateStatus(status) { 
  await api(`/api/alerts/${activeAlert.id}/status`, {method:'PATCH',body:JSON.stringify({status})}); 
  await refresh(); 
}

async function showAiAnalysis() { 
  const id = $('#aiAlert').value; 
  const result = await api(`/api/alerts/${id}/ai-analysis`,{method:'POST'}); 
  $('#aiResult').innerHTML=`<p><strong>AI triage — ${result.confidence} confidence</strong></p><ul><li><b>Likely concern:</b> ${result.concern}</li><li><b>Recommended next step:</b> ${result.recommendedNextStep}</li><li><b>Analyst reminder:</b> ${result.analystReminder}</li></ul>`; 
}

function exportReport() { 
  const open=alerts.filter(a=>!['RESOLVED','CLOSED'].includes(statusLabel(a.status)));
  const text=`SOC SENTINEL — OPEN INCIDENT REPORT\nGenerated: ${new Date().toLocaleString()}\n\n${open.map(a=>`${a.referenceCode} | ${a.priority} | ${a.severity} | ${a.title}\nAsset: ${a.asset}\nSource IP: ${a.sourceIP || 'N/A'} | User: ${a.username || 'N/A'}\nStatus: ${a.status}\nSummary: ${a.description}\nRecommendation: ${a.recommendation}\n`).join('\n')}\nINVESTIGATION NOTES\n${notes.map(n=>`[${new Date(n.createdAt).toLocaleString()}] ${n.referenceCode}: ${n.text}`).join('\n')}`; 
  const link=document.createElement('a');
  link.href=URL.createObjectURL(new Blob([text],{type:'text/plain'}));
  link.download='soc-sentinel-open-incidents.txt';
  link.click();
  URL.revokeObjectURL(link.href);
}

function notify(message) { 
  const existing=$('.notice p'); 
  if(existing) existing.innerHTML=`<strong>${message}</strong>`; 
}

// File uploading functionality
async function handleFile(file) {
  if (!file) return;
  const formData = new FormData();
  formData.append('file', file);
  
  $('#uploadProgress').style.display = 'block';
  $('#progressBar').style.width = '20%';
  
  try {
    const response = await fetch('/api/logs/upload', {
      method: 'POST',
      body: formData
    });
    
    $('#progressBar').style.width = '70%';
    
    if (!response.ok) {
      const errData = await response.json();
      throw new Error(errData.error || 'Upload failed');
    }
    
    const result = await response.json();
    $('#progressBar').style.width = '100%';
    
    setTimeout(async () => {
      $('#uploadProgress').style.display = 'none';
      $('#progressBar').style.width = '0%';
      notify(`Successfully parsed log file. Generated ${result.alertCount} alerts.`);
      await refresh();
      switchView('alerts');
    }, 500);
    
  } catch (err) {
    $('#uploadProgress').style.display = 'none';
    $('#progressBar').style.width = '0%';
    alert('Log Analysis Error: ' + err.message);
  }
}

document.addEventListener('click', async e => { 
  try { 
    const view=e.target.closest('[data-view]');
    if(view){
      e.preventDefault();
      switchView(view.dataset.view);
      return;
    }
    const item=e.target.closest('[data-alert]');
    if(item){
      showDetail(item.dataset.alert);
      return;
    }
    if(e.target.id==='newAlertButton')$('#alertDialog').showModal();
    if(e.target.id==='investigateButton'){
      await updateStatus('INVESTIGATING');
      switchView('investigations');
      $('#noteAlert').value=activeAlert.id;
      $('#noteText').focus();
    }
    if(e.target.id==='resolveButton') await updateStatus('RESOLVED');
    if(e.target.id==='analyzeButton') await showAiAnalysis();
    if(e.target.id==='escalateButton') {
      const reason = window.prompt('Reason for escalation to L2:');
      if (!reason?.trim()) return;
      await api(`/api/alerts/${activeAlert.id}/escalate`, {method:'POST', body: JSON.stringify({reason: reason.trim()})});
      await refresh();
      notify('Alert escalated to Analyst L2.');
    }
    if(e.target.id==='exportButton') exportReport(); 
    if(e.target.id==='browseButton') $('#fileInput').click();
    
    if (e.target.id === 'saveInvestigationBtn') {
      const status = $('#invStatus').value;
      const noteText = $('#invNote').value.trim();
      const actionsPerformed = $('#invActions').value.trim();
      const findings = $('#invFindings').value.trim();
      const conclusion = $('#invConclusion').value.trim();
      
      await api(`/api/alerts/${activeAlert.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status })
      });
      
      if (noteText) {
        await api('/api/notes', {
          method: 'POST',
          body: JSON.stringify({
            alertId: activeAlert.id,
            text: noteText
          })
        });
      }

      await api(`/api/alerts/${activeAlert.id}/investigations`, {
        method: 'POST',
        body: JSON.stringify({notes: noteText || 'Investigation saved.', actionsPerformed: actionsPerformed || 'No separate actions recorded.', findings: findings || 'No separate findings recorded.', conclusion: conclusion || 'No separate conclusion recorded.', finalDecision: status})
      });
      
      notify('Triage & investigation saved successfully.');
      await refresh();
    }
  } catch(error) { 
    alert(error.message); 
  } 
});

// Drag and drop event listeners
document.addEventListener('DOMContentLoaded', () => {
  const dropZone = $('#dropZone');
  if (dropZone) {
    dropZone.addEventListener('dragover', e => {
      e.preventDefault();
      dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', () => {
      dropZone.classList.remove('drag-over');
    });
    dropZone.addEventListener('drop', e => {
      e.preventDefault();
      dropZone.classList.remove('drag-over');
      const files = e.dataTransfer.files;
      if (files.length > 0) {
        handleFile(files[0]);
      }
    });
  }
  
  const fileInput = $('#fileInput');
  if (fileInput) {
    fileInput.addEventListener('change', () => {
      if (fileInput.files.length > 0) {
        handleFile(fileInput.files[0]);
      }
    });
  }
});

document.querySelectorAll('.filter').forEach(button=>button.addEventListener('click',()=>{document.querySelectorAll('.filter').forEach(x=>x.classList.remove('active'));button.classList.add('active');renderTable();}));
$('#alertSearch').addEventListener('input',renderTable);
$('#noteForm').addEventListener('submit',async e=>{e.preventDefault();try{await api('/api/notes',{method:'POST',body:JSON.stringify({alertId:Number($('#noteAlert').value),text:$('#noteText').value.trim()})});$('#noteText').value='';await refresh();notify('Investigation note saved.');}catch(error){alert(error.message);}});
$('#alertForm').addEventListener('submit',async e=>{if(e.submitter?.value==='cancel')return;e.preventDefault();try{const data=new FormData(e.target), severity=data.get('severity').toUpperCase(),score={CRITICAL:95,HIGH:78,MEDIUM:58,LOW:31}[severity];const created=await api('/api/alerts',{method:'POST',body:JSON.stringify({title:data.get('title'),asset:data.get('asset'),description:data.get('description'),severity,priorityScore:score,alertType:data.get('alertType'),attackType:data.get('attackType'),logDetails:data.get('logDetails')})});$('#alertDialog').close();e.target.reset();activeAlert=created;await refresh();switchView('alerts');notify('New alert saved.');}catch(error){alert(error.message);}});
refresh().catch(error=>{console.error(error);alert('Cannot load the database. Start MySQL through XAMPP and run the Spring Boot application first.');});
