# Contabo VM Notes

Read-only infrastructure inventory performed on 2026-09-11 after the Kheera TLS recovery. This document records observed configuration only; it intentionally omits passwords, `.env` values, runner credentials, and database contents.

## Server Summary

| Item | Observed value |
| --- | --- |
| Provider / host | Contabo, `vmi3323323` |
| Public IPv4 | `161.97.102.94` |
| Platform | KVM VM, Ubuntu 24.04.4 LTS, Linux `6.8.0-106-generic`, x86_64 |
| Resources at audit | 145 GB root disk: 20 GB used (14%); 7.8 GiB RAM: 1.8 GiB used; no swap |
| Uptime at audit | 109 days; OS reported a restart required and 49 pending package updates |
| Docker | Docker `29.5.2`, Docker Compose `v5.1.4` |
| Certbot | `2.9.0` |

## Filesystem Layout

`/opt/apps` is the deployment root.

```text
/opt/apps
  backups/                 # Empty when inspected
  config/kheera/           # Runtime frontend config files, prod + dev
  data/postgres/           # Persistent PostgreSQL data volume
  develop/                 # Development deployment worktrees
    kheera-backend/Kheera-Backend/
    kheera-frontend/Kheera-Frontend/
  infra/
    nginx/                 # Docker Compose + reverse-proxy config
    postgres/              # Docker Compose
  logs/
  prod/                    # Production deployment worktrees
    kheera-backend/Kheera-Backend/
    kheera-frontend/Kheera-Frontend/
  runners/kheera-prod/     # Self-hosted GitHub Actions runners
    backend/actions-runner/
    ui/actions-runner/
  scripts/                 # Empty when inspected
```

## Container Topology

All application services join the external Docker bridge network `app-network` (`172.18.0.0/16`). It contains Nginx, PostgreSQL, both frontend containers, and both backend containers.

| Service / container | Role | Host mapping | Internal target |
| --- | --- | --- | --- |
| `nginx` | TLS termination and reverse proxy | `80:80`, `443:443` | Proxies by container name on `app-network` |
| `kheera-frontend` | Production Angular static site | `4200:80` | `kheera-frontend:80` |
| `kheera-frontend-dev` | Development Angular static site | `4201:80` | `kheera-frontend-dev:80` |
| `kheera-backend` | Production Spring Boot API | `8080:8080` | `kheera-backend:8080` |
| `kheera-backend-dev` | Development Spring Boot API | `8081:8080` | `kheera-backend-dev:8080` |
| `postgres` | PostgreSQL 16 | No host port published | `postgres:5432` |

- All listed containers were running during the audit and use `restart: unless-stopped`.
- The host firewall explicitly allows SSH (`22`), HTTP (`80`), and HTTPS (`443`) and denies other inbound traffic by default.
- The non-proxy application ports are nevertheless published by Docker. Docker's iptables rules can bypass UFW in some configurations, so externally test `4200`, `4201`, `8080`, and `8081` before assuming they are blocked.

## Reverse Proxy and Routing

Active host source file: `/opt/apps/infra/nginx/nginx.conf`.

The Nginx container mounts:

- `/opt/apps/infra/nginx/nginx.conf` -> `/etc/nginx/nginx.conf` (read-only)
- `/etc/letsencrypt` -> `/etc/letsencrypt` (read-only)

HTTP requests for all configured names redirect to HTTPS. HTTPS routing is:

| Hostname | Upstream |
| --- | --- |
| `theknightdevelopers.online` | Static `200 The Knight Developers` response |
| `kheera.theknightdevelopers.online` | `kheera-frontend:80` |
| `dev.kheera.theknightdevelopers.online` | `kheera-frontend-dev:80` |
| `api.kheera.theknightdevelopers.online` | `kheera-backend:8080` |
| `dev.api.kheera.theknightdevelopers.online` | `kheera-backend-dev:8080` |

The proxy config uses the shared certificate paths:

```text
/etc/letsencrypt/live/theknightdevelopers.online/fullchain.pem
/etc/letsencrypt/live/theknightdevelopers.online/privkey.pem
```

## TLS / Certbot

