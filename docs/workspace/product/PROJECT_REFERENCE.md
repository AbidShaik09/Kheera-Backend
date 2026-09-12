# Kheera Project Notes

Purpose: compact shared reference for the Kheera frontend, Java backend, and Penpot design. Update this when contracts or screens change.

## Repositories and Runtime

| Area | Location | Stack | Branch / state |
| --- | --- | --- | --- |
| Backend | `Kheera-Backend` | Java 21, Spring Boot 3.3.5, Maven, PostgreSQL, Flyway, Spring Security/JWT, Spring Mail | `develop`; local space/project work is uncommitted |
| Frontend | `Kheera-Frontend` | Angular 21, TypeScript, Angular Material/CDK, Tailwind 4, Vitest | `main`; clean when inspected |
| Design | Penpot: `Kheera / Landing Page` | 1920px desktop boards | Logged-in file; prototype view has 22 frames |

Backend remote: `https://github.com/AbidShaik09/Kheera-Backend`

Frontend remote: `https://github.com/AbidShaik09/Kheera-Frontend`

## Product Model

Kheera is a project-management workspace:

`User -> Space -> Project -> Work Item`

- A Space is a team workspace with members, roles, permissions, and projects.
- A Project has sprint/workflow metadata and work-item types.
- Work items support hierarchy, assignment, comments, attachments, planned/actual dates, and effort.
- Frontend design calls a work item a "Task" in visible UI.

## Backend

### Configuration and infrastructure

- Main application: `src/main/java/com/knightdevelopers/kheerabackend/KheeraBackendApplication.java`; scheduling enabled.
- Environment properties: `JWT_SECRET`, database URL/username/password, `PORT`, SMTP settings.
- JPA schema mode is `validate`; Flyway migrations own the schema.
- Swagger: `/api/swagger-ui.html`.
- Docker production maps host `8080` to container `8080`; dev compose maps `8081` to `8080`.

### Auth contract

| UI operation | Endpoint | Request / response notes |
| --- | --- | --- |
| Log in | `POST /api/auth/login` | Email/password; response body is a raw JWT string. |
| Start sign-up | `POST /api/auth/signup-email` | Sends signup OTP if email is unused. |
| Verify OTP | `POST /api/auth/otp-validation` | Validates email + OTP. |
| Finish sign-up | `POST /api/auth/signup` | User details + OTP; creates account and returns raw JWT. |
| Start reset | `POST /api/auth/forgot-password` | Sends password-reset OTP. |
| Reset password | `POST /api/auth/reset-password` | Valid OTP + new password; returns raw JWT. |
| Current user | `GET /api/users/me` | Uses authenticated JWT subject. |

- JWT is HS256 and expires after 24 hours.
- JWT subject is **email**; `JwtAuthenticationFilter` sets `Authentication.name` to that email.
- Passwords are BCrypt encoded.
- OTPs are six digits and stored with timestamps. Current verification compares the latest OTP but does **not** enforce `expiresAt`; add expiry validation before relying on expiry messaging.
- A scheduled email worker runs every 20 seconds, handles queued email, retries, and marks failed after four attempts.

### Existing secured APIs

- `GET /api/health`: health string.
- `GET /api/users`: list users.
- `GET /api/users/me`: current user; endpoint is permit-all at security level but controller requires authentication to return a user.
- `GET /api/spaces`: in-progress endpoint returning `SpaceListDto(id, name)` for the current user.
- `ProjectController` is currently a placeholder; project APIs are not yet available.

### Important backend issue

`SpaceController` currently calls `UUID.fromString(authentication.getName())`, but `authentication.getName()` is the JWT email subject. This makes `/api/spaces` fail for normal tokens. Resolve the email to a `User` and use its UUID, or deliberately change the JWT subject and all downstream authentication assumptions.

### Important local backend work (do not discard)

Local changes add / modify:

- `SpaceController`, `SpaceService`, `SpaceRepository`, `SpaceMembersRepository`
- `SpaceListDto`, `SpaceSummaryDto`, `ProjectSummaryDto`
- `ProjectController` scaffold
- OTP lookup switched to newest record and value comparison changed to `.equals`

Some are staged and modified again in the worktree. Treat them as user work.

## Frontend

### App shape

- App shell always renders `app-navbar` plus routed content.
- Routes:
  - `/login`: guest-only
  - `/register`: guest-only
  - `/`: authenticated dashboard
  - `/profile`: authenticated
  - `/settings`: authenticated
- `AppInitializer` calls `auth.loginStatus()` at startup.
- Token key: `localStorage.accessToken`.
- `ThemeService` persists light/dark mode and toggles `.dark` on `<html>`.

### Service contracts and caveats

