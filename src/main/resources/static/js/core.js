/* ============================================================
   EduEvent · core.js  — shared API + Auth + UI helpers
   Served from /js/core.js (Spring Boot static resources)
   ============================================================ */

const API_BASE = "/api";

/* ---------------- token / user storage ---------------- */
const Auth = {
  getToken() { return localStorage.getItem("jwt") || ""; },
  setToken(t) { localStorage.setItem("jwt", t); },
  getUser() {
    try { return JSON.parse(localStorage.getItem("edu_user") || "null"); }
    catch (e) { return null; }
  },
  setUser(u) { localStorage.setItem("edu_user", JSON.stringify(u || {})); },
  clear() { localStorage.removeItem("jwt"); localStorage.removeItem("edu_user"); },
  role() { return (this.getUser()?.role || "").toUpperCase(); },

  /** Redirect to the right dashboard for a role */
  homeFor(role) {
    role = (role || "").toUpperCase();
    if (role === "ADMIN") return "/admin/dashboard.html";
    if (role === "JUDGE") return "/judge/dashboard.html";
    return "/user/dashboard.html";
  },

  /** Call at top of every protected page. Returns false + redirects if not allowed. */
  guardPage(requiredRole) {
    const t = this.getToken();
    if (!t) { window.location.replace("/index.html"); return false; }
    if (requiredRole && this.role() !== requiredRole.toUpperCase()) {
      window.location.replace(this.homeFor(this.role()));
      return false;
    }
    return true;
  },

  logout() { this.clear(); window.location.replace("/index.html"); }
};

/* ---------------- fetch wrapper ---------------- */
async function _request(method, path, body, isMultipart) {
  const headers = {};
  const token = Auth.getToken();
  if (token) headers["Authorization"] = "Bearer " + token;
  if (!isMultipart && body !== undefined) headers["Content-Type"] = "application/json";

  let res;
  try {
    res = await fetch(API_BASE + path, {
      method, headers,
      body: isMultipart ? body : (body !== undefined ? JSON.stringify(body) : undefined)
    });
  } catch (netErr) {
    toast("Can't reach the server. Is the backend running?", "error");
    return null;
  }

  if (res.status === 401) {
    // token expired / invalid → back to landing
    Auth.clear();
    toast("Session expired. Please sign in again.", "error");
    setTimeout(() => window.location.replace("/index.html"), 1200);
    return null;
  }

  let data = null;
  const text = await res.text();
  if (text) { try { data = JSON.parse(text); } catch (e) { data = { message: text }; } }

  if (!res.ok) {
    const msg = data?.message || data?.error || ("Request failed (" + res.status + ")");
    toast(msg, "error");
    return null;
  }
  return data ?? {};
}

const api = {
  get:  (p)        => _request("GET", p),
  post: (p, b)     => _request("POST", p, b ?? {}),
  put:  (p, b)     => _request("PUT", p, b ?? {}),
  del:  (p)        => _request("DELETE", p),
  /** multipart upload — pass a FormData */
  upload:    (p, fd) => _request("POST", p, fd, true),
  uploadPut: (p, fd) => _request("PUT",  p, fd, true),
  /** authenticated file download (e.g. certificate PDF) */
  async download(path, filename) {
    const headers = {};
    const token = Auth.getToken();
    if (token) headers["Authorization"] = "Bearer " + token;
    const res = await fetch(API_BASE + path, { headers });
    if (!res.ok) { toast("Download failed.", "error"); return; }
    const blob = await res.blob();
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = filename || "download.pdf";
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 4000);
  }
};

/* ---------------- toast ---------------- */
function toast(msg, type) {
  let wrap = document.getElementById("edu-toast-wrap");
  if (!wrap) {
    wrap = document.createElement("div");
    wrap.id = "edu-toast-wrap";
    document.body.appendChild(wrap);
  }
  const t = document.createElement("div");
  t.className = "edu-toast " + (type === "error" ? "err" : type === "success" ? "ok" : "");
  t.innerHTML = '<span class="tdot"></span><span>' + escapeHtml(String(msg)) + "</span>";
  wrap.appendChild(t);
  requestAnimationFrame(() => t.classList.add("in"));
  setTimeout(() => { t.classList.remove("in"); setTimeout(() => t.remove(), 350); }, 3400);
}

/* ---------------- page loader ----------------
   Add <div id="page-loader">…</div> markup via injectLoader() and call
   hidePageLoader() when your first data render is done.               */