### Incident and repair

The previous certificate expired on 2026-09-06 because Certbot renewal used the `standalone` authenticator while the Docker Nginx proxy already owned port 80. Scheduled Certbot runs failed with `Could not bind TCP port 80`.

The current certificate was renewed successfully on 2026-09-11. It covers all five hostnames and expires on **2026-12-10 03:56 UTC**.

### Persistent renewal behavior

`certbot.timer` is enabled and active. To allow standalone HTTP-01 validation, two executable hooks are installed:

```text
/etc/letsencrypt/renewal-hooks/pre/stop-nginx
/etc/letsencrypt/renewal-hooks/post/start-nginx
```

They stop the Docker `nginx` container before renewal and start it afterwards. This causes a short web-service interruption only during a certificate renewal attempt, but prevents the prior port-80 collision from recurring.

Verification after renewal:

- Certificate state: valid, 89 days remaining at check time.
- `nginx` container: running and mapped to 80/443.
- `https://kheera.theknightdevelopers.online`: `HTTP/1.1 200 OK`.

## Docker Build and Runtime Configuration

### Backend

- Dockerfile: Eclipse Temurin JDK 21, copies `target/*.jar`, runs `java -jar app.jar`.
- Production compose uses `.env`, publishes `8080`, and joins `app-network`.
- Development compose uses `.env.dev`, publishes `8081`, and joins `app-network`.

### Frontend

- Dockerfile: Node 22 Alpine build stage (`npm install`, production Angular build), then Nginx Alpine runtime stage.
- Production compose publishes `4200` and mounts `/opt/apps/config/kheera/prod-config.js` into the generated site as `config.js`.
- Development compose publishes `4201` and mounts `/opt/apps/config/kheera/dev-config.js` similarly.
- The inspected development Dockerfile has the same production-build pattern as the production Dockerfile; it is not an Angular live-reload development server.

### PostgreSQL

- PostgreSQL 16 stores data at `/opt/apps/data/postgres`.
- It is only exposed on the Docker network, not through a host port.
- Its compose file contains an inline database credential rather than an environment file or secret store. The value is intentionally not recorded here.

## CI/CD and Deployment

Two GitHub Actions self-hosted runners are installed as active systemd services and run as user `abid`:

```text
actions.runner.AbidShaik09-Kheera-Backend.prod-kheera-backend.service
actions.runner.AbidShaik09-Kheera-Frontend.kheera-prod-ui.service
```

Runner directories:

```text
/opt/apps/runners/kheera-prod/backend/actions-runner
/opt/apps/runners/kheera-prod/ui/actions-runner
```

Workflow definitions live in both deployment worktrees under `.github/workflows/`.

| Component | Branch trigger | Deployment directory | Deployment action |
| --- | --- | --- | --- |
| Backend production | `main` | `/opt/apps/prod/kheera-backend/Kheera-Backend` | `git fetch origin main`; `git merge --ff-only origin/main`; Maven package with tests enabled; `docker compose down/up -d --build` |
| Backend development | `develop` | `/opt/apps/develop/kheera-backend/Kheera-Backend` | `git fetch origin develop`; `git merge --ff-only origin/develop`; Maven package with tests enabled; dev compose down/up with build |
| Frontend production | `main` | `/opt/apps/prod/kheera-frontend/Kheera-Frontend` | `git fetch`; `git reset --hard origin/main`; compose down/up with build; image prune |
| Frontend development | `develop` | `/opt/apps/develop/kheera-frontend/Kheera-Frontend` | `git fetch`; `git reset --hard origin/develop`; dev compose down/up with build; image prune |

The frontend also has a manual `workflow_dispatch` runner test that prints host, working directory, user, and Docker version.

