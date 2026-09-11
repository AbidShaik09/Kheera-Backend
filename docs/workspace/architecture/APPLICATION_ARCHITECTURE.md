# Kheera Application Architecture

**Last reviewed:** 2026-09-11. This is the whole-system reference. Update it
when a boundary, data flow, integration, storage model, or deployment topology
changes.

## System Shape

```text
Angular web client
  -> HTTPS /api
      -> Nginx reverse proxy
          -> Spring Boot API
              -> PostgreSQL (Flyway-managed schema)
              -> SMTP-backed queued email worker

GitHub Actions self-hosted runners
  -> build/test/deploy containers on Contabo VM
```

## Ownership Boundaries

| Area | Primary responsibility | Canonical documentation |
| --- | --- | --- |
| Angular frontend | Routes, presentation, theme tokens, browser validation, API client, guarded navigation. | [Kheera-Frontend docs](https://github.com/AbidShaik09/Kheera-Frontend/tree/develop/docs) |
| Spring Boot backend | HTTP API, authentication, authorization, business rules, transaction boundaries, persistence access, scheduled work. | `../../` |
| PostgreSQL/Flyway | UUID-based relational data model, schema migration history, database constraints. | `../../architecture/DATABASE_DESIGN.md` |
| Infrastructure | Docker, Nginx, TLS, runtime configuration, PostgreSQL service, self-hosted runners. | `../operations/CONTABO_VM.md` |
| Shared contract | Browser/backend payloads, errors, endpoint state, design-driven required APIs. | `../api/API_CONTRACT.md` |

## Runtime Components

### Frontend

- Angular 21 single-page application with route guards and an HTTP interceptor.
- Runtime API URL is read from public `config.js`; source-controlled examples
  must contain only non-sensitive placeholder values.
- Auth views currently include login, three-stage sign-up, and password reset.
- Shared styling uses token-based CSS/Tailwind conventions. Every new screen
  must support light and dark mode through semantic tokens.

### Backend

- Java 21 and Spring Boot 3.3.5 with Spring Security, JWT, Hibernate/JPA,
  Flyway, Spring Mail, Maven, and PostgreSQL.
- Controller -> service -> repository is the intended application flow.
  Controllers use DTOs; services own authorization and transactional business
  decisions; repositories isolate persistence queries.
- JWT subject is the user email. Authorization must resolve it to server-side
  membership/role data rather than trust browser-provided ownership IDs.
- A scheduled email worker processes queued email and retry state.

### Data and Authentication Flow

```text
Browser credentials / OTP input
  -> auth endpoint
  -> authentication or OTP service
  -> users / one_time_passwords / emails tables
  -> raw JWT response for current auth endpoints
  -> browser auth state and protected API requests
```

Password reset OTP validation currently has a tracked production correctness
issue: [backend #51](https://github.com/AbidShaik09/Kheera-Backend/issues/51).

## Persistence Boundaries

JPA entities remain internal to persistence and service code. The existing user
read endpoints use `UserResponse` DTO projections from `UserRepository`, so
controllers do not serialize `User` entities or traverse lazy associations.
`UserService` owns transactions for user reads and mutations. Production
defaults disable Open Session in View and SQL logging; repository and MVC
coverage for these boundaries is tracked by backend #47 through #49.

## Deployment Topology

- The Contabo VM hosts production and development frontend/backend containers,
  PostgreSQL, and Nginx on a shared Docker network.
- Nginx terminates TLS and proxies public requests to the matching service.
- Database data is persistent and not exposed on a host port by the observed
  configuration.
- Secrets are supplied through runtime configuration and must not be committed,
  copied into documentation, or exposed by logs.

## Architectural Guardrails

- Flyway migrations are the database source of truth; Hibernate validates rather
  than generates production schema.
- The API contract and DTOs are the frontend/backend boundary. Any contract
  drift must be documented and tested before deployment.
- Components communicate through explicit interfaces and contracts, not direct
  cross-layer persistence access or client-supplied authorization fields.
- Security-sensitive flows require expiry, replay, rate-limit, generic-message,
  and logging review appropriate to the risk.
