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
  logout()    { this.clear(); window.location.href = '/login.html'; },
  redirectByRole() {
    const role = this.getRole();
    if (role === 'ADMIN')  window.location.href = '/pages/admin/dashboard.html';
    else if (role === 'JUDGE') window.location.href = '/pages/judge/dashboard.html';
    else window.location.href = '/pages/user/dashboard.html';
  },
  guardPage(expectedRole) {
    if (!this.isLoggedIn()) { window.location.href = '/login.html'; return false; }
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
      window.location.href = '/login.html';
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
    // OK response with a non-JSON body (e.g. a plain-text message like
    // "Registration cancelled successfully.") — treat as success, not failure.
    return isJson ? data : { success: true };
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

/* ── Chart theming (cyber dashboard) ─────────── */
/* Call once after Chart.js loads to apply the dark/blue defaults app-wide. */
function applyChartTheme() {
  if (typeof Chart === 'undefined') return;
  Chart.defaults.color = '#7C82A6';
  Chart.defaults.borderColor = 'rgba(0,229,255,0.08)';
  Chart.defaults.font.family = "'DM Sans', sans-serif";
  const tt = Chart.defaults.plugins.tooltip;
  tt.backgroundColor = 'rgba(8,8,16,0.96)';
  tt.borderColor = 'rgba(0,229,255,0.40)';
  tt.borderWidth = 1; tt.cornerRadius = 8; tt.padding = 10;
  tt.displayColors = false; tt.titleColor = '#EAF0FF'; tt.bodyColor = '#AEB4D0';
}
/* Vertical gradient fill for area/bar charts. top/bottom are rgba strings. */
function gradientFill(ctx, top, bottom, h) {
  const g = ctx.createLinearGradient(0, 0, 0, h || 220);
  g.addColorStop(0, top);
  g.addColorStop(1, bottom || 'rgba(0,0,0,0)');
  return g;
}

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
        <div style="width:48px;height:48px;border-radius:50%;background:rgba(248,113,113,0.14);display:flex;align-items:center;justify-content:center;margin:0 auto 14px;">
          <i class="ti ti-alert-triangle" style="color:#FCA5A5;font-size:22px;"></i>
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
    const colors = ['#38BDF8','#34D399','#A78BFA','#FBBF24','#22D3EE','#F472B6','#FB923C'];
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
// Render the name/email/role/avatar chrome from a user object (avatar shows photo if present)
function renderProfileEls(user) {
  document.querySelectorAll('[data-profile-name]').forEach(el  => el.textContent = user.name || 'User');
  document.querySelectorAll('[data-profile-email]').forEach(el => el.textContent = user.email || '');
  document.querySelectorAll('[data-profile-role]').forEach(el  => el.textContent = user.role || '');
  document.querySelectorAll('[data-profile-avatar]').forEach(el => {
    if (user.photo) {
      el.textContent = '';
      el.style.backgroundImage = `url('${user.photo}')`;
      el.style.backgroundSize = 'cover';
      el.style.backgroundPosition = 'center';
      el.style.color = 'transparent';
      el.style.overflow = 'hidden';
    } else {
      el.style.backgroundImage = '';
      el.style.color = '';
      el.textContent = fmt.initials(user.name);
      el.style.background = fmt.avatarColor(user.name);
    }
  });
}

function populateProfile() {
  const user = Auth.getUser();
  if (!user) return;
  renderProfileEls(user);                       // instant from cached login data
  // Refresh from the server so the latest name + photo always show
  if (typeof api !== 'undefined') {
    api.get('/users/me').then(me => {
      if (!me) return;
      const merged = {
        ...user,
        name:  me.name  ?? user.name,
        email: me.email ?? user.email,
        role:  me.role  ?? user.role,
        photo: me.photoUrl ?? me.photo ?? user.photo
      };
      Auth.setUser(merged);
      renderProfileEls(merged);
    });
  }
}

/* ── Edit-Profile modal (name + photo, with image adjust) — shared across all portals ── */
let _pendingProfilePhoto = null;
let _photoDirty = false;
let _photoState = null;          // { img, baseScale, zoom, offX, offY }
const _PHOTO_VIEW = 168;         // editor circle size (px) — what the user sees
const _PHOTO_OUT  = 256;         // exported square size (px) — what gets stored

function ensureProfileModal() {
  if (document.getElementById('profile-modal')) return;
  const div = document.createElement('div');
  div.className = 'modal-overlay';
  div.id = 'profile-modal';
  div.innerHTML = `
    <div class="modal" style="max-width:420px;">
      <div class="modal-header">
        <div class="modal-title">Edit Profile</div>
        <button class="icon-btn" type="button" onclick="document.getElementById('profile-modal').classList.remove('open')"><i class="ti ti-x"></i></button>
      </div>
      <div style="display:flex;flex-direction:column;align-items:center;gap:10px;padding:6px 0 14px;">
        <div style="position:relative;width:${_PHOTO_VIEW}px;height:${_PHOTO_VIEW}px;">
          <div id="profile-avatar-fallback" style="position:absolute;inset:0;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:54px;font-weight:700;color:#fff;"></div>
          <canvas id="profile-photo-canvas" width="${_PHOTO_VIEW}" height="${_PHOTO_VIEW}" style="position:absolute;inset:0;width:${_PHOTO_VIEW}px;height:${_PHOTO_VIEW}px;border-radius:50%;display:none;cursor:grab;touch-action:none;box-shadow:inset 0 0 0 2px rgba(255,255,255,0.12);"></canvas>
        </div>
        <input type="range" id="profile-zoom" min="1" max="3" step="0.01" value="1" style="width:${_PHOTO_VIEW}px;display:none;accent-color:var(--brand);">
        <label class="btn btn-secondary btn-sm" style="cursor:pointer;">
          <i class="ti ti-camera"></i> Choose Photo
          <input type="file" id="profile-photo-input" accept="image/*" style="display:none;">
        </label>
        <div id="profile-photo-hint" style="font-size:11px;color:var(--text-muted);">PNG / JPG · a square image looks best</div>
      </div>
      <div class="form-group" style="margin-bottom:14px;">
        <label class="form-label">Display Name</label>
        <input type="text" id="profile-name-input" placeholder="Your name">
      </div>
      <div style="display:flex;gap:10px;justify-content:flex-end;">
        <button class="btn btn-secondary" type="button" onclick="document.getElementById('profile-modal').classList.remove('open')">Cancel</button>
        <button class="btn btn-primary" type="button" id="profile-save-btn" onclick="saveProfile()"><i class="ti ti-check"></i> Save</button>
      </div>
    </div>`;
  document.body.appendChild(div);
  div.querySelector('#profile-photo-input').addEventListener('change', handleProfilePhoto);
  div.addEventListener('click', e => { if (e.target === div) div.classList.remove('open'); });
  _wirePhotoEditor();
}

// Geometry helpers — "baseScale" makes the image cover the circle; "zoom" is the user multiplier on top.
function _photoDims() {
  const s = _photoState.baseScale * _photoState.zoom;
  return { dw: _photoState.img.width * s, dh: _photoState.img.height * s };
}
function _clampPhoto() {
  const { dw, dh } = _photoDims();
  _photoState.offX = Math.min(0, Math.max(_PHOTO_VIEW - dw, _photoState.offX));
  _photoState.offY = Math.min(0, Math.max(_PHOTO_VIEW - dh, _photoState.offY));
}
function _drawPhotoEditor() {
  if (!_photoState) return;
  const ctx = document.getElementById('profile-photo-canvas').getContext('2d');
  const { dw, dh } = _photoDims();
  ctx.clearRect(0, 0, _PHOTO_VIEW, _PHOTO_VIEW);
  ctx.drawImage(_photoState.img, _photoState.offX, _photoState.offY, dw, dh);
}

function _showFallbackAvatar(user) {
  const fb = document.getElementById('profile-avatar-fallback');
  fb.style.display = 'flex';
  fb.textContent = fmt.initials(user.name);
  fb.style.background = fmt.avatarColor(user.name);
  document.getElementById('profile-photo-canvas').style.display = 'none';
  document.getElementById('profile-zoom').style.display = 'none';
  _photoState = null;
}

function loadPhotoIntoEditor(src) {
  const img = new Image();
  img.crossOrigin = 'anonymous';
  img.onload = () => {
    const base = Math.max(_PHOTO_VIEW / img.width, _PHOTO_VIEW / img.height);
    _photoState = { img, baseScale: base, zoom: 1, offX: 0, offY: 0 };
    const { dw, dh } = _photoDims();
    _photoState.offX = (_PHOTO_VIEW - dw) / 2;   // center by default
    _photoState.offY = (_PHOTO_VIEW - dh) / 2;
    document.getElementById('profile-avatar-fallback').style.display = 'none';
    document.getElementById('profile-photo-canvas').style.display = 'block';
    const zoom = document.getElementById('profile-zoom');
    zoom.value = 1; zoom.style.display = 'block';
    document.getElementById('profile-photo-hint').textContent = 'Drag to reposition · slider to zoom';
    _drawPhotoEditor();
  };
  img.onerror = () => Toast.show('Could not load that image.', 'error');
  img.src = src;
}

function _wirePhotoEditor() {
  const cv = document.getElementById('profile-photo-canvas');
  const zoom = document.getElementById('profile-zoom');
  let dragging = false, lastX = 0, lastY = 0;
  const start = (x, y) => { if (!_photoState) return; dragging = true; lastX = x; lastY = y; cv.style.cursor = 'grabbing'; };
  const move = (x, y) => {
    if (!dragging || !_photoState) return;
    _photoState.offX += (x - lastX); _photoState.offY += (y - lastY);
    lastX = x; lastY = y; _clampPhoto(); _drawPhotoEditor(); _photoDirty = true;
  };
  const end = () => { dragging = false; cv.style.cursor = 'grab'; };
  cv.addEventListener('mousedown', e => start(e.clientX, e.clientY));
  window.addEventListener('mousemove', e => move(e.clientX, e.clientY));
  window.addEventListener('mouseup', end);
  cv.addEventListener('touchstart', e => { const t = e.touches[0]; start(t.clientX, t.clientY); }, { passive: true });
  cv.addEventListener('touchmove', e => { const t = e.touches[0]; move(t.clientX, t.clientY); e.preventDefault(); }, { passive: false });
  cv.addEventListener('touchend', end);
  zoom.addEventListener('input', () => {
    if (!_photoState) return;
    // zoom toward the centre of the circle so the framing stays put
    const c = _PHOTO_VIEW / 2;
    const prev = _photoState.baseScale * _photoState.zoom;
    const fx = (c - _photoState.offX) / prev, fy = (c - _photoState.offY) / prev;
    _photoState.zoom = parseFloat(zoom.value);
    const next = _photoState.baseScale * _photoState.zoom;
    _photoState.offX = c - fx * next; _photoState.offY = c - fy * next;
    _clampPhoto(); _drawPhotoEditor(); _photoDirty = true;
  });
}

function openProfileModal() {
  ensureProfileModal();
  const user = Auth.getUser() || {};
  _pendingProfilePhoto = null;
  _photoDirty = false;
  document.getElementById('profile-name-input').value = user.name || '';
  document.getElementById('profile-photo-hint').textContent = 'PNG / JPG · a square image looks best';
  if (user.photo) loadPhotoIntoEditor(user.photo);
  else _showFallbackAvatar(user);
  document.getElementById('profile-modal').classList.add('open');
}

function handleProfilePhoto(e) {
  const file = e.target.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) { Toast.show('Image too large (max 5MB).', 'error'); return; }
  const reader = new FileReader();
  reader.onload = ev => { loadPhotoIntoEditor(ev.target.result); _photoDirty = true; };
  reader.readAsDataURL(file);
}

