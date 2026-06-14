/* ═══════════════════════════════════════════
   EDUEVENT — core.js
   JWT auth, API interceptor, error handler,
   toast system, shared utilities
═══════════════════════════════════════════ */

const BASE_URL = '/api'; // Spring Boot serves from root

/* ── Auth ──────────────────────────────────── */
const Auth = {
  getToken()  { return localStorage.getItem('jwt'); },
  setToken(t) { localStorage.setItem('jwt', t); },
  getUser()   { try { return JSON.parse(localStorage.getItem('edu_user') || 'null'); } catch { return null; } },
  setUser(u)  { localStorage.setItem('edu_user', JSON.stringify(u)); },
  clear()     { localStorage.removeItem('jwt'); localStorage.removeItem('edu_user'); },
  isLoggedIn(){ return !!this.getToken(); },
  getRole()   { return this.getUser()?.role || null; },
  logout()    { this.clear(); window.location.href = '/index.html'; },
  redirectByRole() {
    const role = this.getRole();
    if (role === 'ADMIN')  window.location.href = '/pages/admin/dashboard.html';
    else if (role === 'JUDGE') window.location.href = '/pages/judge/dashboard.html';
    else window.location.href = '/pages/user/dashboard.html';
  },
  guardPage(expectedRole) {
    if (!this.isLoggedIn()) { window.location.href = '/index.html'; return false; }
    if (expectedRole && this.getRole() !== expectedRole) { this.redirectByRole(); return false; }
    return true;
  }
};

/* ── API Fetch Interceptor ─────────────────── */
/* ── API Fetch Interceptor ─────────────────── */
async function apiFetch(endpoint, options = {}) {
  const token = Auth.getToken();
  const isFormData = options.body instanceof FormData;

  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  try {
    const res = await fetch(`${BASE_URL}${endpoint}`, { ...options, headers });

    // FIX: 401 means invalid/expired token -> Clear auth and redirect
    if (res.status === 401) {
      Auth.clear();
      window.location.href = '/index.html';
      return null;
    }

    // FIX: 403 means logged in, but wrong role -> Show toast, DO NOT redirect
    if (res.status === 403) {
      Toast.show('Access denied on this resource.', 'error');
      return null;
    }

    // Handle PDF/binary download
    if (options._blobResponse) {
      if (!res.ok) { Toast.show('Download failed.', 'error'); return null; }
      return await res.blob();
    }

    const isJson = res.headers.get('content-type')?.includes('application/json');
    const data = isJson ? await res.json() : null;

    if (!res.ok) {
      const msg = data?.message || `Server error (${res.status})`;
      Toast.show(msg, res.status === 409 ? 'warning' : 'error');
      return null;
    }

    if (res.status === 204) return { success: true };
    return data;
  } catch (err) {
    Toast.show('Cannot reach the server. Check your connection.', 'error', 'Network Error');
    return null;
  }
}

/* Convenience wrappers */
const api = {
  get:      (url)        => apiFetch(url),
  post:     (url, data)  => apiFetch(url, { method: 'POST', body: JSON.stringify(data) }),
  put:      (url, data)  => apiFetch(url, { method: 'PUT',  body: JSON.stringify(data) }),
  delete:   (url)        => apiFetch(url, { method: 'DELETE' }),
  upload:   (url, fd)    => apiFetch(url, { method: 'POST', body: fd }),
  download: (url)        => apiFetch(url, { _blobResponse: true }),
};

/* ── Cache ─────────────────────────────────── */
const _cache = {};
async function cachedGet(url, ttlMs = 60000) {
  const now = Date.now();
  if (_cache[url] && (now - _cache[url].ts) < ttlMs) return _cache[url].data;
  const data = await api.get(url);
  if (data) _cache[url] = { data, ts: now };
  return data;
}