function injectLoader(label, dark) {
  const d = document.createElement("div");
  d.id = "page-loader";
  d.className = "page-loader" + (dark ? " dark" : "");
  d.innerHTML =
    '<div class="pl-mark"><div class="pl-tile">E</div><div class="pl-ring"></div></div>' +
    '<div class="pl-text">' + escapeHtml(label || "Loading EduEvent…") + "</div>";
  document.body.appendChild(d);
}
function hidePageLoader() {
  const el = document.getElementById("page-loader");
  if (!el) return;
  el.classList.add("out");
  setTimeout(() => el.remove(), 450);
}

/* ---------------- tiny utils ---------------- */
function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"']/g, m => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[m]));
}
function initials(name) {
  return String(name || "?").trim().split(/\s+/).slice(0, 2)
    .map(w => w[0]?.toUpperCase() || "").join("") || "?";
}
const AV_COLORS = ["linear-gradient(135deg,#6366f1,#4f46e5)","linear-gradient(135deg,#0d9488,#0ea5e9)","linear-gradient(135deg,#f59e0b,#ec4899)","linear-gradient(135deg,#db2777,#a855f7)","linear-gradient(135deg,#10b981,#0d9488)"];
function avColor(seed) {
  let h = 0; const s = String(seed || "x");
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return AV_COLORS[h % AV_COLORS.length];
}
/** Backend stores LocalDateTime in UTC but serialises it WITHOUT a timezone marker
 *  (e.g. "2026-07-09T05:15:00"). Browsers parse a marker-less ISO string as LOCAL
 *  time, silently shifting every timestamp by the viewer's UTC offset (5h for PKT).
 *  This helper appends "Z" when no zone/offset is present so the value is read as
 *  UTC, while leaving strings that already carry one (Z / +05:00) untouched. */
function toUtcDate(d) {
  if (d instanceof Date) return d;
  if (typeof d === "number") return new Date(d);
  if (typeof d !== "string") return new Date(d);
  const hasZone = /[zZ]$|[+-]\d{2}:?\d{2}$/.test(d.trim());
  return new Date(hasZone ? d : d + "Z");
}
function fmtDate(d) {
  if (!d) return "—";
  const dt = toUtcDate(d);
  if (isNaN(dt)) return String(d);
  return dt.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}
function fmtDateTime(d) {
  if (!d) return "—";
  const dt = toUtcDate(d);
  if (isNaN(dt)) return String(d);
  return dt.toLocaleDateString(undefined, { day: "numeric", month: "short" }) + " · " +
         dt.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}
function timeAgo(d) {
  const dt = toUtcDate(d); if (isNaN(dt)) return "";
  const s = (Date.now() - dt.getTime()) / 1000;
  if (s < 60) return "just now";
  if (s < 3600) return Math.floor(s / 60) + "m ago";
  if (s < 86400) return Math.floor(s / 3600) + "h ago";
  return Math.floor(s / 86400) + "d ago";
}
/** first defined key from an object — survives DTO naming differences */
function pick(obj, ...keys) {
  for (const k of keys) if (obj && obj[k] !== undefined && obj[k] !== null) return obj[k];
  return undefined;
}
/** normalise list responses: [..] or {content:[..]} or {data:[..]} */
function asList(res) {
  if (Array.isArray(res)) return res;
  if (Array.isArray(res?.content)) return res.content;
  if (Array.isArray(res?.data)) return res.data;
  if (Array.isArray(res?.items)) return res.items;
  return [];
}

/* type → theme mapping used everywhere */
function typeTheme(type) {
  const t = String(type || "").toLowerCase();
  if (t.includes("hack"))  return { emoji: "💻", bg: "linear-gradient(135deg,#fde9f3,#fbcfe8)", chip: "#db2777", chipBg: "#fde9f3", label: "HACKATHON" };
  if (t.includes("quiz"))  return { emoji: "🧠", bg: "linear-gradient(135deg,#fef4dc,#fde68a)", chip: "#d97706", chipBg: "#fdf0d6", label: "QUIZ" };
  if (t.includes("conf"))  return { emoji: "🎤", bg: "linear-gradient(135deg,#e6fbf6,#cffaf0)", chip: "#0d9488", chipBg: "#d7f3ee", label: "CONFERENCE" };
  return { emoji: "🎥", bg: "linear-gradient(135deg,#eef1ff,#e0e7ff)", chip: "#4f46e5", chipBg: "#eef0ff", label: "WEBINAR" };
}