// Render the user's chosen framing onto a 256×256 square (cover) for storage.
function _exportAdjustedPhoto() {
  const k = _PHOTO_OUT / _PHOTO_VIEW;
  const canvas = document.createElement('canvas');
  canvas.width = _PHOTO_OUT; canvas.height = _PHOTO_OUT;
  const ctx = canvas.getContext('2d');
  const { dw, dh } = _photoDims();
  ctx.drawImage(_photoState.img, _photoState.offX * k, _photoState.offY * k, dw * k, dh * k);
  return canvas.toDataURL('image/jpeg', 0.85);
}

async function saveProfile() {
  const name = document.getElementById('profile-name-input').value.trim();
  if (!name) { Toast.show('Name cannot be empty.', 'warning'); return; }
  const btn = document.getElementById('profile-save-btn');
  setLoading(btn, true);
  const payload = { name };
  if (_photoDirty && _photoState) {
    try { _pendingProfilePhoto = _exportAdjustedPhoto(); }
    catch (err) { setLoading(btn, false); Toast.show('Could not process that image — try choosing the file again.', 'error'); return; }
    payload.photoUrl = _pendingProfilePhoto;
  }
  const res = await api.put('/users/me', payload);
  setLoading(btn, false);
  if (!res) return;
  const user = Auth.getUser() || {};
  const merged = { ...user, name: res.name ?? name, photo: res.photoUrl ?? _pendingProfilePhoto ?? user.photo };
  Auth.setUser(merged);
  renderProfileEls(merged);
  if (typeof populateAdminProfile === 'function') populateAdminProfile(); // refresh admin sidebar avatar/name
  document.getElementById('profile-modal').classList.remove('open');
  Toast.show('Profile updated!', 'success');
}