- `AuthService.loginStatus()` requests `users/me`; success marks the logged-in signal true.
- `ApiService` concatenates `apiUrl + url`.
- Backend routes include `/api`, while frontend calls `users/me` without a leading slash. Therefore runtime `window.__config.apiUrl` must include the `/api/` prefix (for example `http://localhost:8080/api/`), or the frontend request paths must be changed.
- Auth interceptor currently sends `Authorization: Bearer null` when no token exists. Skip the header when the token is absent.
- Current login/register/dashboard/profile/settings pages are placeholders; auth forms and API wiring are not implemented yet.

### Existing UI foundation

- Navbar is already present for authenticated use and includes search, create, notifications, theme, settings, profile, and mobile menu controls.
- `KnightBtn` provides Material icon/button styling. Its icon class has `items center`; correct to `items-center` during related work.
- Tailwind styles use a teal + neutral system, which matches Penpot closely.

## Penpot Design

Design file: `Kheera / Landing Page`, desktop boards are approximately `1920 x 1080` / `1920 x 1083`.

### Visual tokens

| Token | Value / role |
| --- | --- |
| Primary light | `#61e7e3` |
| Primary action | `#07cfc9` |
| Background | `#ffffff` |
| Card background | `#f5f7f8` |
| Border | `#e7ecef` |
| Primary text | `#4b5563` |
| Danger / task accent | `#d21616` |

- Style: white work surface, restrained cool gray text and borders, teal actions/progress accents.
- Main desktop application layout: top bar, compact left rail with calendar/favourites/spaces, central content, contextual right information rail.
- Landing/auth uses a white two-column composition: Kheera identity and three-line benefit statement on left; pale form card on right.
- Design includes a Kheera wordmark and stylized K icon. Reuse a provided export if available; do not approximate it with arbitrary text/CSS when implementing branded screens.

### Landing and auth flow

1. **Landing**: wordmark, “Modern Project Workspace”, “Organize Work. Collaborate Better. Deliver Faster.”, and `Get Started`.
   - Prototype action: `Get Started -> Login Board`.
2. **Login**: `Welcome Back`, email, password, `Forgotten Password?`, `Log in`, `Create new account`.
3. **Sign-up Step 1/3**: `Enter your email` + `Send OTP`.
   - Backend: `POST /api/auth/signup-email`.
4. **Sign-up Step 2/3**: `Enter OTP` + `Verify OTP`; includes `Didn't receive it? Resend OTP` and “We've sent a verification code to your email.”
   - Backend: `POST /api/auth/otp-validation`; resend repeats step 1 endpoint.
5. **Sign-up Step 3/3**: name, password, confirm password, `Create New Account`.
   - Backend: `POST /api/auth/signup`.

Keep confirmation-password validation in the frontend. Never send it as the password field by mistake.

### Auth flow implementation state

- Backend supports the auth endpoints above.
- Login and three-step registration are implemented in `Kheera-Frontend/src/app/pages/login` and `Kheera-Frontend/src/app/pages/register`.
- `AuthService` now owns login, signup-OTP, OTP verification, and registration calls; it sends these through the plain-text API method because backend auth responses and errors are raw strings. It stores raw JWTs on successful login/registration and navigates to `/`.
- API endpoint joining is slash-safe. Runtime `apiUrl` must include `/api` (example: `http://localhost:8080/api`).
- The auth interceptor intentionally skips the `Authorization` header when no token exists.
- Password reset is still not implemented in the frontend.
- For route guards, wait for the initializer-driven `loginStatus()` rather than assuming local storage alone is valid.

### Workspace boards

| Board | Information / interactions represented |
| --- | --- |
| Dashboard | Recently Visited grouped by time, Today’s Focus, Activity calendar, Favourites, Spaces, top search/create/action controls. |
| Space Details | Space identity, ID, description, admins, members, project list with project progress and open-task counts. |
| Project Details | Project summary, five task columns: To Do, In Progress, In Review, Done, Blocked; backlog and epic list. |
| Task Details | Detail/editor panel with task title, description, status/check control, and red accent / destructive or close action. |

The backend entity model supports these screens conceptually, but only auth/users and space listing currently have controller-level APIs. Implement backend read models/endpoints before connecting dashboard/project/task UI to real data.

## Recommended Implementation Order

1. Stabilize API base URL and unauthenticated interceptor behavior.
2. Build the complete login, OTP sign-up, and reset-password UI against existing auth APIs.
3. Fix the email/UUID mismatch in space listing; complete spaces read API.
4. Implement the authenticated workspace shell and data-backed sidebar.
5. Add dashboard and space detail APIs/UI.
6. Define project/work-item API contracts, then build project board and task detail UI.

## Test / Verification Checklist

- Backend: Maven tests, Flyway migration validation against an empty PostgreSQL database, auth token request/validation, OTP expiry behavior, authenticated spaces request.
- Frontend: Angular unit tests for auth service/interceptor/guards; verify login, signup steps, logout, theme, desktop and mobile layouts.
- Integration: configure `apiUrl` consistently; test CORS with frontend origin; ensure no `Bearer null`; test expired token sends user to login.
