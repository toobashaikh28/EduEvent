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

/* ── Login splash (ElevenLabs-style intro) ──── */
function playLoginSplash() {
  if (sessionStorage.getItem('eduSplash') !== '1') return;
  sessionStorage.removeItem('eduSplash');           // one-shot per login
  if (!document.body) return;

  const reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Sparkles streaming from the unicorn's horn (upper-left of the figure)
  const glyphs = ['✨','⭐','🌟','✦','💫'];
  let sparks = '';
  for (let i = 0; i < 14; i++) {
    const g = glyphs[i % glyphs.length];
    const top  = (8 + Math.random() * 40).toFixed(0);
    const left = (-2 + Math.random() * 22).toFixed(0);
    const delay = (Math.random() * 2.2).toFixed(2);
    const dur   = (0.7 + Math.random() * 0.7).toFixed(2);
    const size  = (1.2 + Math.random() * 1.8).toFixed(1);
    sparks += `<span class="spark" style="top:${top}%;left:${left}%;font-size:${size}vh;animation-delay:${delay}s;animation-duration:${dur}s;">${g}</span>`;
  }

  const horseImg = `<img class="u" src="/images/horse-gallop.gif" alt="" draggable="false">`;

  const splash = document.createElement('div');
  splash.className = 'edu-splash';
  splash.innerHTML = `
    <div class="edu-splash__fill"></div>
    <div class="edu-unicorn">${horseImg}${sparks}</div>
    <div class="edu-splash__word">
      <span class="edu-splash__logo">E</span>
      <span class="edu-splash__name">EduEvent</span>
    </div>`;
  document.body.appendChild(splash);

  if (reduced) {
    splash.classList.add('reduced');
    setTimeout(() => splash.remove(), 800);
  } else {
    // horse gallops across (~3s) painting colour in → name reveals → fades → remove
    setTimeout(() => splash.remove(), 5300);
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