// Delegated trigger — any element with data-action="profile" opens the editor
document.addEventListener('click', e => {
  const t = e.target.closest && e.target.closest('[data-action="profile"]');
  if (t) { e.preventDefault(); e.stopPropagation(); openProfileModal(); }
});

/* ── Notification tune (short pleasant chime via Web Audio) ── */
function playNotifSound() {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext;
    if (!Ctx) return;
    const ctx = new Ctx();
    const now = ctx.currentTime;
    // two quick notes — a soft "ding-dong"
    [[880, 0], [1175, 0.12]].forEach(([freq, t]) => {
      const o = ctx.createOscillator(), g = ctx.createGain();
      o.type = 'sine'; o.frequency.value = freq;
      o.connect(g); g.connect(ctx.destination);
      g.gain.setValueAtTime(0.0001, now + t);
      g.gain.exponentialRampToValueAtTime(0.22, now + t + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, now + t + 0.30);
      o.start(now + t); o.stop(now + t + 0.32);
    });
    setTimeout(() => ctx.close(), 800);
  } catch (e) { /* audio not allowed yet — ignore */ }
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
  await saveBlobAs(blob, filename);
}

/* Save a blob to disk. In Chromium browsers this opens the native OS
   "Save As" file-explorer dialog so the user picks the location; elsewhere
   it falls back to a normal download into the browser's download folder. */