/* ── Toast ─────────────────────────────────── */
const Toast = {
  show(message, type = 'info', title = null) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }
    const icons = { success:'ti-circle-check', error:'ti-circle-x', warning:'ti-alert-triangle', info:'ti-info-circle' };
    const titles = { success:'Success', error:'Error', warning:'Warning', info:'Info' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <i class="ti ${icons[type]} toast-icon"></i>
      <div class="toast-body">
        <div class="toast-title">${title || titles[type]}</div>
        <div class="toast-msg">${message}</div>
      </div>
      <button onclick="this.closest('.toast').remove()" style="background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:14px;margin-top:1px;padding:0;"><i class="ti ti-x"></i></button>`;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.animation = 'toastOut 0.3s ease forwards';
      setTimeout(() => toast.remove(), 300);
    }, 4500);
  }
};

/* ── Confirm Dialog ────────────────────────── */
function confirmAction(message, onConfirm, danger = true) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay open';
  overlay.innerHTML = `
    <div class="modal confirm-dialog">
      <div style="text-align:center;padding:8px 0 20px;">
        <div style="width:48px;height:48px;border-radius:50%;background:rgba(239,68,68,0.12);display:flex;align-items:center;justify-content:center;margin:0 auto 14px;">
          <i class="ti ti-alert-triangle" style="color:#F87171;font-size:22px;"></i>
        </div>
        <div style="font-family:var(--font-display);font-size:16px;font-weight:700;margin-bottom:8px;">Confirm Action</div>
        <div style="font-size:13px;color:var(--text-secondary);">${message}</div>
      </div>
      <div style="display:flex;gap:10px;justify-content:center;">
        <button class="btn btn-secondary" id="conf-cancel">Cancel</button>
        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" id="conf-ok">Confirm</button>
      </div>
    </div>`;
  document.body.appendChild(overlay);
  overlay.querySelector('#conf-cancel').onclick = () => overlay.remove();
  overlay.querySelector('#conf-ok').onclick = () => { overlay.remove(); onConfirm(); };
  overlay.onclick = e => { if (e.target === overlay) overlay.remove(); };
}

/* ── Format Helpers ─────────────────────────── */
const fmt = {
  date(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-US', { year:'numeric', month:'short', day:'numeric' });
  },
  datetime(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-US', { year:'numeric', month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' });
  },
  number(n)  { return Number(n || 0).toLocaleString(); },
  percent(n) { return `${Math.round(n || 0)}%`; },
  score(n)   { return Number(n || 0).toFixed(1); },
  initials(name) { return (name||'U').split(' ').map(n=>n[0]).join('').slice(0,2).toUpperCase(); },
  avatarColor(name) {
    const colors = ['#4F46E5','#14B8A6','#D946EF','#F59E0B','#3B82F6','#22C55E','#EF4444'];
    return colors[(name||'A').charCodeAt(0) % colors.length];
  }
};

/* ── Loading State ──────────────────────────── */
function setLoading(el, isLoading) {
  if (!el) return;
  if (isLoading) {
    el.disabled = true;
    el.dataset.orig = el.innerHTML;
    el.innerHTML = `<i class="ti ti-loader spin"></i> Loading…`;
  } else {
    el.disabled = false;
    if (el.dataset.orig) el.innerHTML = el.dataset.orig;
  }
}

/* ── Skeleton loader helpers ─────────────────── */
function skeletonRows(n, cols) {
  return Array(n).fill(0).map(() =>
    `<tr>${Array(cols).fill('<td><div class="skeleton skeleton-text full"></div></td>').join('')}</tr>`
  ).join('');
}
function skeletonCards(n) {
  return Array(n).fill('<div class="skeleton skeleton-card"></div>').join('');
}

/* ── Populate profile in UI ─────────────────── */
function populateProfile() {
  const user = Auth.getUser();
  if (!user) return;
  document.querySelectorAll('[data-profile-name]').forEach(el  => el.textContent = user.name || 'User');
  document.querySelectorAll('[data-profile-email]').forEach(el => el.textContent = user.email || '');
  document.querySelectorAll('[data-profile-role]').forEach(el  => el.textContent = user.role || '');
  document.querySelectorAll('[data-profile-avatar]').forEach(el => {
    el.textContent = fmt.initials(user.name);
    el.style.background = fmt.avatarColor(user.name);
  });
}

/* ── Active nav link ────────────────────────── */
function setActiveNav() {
  const page = window.location.pathname.split('/').pop();
  document.querySelectorAll('.nav-item, .topnav-link').forEach(el => {
    el.classList.remove('active');
    const link = el.tagName === 'A' ? el : el.querySelector('a');
    if (link?.getAttribute('href')?.includes(page)) el.classList.add('active');
  });
}

/* ── Download certificate ───────────────────── */
async function downloadCertificate(certId, filename = 'certificate.pdf') {
  Toast.show('Preparing download…', 'info');
  const blob = await api.download(`/certificates/${certId}/download`);
  if (!blob) return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  a.click(); URL.revokeObjectURL(url);
  Toast.show('Certificate downloaded.', 'success');
}

/* ── Global dropdown/panel close ───────────────── */
document.addEventListener('click', e => {
  document.querySelectorAll('.dropdown-menu.open').forEach(dd => {
    if (!dd.contains(e.target) && !dd.previousElementSibling?.contains(e.target)) {
      dd.classList.remove('open');
    }
  });
  document.querySelectorAll('.notif-panel.open').forEach(np => {
    if (!np.contains(e.target)) np.classList.remove('open');
  });
});

/* ── DOM ready init ─────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  populateProfile();
  setActiveNav();

  // Logout buttons
  document.querySelectorAll('[data-action="logout"]').forEach(btn => {
    btn.addEventListener('click', () => confirmAction('Sign out of EduEvent?', () => Auth.logout()));
  });

  // Modal close on overlay click
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) overlay.classList.remove('open');
    });
  });
});
