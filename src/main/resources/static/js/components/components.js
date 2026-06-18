/* ═══════════════════════════════════════════
   EDUEVENT — components.js
   Shared UI component builders
═══════════════════════════════════════════ */

/* ── Admin Sidebar ──────────────────────────── */
function buildAdminSidebar(activePage) {
  const nav = [
    { href:'dashboard.html', icon:'ti-layout-dashboard', label:'Dashboard', section:'main' },
    { href:'events.html',    icon:'ti-calendar-event',   label:'Events',    section:'main' },
    { href:'hackathons.html',icon:'ti-tournament',        label:'Event Management',section:'main' },
    { href:'quizzes.html',   icon:'ti-clipboard-check',  label:'Quizzes',   section:'main' },
    { href:'leaderboards.html',icon:'ti-trophy',          label:'Leaderboards',section:'insights' },
    { href:'analytics.html', icon:'ti-chart-bar',        label:'Analytics', section:'insights' },
    { href:'certificates.html',icon:'ti-certificate',    label:'Certificates',section:'insights' },
    { href:'users.html',     icon:'ti-users',            label:'Users',     section:'manage' },
    { href:'notifications.html',icon:'ti-bell',          label:'Notifications',section:'manage', badge:'notif-count' },
  ];
  const sections = { main:'Main', insights:'Insights', manage:'Manage' };
  let html = '<aside class="sidebar">';
  html += `<div class="sidebar-logo">
    <div class="sidebar-logo-icon">E</div>
    <span class="sidebar-logo-text">EduEvent</span>
    <span class="sidebar-logo-badge">Admin</span>
  </div>`;
  let lastSection = null;
  nav.forEach(item => {
    if (item.section !== lastSection) {
      if (lastSection) html += '</ul></div>';
      html += `<div class="sidebar-section"><div class="sidebar-label">${sections[item.section]}</div><ul class="sidebar-nav">`;
      lastSection = item.section;
    }
    const active = activePage === item.href ? 'active' : '';
    const badge  = item.badge ? `<span class="nav-badge" id="${item.badge}"></span>` : '';
    html += `<li class="nav-item ${active}">
      <a href="${item.href}"><span class="nav-icon"><i class="ti ${item.icon}"></i></span><span>${item.label}</span>${badge}</a>
    </li>`;
  });
  html += `</ul></div>
  <div class="sidebar-footer">
    <div class="admin-profile">
      <div style="display:flex;align-items:center;gap:8px;flex:1;min-width:0;cursor:pointer;" data-action="profile" title="Edit profile">
        <div class="avatar" data-profile-avatar style="width:32px;height:32px;font-size:12px;flex-shrink:0;">A</div>
        <div class="admin-info" style="min-width:0;">
          <div style="font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" data-profile-name>Admin</div>
          <div style="font-size:11px;color:var(--text-muted);" data-profile-role>Administrator</div>
        </div>
      </div>
      <button class="icon-btn" data-action="logout" title="Sign out" style="margin-left:auto;flex-shrink:0;"><i class="ti ti-logout"></i></button>
    </div>
  </div>
  </aside>`;
  return html;
}

/* ── Admin Topbar ───────────────────────────── */
function buildAdminTopbar(title) {
  return `<header class="topbar">
    <div style="font-family:var(--font-display);font-size:17px;font-weight:700;flex:1;">${title}</div>
    <div class="topbar-right">
      <button class="icon-btn" id="notif-toggle" title="Notifications">
        <i class="ti ti-bell"></i><span class="notif-dot" id="notif-dot" style="display:none;"></span>
      </button>
      <button class="icon-btn"><i class="ti ti-search"></i></button>
    </div>
  </header>`;
}

