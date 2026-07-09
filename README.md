
# 🎓 EduEvent

**A full-stack educational event management platform** — webinars, conferences, hackathons, and webcam-proctored quizzes, with live leaderboards, judge scoring, and auto-issued certificates.

[![Live Demo](https://img.shields.io/badge/demo-live-4f46e5?style=for-the-badge)](https://eduevent-rfgx.onrender.com/)
[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?style=for-the-badge&logo=mongodb)](https://www.mongodb.com/atlas)

**[🔗 Live Demo](https://eduevent-rfgx.onrender.com/)** · **[📘 API Docs (Swagger)](https://eduevent-rfgx.onrender.com/swagger-ui/index.html)** · **[💻 Repository](https://github.com/toobashaikh28/EduEvent)**


---

> ⚠️ **Cold start note:** The live demo runs on Render's free tier, which spins down after ~15 minutes of inactivity. The first request after idle time can take **30–50 seconds** to wake the server — this is a hosting limitation, not an application bug.

---

## ✨ Try It Instantly — No Sign-Up Needed

The live demo is pre-seeded with one account per role:

| Role | Email | Password |
|---|---|---|
| 🛡️ **Admin** | `admin@eduevent.com` | `admin123` |
| ⚖️ **Judge** | `judge@eduevent.com` | `judge123` |
| 👤 **Participant** | `user@eduevent.com` | `user123` |

Log in at **[eduevent-rfgx.onrender.com](https://eduevent-rfgx.onrender.com/)** with any of the above and explore that role's full dashboard. Registering for events, forming a hackathon team, taking a proctored quiz, or scoring a submission — all of it is safe to try; the seed data exists to be explored.

---

## 📖 Overview

Running a real campus event — a student workshop, a hackathon, a Google-Form-and-spreadsheet nightmare — usually means duct-taping together registration forms, WhatsApp groups, and manual certificate generation, with organizers chasing updates across five different apps. **EduEvent replaces all of it with one coherent system** — three role-based portals (Admin, Participant, Judge) covering the full lifecycle of an event, from the first registration to the final certificate — built the way a production event platform actually needs to work:

- Registrations **respect capacity** and automatically waitlist once an event fills up, promoting people the moment a spot opens
- Hackathon teams form themselves via **shareable invite codes**, with leader-approved join requests — no admin has to manually assign anyone to a team
- Teams **communicate right inside the platform** — a leader-posted announcement board that any member can reply to, so project updates and questions don't get lost in a random WhatsApp group
- Quizzes are **proctored in-browser** — live face detection, tab-switch tracking, and a hard 3-strike cutoff that auto-terminates cheating attempts
- Judges score independently and privately; **live standings** are visible to everyone before an admin formally announces winners
- Winning teams get **certificates and notifications generated automatically** the moment results are announced — no one manually designs and emails 30 PDFs
- Every meaningful event — a registration, a win, an admin broadcast — triggers **in-app and email notifications**, so participants never have to go looking for updates
- Admins get a real **analytics dashboard** (registrations, quiz pass rates, participant geography) instead of guessing how an event went from vibes alone

The result: one login, one dashboard per role, and an actual system of record for everything that happens around an event — instead of the usual scramble of forms, screenshots, and "did anyone see my message" messages.

---

## 🚀 Feature Breakdown

### 👤 Participant
- Browse/filter Webinars, Conferences, Hackathons, and Quizzes; register or join the waitlist
- Live waitlist position tracking; automatic promotion when a spot opens
- Create a hackathon team (get an invite code) or join one with a code — leader approves join requests
- Post team announcements (leader) and reply to them (any member) — a lightweight team chat
- Submit hackathon projects: title, description, GitHub link, optional file upload
- Take webcam-proctored quizzes with a live countdown and instant scoring
- View quiz history, download PDF certificates with a public verification QR/link
- Track standings on global and per-hackathon leaderboards
- In-app + email notifications for registrations, results, and admin broadcasts

### ⚖️ Judge
- See only the hackathons they've been explicitly assigned to
- Review every team's submission — description, GitHub repo, uploaded file
- Score each submission out of 10 with written feedback; rescore anytime before results are announced
- Watch **live rankings** update in real time as scores come in

### 🛡️ Admin
- Full event CRUD — banner image upload, capacity, status, optional join links for webinars/conferences
- Quiz builder — multiple-choice questions with per-option correct-answer marking, randomization toggle
- Hackathon control center — view teams, assign/remove judges, review live results, and **announce winners** (which locks the official leaderboard and triggers certificate generation)
- Platform analytics — registrations by event, quiz pass/fail rates, participant city breakdown
- Messaging — role-targeted broadcasts (all/participants/judges/admins) or a direct message to one user by email, both with optional email delivery
- User management — role changes, activate/deactivate accounts

### 🎥 The Proctoring Engine
This is the feature I'm most proud of. Quizzes run fullscreen with:
- **Real-time face detection** (`face-api.js`, TensorFlow.js backend) confirming the test-taker stays in frame
- **Tab-switch / window-blur detection**
- A **3-violation limit** — the 3rd strike locks the screen with a red "Session Terminated" overlay and auto-submits
- Graceful camera-retry handling if webcam access briefly drops

### ⏱️ Background Automation
Two scheduled jobs keep data honest without any manual intervention:
- **`EventScheduler`** — flips events from `UPCOMING` → `LIVE` automatically once their start time passes
- **`QuizSessionScheduler`** — auto-closes and grades quiz sessions that ran past their time limit

---

## 🏗️ Architecture & Tech Stack

**Backend**
- Java 17 · Spring Boot 3.3 · Spring Security (JWT, stateless)
- Spring Data MongoDB · MongoDB Atlas
- Spring Mail (Gmail SMTP) for transactional + broadcast email
- iText 5 (PDF certificate generation) · ZXing (QR-code verification links)
- springdoc-openapi (auto-generated Swagger/OpenAPI docs)

**Frontend**
- Vanilla HTML / CSS / JavaScript — zero framework, zero build step, deployed as Spring Boot static resources
- A custom design system ("dc theme": Plus Jakarta Sans + Space Mono, light-indigo palette) shared across all three dashboards
- `face-api.js` for in-browser proctoring

**Infrastructure**
- Dockerized, deployed on **Render** (auto-deploys on every push to `main`)
- **MongoDB Atlas** M0 (free tier) as the database
- No separate frontend host — the whole app is a single Spring Boot service

### Design Patterns in Practice
This project was also built to demonstrate applied software design (SDA coursework), so the patterns aren't decorative — they solve real problems in the code:

| Pattern | Where | Why |
|---|---|---|
| **Chain of Responsibility** | `chain/EventHandler` + `chain/handlers/` (`AuthorizationHandler`, `DuplicateCheckHandler`, `EventValidationHandler`) | Event creation runs through an ordered validation pipeline instead of one giant `if` block |
| **Mediator** | `mediator/NotificationMediator` | Decouples "something happened" (a win, a new registration) from "who needs to know" |
| **Factory** | `factory/UserFactory` | Centralizes user construction + password encoding so registration logic can't drift out of sync |
| **Null Object** | `pattern/NullUser` implementing `UserInterface` | Avoids null-checks scattered through code paths that expect "a user, possibly absent" |
| **Builder** | Lombok `@Builder` across entities/DTOs | Immutable, readable object construction throughout the domain model |
| **Singleton** | Spring-managed `@Service`/`@Component` beans | The framework's IoC container guarantees one instance per service |

---

## 🗂️ Domain Model

MongoDB collections mirror these core entities:

`User` · `Event` · `Registration` · `Quiz` · `Question` · `Option` · `QuizSession` · `Violation` · `Team` · `TeamMember` · `TeamAnnouncement` · `AnnouncementComment` · `Submission` · `JudgeAssignment` · `Score` · `Leaderboard` · `Certificate` · `Notification` · `SentNotification`

## 📁 Project Structure

```
EduEvent/
├── src/main/java/com/tooba/EduEvent/
│   ├── controller/     # 16 REST controllers (auth, events, quiz, hackathon, judge, admin…)
│   ├── service/         # Business logic (+ impl/)
│   ├── repository/     # Spring Data MongoDB repositories
│   ├── entity/           # 19 MongoDB documents
│   ├── dto/               # request/ and response/ DTOs
│   ├── config/           # Security (JWT + role-based access), CORS
│   ├── chain/             # Chain of Responsibility — event validation pipeline
│   ├── factory/           # Factory pattern — user creation
│   ├── mediator/         # Mediator pattern — notification dispatch
│   ├── pattern/           # Null Object pattern
│   └── scheduler/       # Background jobs (event status, quiz auto-close)
├── src/main/resources/
│   ├── application.properties
│   └── static/
│       ├── index.html          # Landing + login/register
│       ├── user/dashboard.html · quiz.html
│       ├── admin/dashboard.html
│       ├── judge/dashboard.html
│       ├── css/theme.css
│       └── js/core.js          # Shared API client, JWT auth, formatting helpers
├── Dockerfile
└── pom.xml
```

---

## 🔑 Selected API Endpoints

| Category | Endpoint | Notes |
|---|---|---|
| Auth | `POST /api/auth/login`, `/register` | JWT issuance |
| Events | `GET/POST/PUT/DELETE /api/events` | Public GET; admin-only writes; multipart banner upload |
| Registration | `POST/DELETE /api/events/{id}/register` | Waitlists automatically when full |
| Quiz | `POST /api/quiz/{id}/start`, `/session/{id}/submit`, `/session/{id}/violation` | Full proctored session lifecycle |
| Hackathon | `POST /api/hackathon/{id}/team/create`, `/team/join` | Invite-code team formation |
| Judging | `POST /api/judge/score` | 1–10 score + feedback per submission |
| Results | `GET /api/hackathon/{id}/results` | Public — live standings pre-announcement, official leaderboard after |
| Certificates | `GET /api/certificates/{id}/download`, `/verify/{uuid}` | PDF download + public verification |
| Admin messaging | `POST /api/admin/broadcast`, `/message-by-email` | Role-targeted or one-to-one, with optional email |

**Full interactive documentation:** every endpoint, request/response shape, and a live "Try it out" console is available at **[`/swagger-ui/index.html`](https://eduevent-rfgx.onrender.com/swagger-ui/index.html)** — generated automatically from the code, no manual docs to maintain.

---

## ⚙️ Running Locally

### Prerequisites
- Java 17+, Maven 3.9+
- A MongoDB instance — local or [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (free tier is enough)
- A Gmail account with an [App Password](https://myaccount.google.com/apppasswords) generated (for the email features)

### Steps

```bash
git clone https://github.com/toobashaikh28/EduEvent.git
cd EduEvent
```

Set the following environment variables (this project uses `spring-dotenv`, so a `.env` file in the project root works too):

```env
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/eduevent
JWT_SECRET=<a-long-random-string>
MAIL_USERNAME=<your-gmail-address>
MAIL_PASSWORD=<your-gmail-app-password>
```

Run it:

```bash
mvn spring-boot:run
```

Open **`http://localhost:8080`**. The three seeded accounts (see credentials table above) are created automatically on first boot.

### Running with Docker

```bash
docker build -t eduevent .
docker run -p 8080:8080 \
  -e MONGODB_URI=<your-uri> \
  -e JWT_SECRET=<your-secret> \
  -e MAIL_USERNAME=<your-email> \
  -e MAIL_PASSWORD=<your-app-password> \
  eduevent
```

---

## ☁️ Deployment

The live instance runs as a **Dockerized web service on Render**, connected directly to this repository — every push to `main` triggers an automatic build and redeploy. The database is **MongoDB Atlas** (M0, free tier).

---

## 👩‍💻 About

EduEvent started as a Software Design & Architecture course project and grew into something I actually use — it now runs real event workflows end-to-end, from registration through hackathon judging to certificate delivery, for a university student organization.

**Tooba Shaikh**
Software Engineering, Sir Syed University of Engineering and Technology

---

## 📄 License

Built for academic and organizational use. Explore, learn from it, and adapt it for your own student community's events.