PR #63 replaces wildcard credentialed CORS with exact browser-origin allowlists.
Production Compose defaults to `https://kheera.theknightdevelopers.online`;
development Compose defaults to `https://dev.kheera.theknightdevelopers.online`.
Override Compose interpolation with `CORS_ALLOWED_ORIGINS` for production or
`DEV_CORS_ALLOWED_ORIGINS` for development in the invoking shell or Compose
environment file (`--env-file`); values are comma-separated exact origins.
Compose passes the result as `CORS_ALLOWED_ORIGINS` to the backend container.
Standalone Java startup reads `CORS_ALLOWED_ORIGINS` directly and otherwise
allows only `http://localhost:4200`. The settings are implemented on the PR;
deployment and deployed-browser smoke verification remain pending merge.
Backend PR #62 adds a separate `Verify Backend` pull-request workflow on
GitHub-hosted Ubuntu with Java 21 and PostgreSQL Testcontainers. It runs Maven
`verify` with read-only repository permissions and no deployment credentials.
Local Docker-backed verification also passed on 2026-09-12. Backend #55 adds
workflow concurrency, pinned deployment checkout actions, environment-scoped
deployment jobs, test-enabled Maven package, and fast-forward-only deployment
updates.

## Observed Repository State

| Worktree | Observed branch / HEAD | Note |
| --- | --- | --- |
| Production backend | `main`, `6f400a7` | No status output observed. |
| Production frontend | `main`, `62bb9d7` | No status output observed. |
| Development backend | `develop-deployment`, `535db8d` | Tracking report: 44 commits ahead of `origin/develop-deployment`; its HEAD was also labelled `origin/develop` in the log. This is a deployment-state inconsistency. |
| Development frontend | `main`, `0e5444a` | Tracking report: 6 commits ahead and 4 behind `origin/main`; HEAD was also labelled `origin/develop`. This conflicts with the workflow expectation that development resets to `origin/develop`. |

Backend deployment remotes use GitHub SSH; production frontend used an HTTPS remote during the audit, while development frontend used SSH. This difference can affect unattended `git pull/fetch` behavior and should be standardized.

## Scheduled Work and Backups

- `certbot.timer` is active and is the only Kheera-specific observed scheduled mechanism.
- Root crontab was empty.
- `/opt/apps/scripts` was empty.
- `/opt/apps/backups` contained no files at audit time.
- Standard Ubuntu maintenance timers (APT, logrotate, fstrim, etc.) are active.

## Findings and Follow-up Priorities

1. **[P1] Rotate exposed credentials.** SSH/sudo credentials were shared during incident work and should be changed. Move the PostgreSQL inline credential to an environment file or secret mechanism, then rotate it as a planned database change.
2. **[P1] Restore a backup strategy.** The designated backup directory was empty. Define automated PostgreSQL logical backups, off-host retention, encryption, and restore tests.
3. **[P1] Reconcile development worktrees.** The observed branch/remote divergence can cause deployments to build a different revision than expected. Resolve it before relying on the next development deployment.
4. **[P2] Verify firewall behavior for Docker-published ports.** Confirm from an external network that direct 4200/4201/8080/8081 access is blocked; if not, remove host port mappings or add Docker/UFW policy.
5. **[P2] Finish cross-repository CI hardening.** Backend deployment workflows now build with tests enabled; frontend PR/deployment checks and repository branch-protection settings still need owner/admin verification.
6. **[P2] Pin infrastructure images and add health checks.** `nginx:latest` is unpinned and compose services have no health checks. Pin versions and define readiness checks.
7. **[P3] Plan maintenance reboot.** The VM has been up for 109 days and reports a required reboot plus pending updates. Schedule a maintenance window.
8. **[P3] Consider a no-downtime ACME method.** The current pre/post hooks solve renewal correctly but briefly stop Nginx. A webroot challenge mounted into the proxy or a dedicated ACME-capable proxy would avoid that interruption.

## Useful Read-only Checks

```bash
sudo docker ps
sudo docker network inspect app-network
sudo certbot certificates
sudo systemctl status certbot.timer --no-pager
sudo systemctl status actions.runner.AbidShaik09-Kheera-Backend.prod-kheera-backend.service --no-pager
sudo systemctl status actions.runner.AbidShaik09-Kheera-Frontend.kheera-prod-ui.service --no-pager
curl -I https://kheera.theknightdevelopers.online
sudo ufw status verbose
```

Do not print `.env`, runner `.credentials`, private keys, or database dumps into a terminal transcript or this document.