/* ── User/Judge Topnav ──────────────────────── */
function buildTopnav(role, activePage) {
  const navMap = {
    USER: [
      { href:'dashboard.html',    icon:'ti-layout-dashboard', label:'Dashboard' },
      { href:'events.html',       icon:'ti-calendar-event',   label:'Events' },
      { href:'quizzes.html',      icon:'ti-clipboard-check',  label:'My Quizzes' },
      { href:'my-team.html',      icon:'ti-users-group',       label:'My Team' },
      { href:'certificates.html', icon:'ti-certificate',       label:'Certificates' },
    ],
    JUDGE: [
      { href:'dashboard.html',    icon:'ti-layout-dashboard',  label:'Dashboard' },
      { href:'hackathons.html',   icon:'ti-calendar-event',     label:'My Events' },
      { href:'submissions.html',  icon:'ti-files',              label:'Submissions' },
      { href:'scoring.html',      icon:'ti-pencil-star',        label:'Scoring' },
      { href:'results.html',      icon:'ti-chart-bar',          label:'Results' },
    ]
  };

  const links = (navMap[role] || []).map(p => {
    const active = activePage === p.href;
    return `<a class="topnav-link${active ? ' active' : ''}" href="${p.href}">
      <i class="ti ${p.icon}"></i><span>${p.label}</span>
    </a>`;
  }).join('');

  const badgeClass = role === 'JUDGE' ? 'judge' : 'user';
  const badgeLabel = role === 'JUDGE' ? 'Judge' : 'Participant';

  return `<nav class="topbar">
    <a href="dashboard.html" class="topnav-logo">
      <div class="topnav-logo-icon">E</div>
      <span class="topnav-logo-text">EduEvent</span>
      <span class="topnav-badge ${badgeClass}">${badgeLabel}</span>
    </a>
    <div class="topnav-links">${links}</div>
    <div class="topbar-right">
      <button class="icon-btn" id="notif-toggle" title="Notifications">
        <i class="ti ti-bell"></i><span class="notif-dot" id="notif-dot" style="display:none;"></span>
      </button>
      <div class="portal-avatar ${badgeClass}" id="avatar-btn" data-profile-avatar>U</div>
    </div>
  </nav>`;
}

/* ── Notification Panel ─────────────────────── */
function buildNotifPanel() {
  return `<div class="notif-panel" id="notif-panel">
    <div class="notif-panel-header">
      Notifications
      <button class="btn btn-secondary btn-sm" onclick="markAllNotifsRead()">Mark all read</button>
    </div>
    <div id="notif-panel-list"><div class="empty-state" style="padding:24px;"><div class="empty-desc">Loading…</div></div></div>
  </div>`;
}

/* ── User Dropdown ──────────────────────────── */
function buildUserDropdown() {
  const user = Auth.getUser();
  return `<div class="dropdown-menu" id="user-dropdown">
    <div class="dropdown-user-info">
      <div class="dropdown-user-name" data-profile-name>${user?.name||'User'}</div>
      <div class="dropdown-user-email" data-profile-email>${user?.email||''}</div>
      ${user?.role ? `<span class="badge badge-upcoming" style="margin-top:6px;font-size:10px;">${user.role}</span>` : ''}
    </div>
    <button class="btn btn-secondary btn-sm" data-action="profile" style="width:100%;justify-content:flex-start;margin-bottom:4px;">
      <i class="ti ti-user"></i> My Profile
    </button>
    <hr class="divider" style="margin:6px 0;">
    <button class="btn btn-danger btn-sm" style="width:100%;justify-content:flex-start;" data-action="logout">
      <i class="ti ti-logout"></i> Sign Out
    </button>
  </div>`;
}

/* ── Wire up notification toggle ───────────── */
function initNotifToggle(notifData) {
  const toggle = document.getElementById('notif-toggle');
  const panel  = document.getElementById('notif-panel');
  const dot    = document.getElementById('notif-dot');
  if (!toggle || !panel) return;

  const unread = (notifData || []).filter(n => n.unread).length;
  if (dot && unread > 0) dot.style.display = '';

  renderNotifPanel(notifData || []);

  toggle.addEventListener('click', e => {
    e.stopPropagation();
    panel.classList.toggle('open');
    document.getElementById('user-dropdown')?.classList.remove('open');
  });

  // Always replace any seed/mock data with the user's real notifications.
  refreshNotifs();
}

/* ── Notification mapping + live fetch ──────── */
function _notifTimeAgo(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso), s = Math.floor((Date.now() - d.getTime()) / 1000);
    if (s < 60)    return 'Just now';
    if (s < 3600)  return Math.floor(s / 60) + 'm ago';
    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
    return (typeof fmt !== 'undefined' && fmt.date) ? fmt.date(iso) : d.toLocaleDateString();
  } catch (e) { return ''; }
}

// Map a backend NotificationResponse {id,title,message,isRead,createdAt}
// to the shape renderNotifPanel expects.
function _mapNotif(n) {
  return {
    id:     n.id,
    title:  n.title || 'Notification',
    body:   n.message || n.body || '',
    time:   _notifTimeAgo(n.createdAt || n.time),
    unread: (n.isRead === false) || (n.unread === true),
    icon:   'ti-bell',
    bg:     'rgba(56,189,248,0.14)',
    color:  'var(--brand)'
  };
}

