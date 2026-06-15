# EduEvent Frontend

Complete multi-role frontend for the EduEvent Spring Boot platform.  
Dark SaaS theme consistent across all three portals.

---

## Folder Structure

```
/frontend/
├── index.html                    ← Universal auth portal (Login + Register)
├── css/
│   ├── global.css                ← Full shared design system, CSS variables, all components
│   ├── user.css                  ← User portal overrides (topnav layout, event cards)
│   └── judge.css                 ← Judge portal overrides (accordion, scoring UI)
├── js/
│   ├── core.js                   ← JWT auth, API interceptor, error handler, toast, helpers
│   └── components/
│       └── components.js         ← Sidebar, topnav, notification panel builders
└── pages/
    ├── admin/                    ← Role-protected: ADMIN only
    │   ├── dashboard.html        ← 5-card analytics hub + charts
    │   ├── events.html           ← Event CRUD with filters
    │   ├── hackathons.html       ← Team management + Announce Winners
    │   ├── quizzes.html          ← Quiz management, participant breakdown
    │   ├── leaderboards.html     ← Global + per-hackathon rankings with podium
    │   ├── analytics.html        ← Chart.js: registrations, scores, certificates
    │   ├── certificates.html     ← Certificate issuance and revoke
    │   ├── users.html            ← User management, roles, suspend/restore
    │   └── notifications.html    ← System alerts + broadcast composer
    ├── user/                     ← Role-protected: USER only
    │   ├── dashboard.html        ← Command center (rank, events, quizzes, teams)
    │   ├── events.html           ← Event catalog with filters and registration
    │   ├── quizzes.html          ← PROCTORED quiz: face detection, gaze, tab-switch
    │   ├── my-team.html          ← Team create/join, member management, file submit
    │   └── certificates.html     ← Certificate wallet with preview + PDF download
    └── judge/                    ← Role-protected: JUDGE only
        ├── dashboard.html        ← Overview of assigned hackathons + pending reviews
        ├── hackathons.html       ← My assigned hackathons with scoring progress
        ├── submissions.html      ← Accordion cards: GitHub, files, score button
        ├── scoring.html          ← Split-screen: 5 criteria + feedback + live score ring
        └── results.html          ← Podium + full rankings + criteria expansion
```

---

## Setup with Spring Boot

### Option A — Spring Boot serves the frontend (recommended)

Place this entire folder inside:
```
src/main/resources/static/
```

Spring Boot will serve `index.html` at `http://localhost:8080/`.

Set `BASE_URL` in `js/core.js`:
```js
const BASE_URL = '/api';
```

### Option B — Separate dev server (Vite / Live Server)

Run from the frontend folder:
```bash
python -m http.server 3000
# or: npx live-server --port=3000
```

Set `BASE_URL` in `js/core.js`:
```js
const BASE_URL = 'http://localhost:8080/api';
```

---

## Authentication Flow

1. User logs in at `index.html` → `POST /api/auth/login`
2. Response `{ token, role, name, email }` stored in `localStorage`
3. All subsequent requests inject `Authorization: Bearer <token>` automatically
4. Role-based redirect:
   - `ADMIN`  → `/pages/admin/dashboard.html`
   - `USER`   → `/pages/user/dashboard.html`
   - `JUDGE`  → `/pages/judge/dashboard.html`
5. Each page calls `Auth.guardPage('ROLE')` — wrong role redirects immediately
6. `401/403` responses clear token and redirect to `index.html`

---

## Error Handling

All API errors display a **bottom-right toast notification** (not alert boxes).  
The backend's JSON error DTO `{ status, message, timestamp }` is read and the `message` shown.

---

## Quiz Proctoring

`pages/user/quizzes.html` implements:
- **Face detection** via `face-api.js` (TinyFaceDetector)
- **Gaze tracking** via facial landmarks
- **Tab-switch monitoring** via `document.addEventListener('visibilitychange')`
- Every violation calls `POST /api/quiz/{id}/violation`
- After **5 violations** the session is invalidated and locked

Pass quiz ID via URL: `quizzes.html?id=1&duration=30&pass=60`

---

## API Wiring

All pages contain `/* Real: await api.post(...) */` comments showing exactly where to replace mock data with live API calls. The `api` helper is in `js/core.js`:

```js
api.get('/users/me/dashboard')
api.post('/quiz/1/start', {})
api.post('/hackathon/1/submit', formData)   // multipart
api.download('/certificates/1/download')    // returns Blob → triggers save
```

---

## Design System

| Token | Value |
|-------|-------|
| `--bg-base` | `#0F0F11` |
| `--brand` | `#7C6FCD` |
| `--accent-hackathon` | `#C5A9E8` |
| `--accent-webinar` | `#89C4E1` |
| `--accent-conference` | `#80CDB8` |
| `--accent-quiz` | `#E0A93B` |
| Font (headings) | Plus Jakarta Sans 800 |
| Font (body) | DM Sans |
| Font (code/scores) | DM Mono |

---

## Port Reference

| Service | Default Port |
|---------|-------------|
| Spring Boot backend | `8080` |
| Frontend dev server | `3000` |