async function saveBlobAs(blob, filename = 'download.pdf') {
  if (window.showSaveFilePicker) {
    try {
      const handle = await window.showSaveFilePicker({
        suggestedName: filename,
        types: [{ description: 'PDF Document', accept: { 'application/pdf': ['.pdf'] } }]
      });
      const writable = await handle.createWritable();
      await writable.write(blob);
      await writable.close();
      Toast.show('Certificate saved.', 'success');
      return;
    } catch (err) {
      if (err && err.name === 'AbortError') return; // user cancelled the dialog
      // any other error → fall through to the classic download
    }
  }
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

/* ── Login splash (ElevenLabs-style intro) ──── */
function playLoginSplash() {
  if (sessionStorage.getItem('eduSplash') !== '1') return;
  sessionStorage.removeItem('eduSplash');           // one-shot per login
  if (!document.body) return;

  const reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const splash = document.createElement('div');
  splash.className = 'edu-splash';
  splash.innerHTML = `
    <div class="edu-splash__fill"></div>
    <div class="edu-splash__word">
      <span class="edu-splash__logo">E</span>
      <span class="edu-splash__name">EduEvent</span>
    </div>`;
  document.body.appendChild(splash);

  if (reduced) {
    splash.classList.add('reduced');
    setTimeout(() => splash.remove(), 800);
  } else {
    // colour paints in → name reveals → fades → remove
    setTimeout(() => splash.remove(), 3200);
  }
}

/* ── DOM ready init ─────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  playLoginSplash();
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

/* ═══════════════════════════════════════════
   EDUEVENT — MOTION ENGINE (Bright Pastel · Premium SaaS)
   Self-contained, dependency-free, 60fps.
   • Page-enter choreography (chrome → content stagger)
   • Scroll/entrance reveals (IntersectionObserver, blur-to-sharp)
   • Animated number counters
   • Magnetic primary buttons + soft brand ripple
   • Refined cursor-glow + micro-tilt (tasteful, ≤3°)
   • Chart.js pastel + animation defaults
   • MutationObserver catches dynamically-rendered content
   • Honors prefers-reduced-motion
═══════════════════════════════════════════ */
(function EduMotion() {
  const reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const REVEAL_SEL = [
    '.card', '.stat-card', '.event-card', '.cert-card', '.cert-card-user',
    '.hack-card', '.hack-select-card', '.chart-card', '.podium-block',
    '.accordion-card', '.result-team-card', '.member-card', '.rank-card',
    '.quiz-stat-pill', '.empty-state'
  ].join(',');
  const TILT_SEL = '.stat-card, .event-card, .cert-card, .cert-card-user, .hack-card';
  const MAGNET_SEL = '.btn-primary';
  const CHROME_SEL = '.topbar, .topnav, .judge-topnav, .sidebar';
  const COUNTER_SEL = '.stat-value, .rank-number, .quiz-stat-pill .val';

  let io = null;
  if (!reduced && 'IntersectionObserver' in window) {
    io = new IntersectionObserver((entries, obs) => {
      entries.forEach(en => {
        if (en.isIntersecting) {
          const el = en.target;
          const delay = Math.min(parseInt(el.dataset.rvIndex || '0', 10), 8) * 70;
          el.style.transitionDelay = delay + 'ms';
          el.classList.add('in');
          if (el.matches(COUNTER_SEL)) animateCounter(el);
          else el.querySelectorAll && el.querySelectorAll(COUNTER_SEL).forEach(animateCounter);
          obs.unobserve(el);
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
  }

  /* Reveal setup */
  function setupReveals(root) {
    if (reduced || !io) { (root || document).querySelectorAll(COUNTER_SEL).forEach(animateCounter); return; }
    const scope = root && root.querySelectorAll ? root : document;
    const nodes = scope.querySelectorAll(REVEAL_SEL);
    let i = 0;
    nodes.forEach(el => {
      if (el.dataset.rv) return;
      el.dataset.rv = '1';
      el.dataset.rvIndex = (i++ % 9);
      el.classList.add('reveal');
      io.observe(el);
    });
  }

  /* Counters */
  function animateCounter(el) {
    if (reduced || !el || el.dataset.counted) return;
    const raw = (el.textContent || '').trim();
    const m = raw.match(/^([^\d-]*)(-?[\d,]+(?:\.\d+)?)(.*)$/);
    if (!m) return;
    const prefix = m[1], suffix = m[3];
    const hasComma = m[2].indexOf(',') >= 0;
    const numStr = m[2].replace(/,/g, '');
    const target = parseFloat(numStr);
    if (!isFinite(target)) return;
    const decimals = (numStr.split('.')[1] || '').length;
    el.dataset.counted = '1';
    const dur = 1100, start = performance.now();
    function fmtNum(v) {
      let s = decimals ? v.toFixed(decimals) : Math.round(v).toString();
      if (hasComma) s = Number(s).toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
      return prefix + s + suffix;
    }
    function tick(now) {
      const t = Math.min(1, (now - start) / dur);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = fmtNum(target * eased);
      if (t < 1) requestAnimationFrame(tick); else el.textContent = fmtNum(target);
    }
    el.textContent = fmtNum(0);
    requestAnimationFrame(tick);
  }

  /* Soft brand-tinted ripple (event delegation) */
  document.addEventListener('pointerdown', e => {
    if (reduced) return;
    const btn = e.target.closest && e.target.closest('.btn');
    if (!btn) return;
    const r = btn.getBoundingClientRect();
    const ink = document.createElement('span');
    ink.className = 'ripple-ink';
    const size = Math.max(r.width, r.height);
    ink.style.width = ink.style.height = size + 'px';
    ink.style.left = (e.clientX - r.left - size / 2) + 'px';
    ink.style.top = (e.clientY - r.top - size / 2) + 'px';
    btn.appendChild(ink);
    setTimeout(() => ink.remove(), 600);
  }, { passive: true });

  /* Refined cursor-glow + micro-tilt (rAF-throttled, tasteful ≤3°) */
  function bindTilt(root) {
    if (reduced) return;
    (root && root.querySelectorAll ? root : document).querySelectorAll(TILT_SEL).forEach(card => {
      if (card.dataset.tilt) return;
      card.dataset.tilt = '1';
      card.classList.add('tilt');
      let raf = null;
      card.addEventListener('pointermove', ev => {
        if (ev.pointerType === 'touch') return;
        if (raf) return;
        raf = requestAnimationFrame(() => {
          raf = null;
          const r = card.getBoundingClientRect();
          const px = (ev.clientX - r.left) / r.width - 0.5;
          const py = (ev.clientY - r.top) / r.height - 0.5;
          card.style.setProperty('--mx', ((ev.clientX - r.left) / r.width * 100) + '%');
          card.style.setProperty('--my', ((ev.clientY - r.top) / r.height * 100) + '%');
          card.classList.add('glow');
          card.style.transform = `perspective(900px) translateY(-4px) rotateX(${(-py * 3).toFixed(2)}deg) rotateY(${(px * 3).toFixed(2)}deg)`;
        });
      });
      card.addEventListener('pointerleave', () => { card.style.transform = ''; card.classList.remove('glow'); });
    });
  }

  /* Magnetic primary buttons — nudge toward cursor (~4px) */
  function bindMagnet(root) {
    if (reduced) return;
    (root && root.querySelectorAll ? root : document).querySelectorAll(MAGNET_SEL).forEach(btn => {
      if (btn.dataset.magnet) return;
      btn.dataset.magnet = '1';
      btn.addEventListener('pointermove', ev => {
        if (ev.pointerType === 'touch') return;
        const r = btn.getBoundingClientRect();
        const mx = (ev.clientX - r.left) / r.width - 0.5;
        const my = (ev.clientY - r.top) / r.height - 0.5;
        btn.style.transform = `translate(${(mx * 7).toFixed(1)}px, ${(my * 5 - 1).toFixed(1)}px)`;
      });
      btn.addEventListener('pointerleave', () => { btn.style.transform = ''; });
    });
  }

  /* Page-enter choreography: chrome fades/slides in on load */
  function playPageEnter() {
    if (reduced) return;
    document.querySelectorAll(CHROME_SEL).forEach(el => el.classList.add('page-enter'));
  }

  /* Chart.js pastel + entrance-animation defaults (only if Chart is present) */
  function applyChartDefaults() {
    if (!window.Chart || window.__eduChartThemed) return;
    window.__eduChartThemed = true;
    const C = window.Chart;
    try {
      C.defaults.font.family = "'DM Sans', sans-serif";
      C.defaults.color = '#9490B5';
      C.defaults.borderColor = 'rgba(124,111,205,0.12)';
      if (!reduced) { C.defaults.animation = { duration: 900, easing: 'easeOutQuart' }; }
      else { C.defaults.animation = false; }
      if (C.defaults.plugins && C.defaults.plugins.legend && C.defaults.plugins.legend.labels) {
        C.defaults.plugins.legend.labels.color = '#4A4570';
      }
    } catch (_) {}
  }

  function scan(root) { setupReveals(root); bindTilt(root); bindMagnet(root); }

  /* Observe dynamically-injected content (pages replace #app after load) */
  function startObserver() {
    if (!('MutationObserver' in window)) return;
    const mo = new MutationObserver(muts => {
      for (const mu of muts) {
        for (const node of mu.addedNodes) {
          if (node.nodeType === 1) scan(node);
        }
      }
    });
    mo.observe(document.body, { childList: true, subtree: true });
  }

  function init() {
    playPageEnter();
    applyChartDefaults();
    scan(document);
    startObserver();
    // Chart.js may load after core.js — retry defaults shortly
    setTimeout(applyChartDefaults, 300);
    // Safety net: never leave content hidden if observer misfires
    setTimeout(() => document.querySelectorAll('.reveal:not(.in)').forEach(el => el.classList.add('in')), 1800);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();

  window.EduMotion = { scan, animateCounter };
})();