// Fetch the logged-in user's notifications and render them into the bell panel.
let _lastUnreadCount = null;
async function refreshNotifs() {
  if (typeof api === 'undefined') return;
  const data = await api.get('/notifications');
  if (!Array.isArray(data)) return;            // null on error — keep whatever is shown
  const items = data.map(_mapNotif);
  renderNotifPanel(items);
  const unread = items.filter(n => n.unread).length;
  const dot = document.getElementById('notif-dot');
  if (dot) dot.style.display = unread > 0 ? '' : 'none';
  // Chime when a new notification arrives (not on the very first load)
  if (_lastUnreadCount !== null && unread > _lastUnreadCount && typeof playNotifSound === 'function') {
    playNotifSound();
  }
  _lastUnreadCount = unread;
}

function renderNotifPanel(items) {
  const list = document.getElementById('notif-panel-list');
  if (!list) return;
  if (!items.length) {
    list.innerHTML = '<div class="empty-state" style="padding:24px;"><div class="empty-desc">No notifications</div></div>';
    return;
  }
  list.innerHTML = items.map(n => `
    <div class="notif-item ${n.unread?'unread':''}" data-nid="${n.id}" onclick="readNotif('${n.id}',this)">
      <div class="notif-item-icon" style="background:${n.bg};color:${n.color};">
        <i class="ti ${n.icon}"></i>
      </div>
      <div style="flex:1;min-width:0;">
        <div class="notif-item-title">${n.title}</div>
        <div class="notif-item-body">${n.body}</div>
        <div class="notif-item-time">${n.time}</div>
      </div>
      ${n.unread ? '<div class="notif-unread-dot"></div>' : ''}
    </div>`).join('');
}

function readNotif(id, el) {
  el.classList.remove('unread');
  el.querySelector('.notif-unread-dot')?.remove();
  if (id && id !== 'undefined' && typeof api !== 'undefined') {
    api.put(`/notifications/${id}/read`, {});   // persist on the server
  }
  const dot = document.getElementById('notif-dot');
  if (dot && !document.querySelector('.notif-item.unread')) dot.style.display = 'none';
}

function markAllNotifsRead() {
  document.querySelectorAll('.notif-item.unread').forEach(el => {
    el.classList.remove('unread');
    el.querySelector('.notif-unread-dot')?.remove();
    const id = el.getAttribute('data-nid');
    if (id && id !== 'undefined' && typeof api !== 'undefined') {
      api.put(`/notifications/${id}/read`, {});
    }
  });
  const dot = document.getElementById('notif-dot');
  if (dot) dot.style.display = 'none';
}

/* ── Wire up avatar dropdown ─────────────────── */
function initAvatarDropdown() {
  const btn = document.getElementById('avatar-btn');
  const dd  = document.getElementById('user-dropdown');
  if (!btn || !dd) return;
  btn.addEventListener('click', e => {
    e.stopPropagation();
    dd.classList.toggle('open');
    document.getElementById('notif-panel')?.classList.remove('open');
  });
}

/* ── Populate Admin Profile (sidebar footer) ── */
function _renderAdminProfile(user) {
  const avatar = document.getElementById('admin-avatar');
  const name   = document.getElementById('admin-name');
  const role   = document.getElementById('admin-role');
  if (name) name.textContent = user.name || 'Admin';
  if (role) role.textContent = user.role || 'Administrator';
  if (avatar) {
    if (user.photo) {
      avatar.textContent = '';
      avatar.style.backgroundImage = `url('${user.photo}')`;
      avatar.style.backgroundSize = 'cover';
      avatar.style.backgroundPosition = 'center';
    } else {
      avatar.style.backgroundImage = '';
      avatar.textContent = (user.name || 'A').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
    }
    // Click the avatar to edit profile (the rest of the row still logs out)
    avatar.style.cursor = 'pointer';
    avatar.title = 'Edit profile';
    avatar.onclick = (e) => { e.stopPropagation(); if (typeof openProfileModal === 'function') openProfileModal(); };
  }
}

function populateAdminProfile() {
  const user = Auth.getUser();
  if (!user) return;
  _renderAdminProfile(user);                  // instant from cache
  if (typeof api !== 'undefined') {
    api.get('/users/me').then(me => {
      if (!me) return;
      const merged = { ...user, name: me.name ?? user.name, role: me.role ?? user.role,
                       photo: me.photoUrl ?? me.photo ?? user.photo };
      Auth.setUser(merged);
      _renderAdminProfile(merged);
    });
  }
}