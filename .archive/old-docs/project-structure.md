# FocusHive — Project Structure Guide  
_A lean Spring Boot + React web implementation (no desktop/mobile clients)_

---

## 1. Top-level layout

```
focushive/
├─ backend/ # Java 21 / Spring Boot 3.x source
├─ frontend/ # React + TypeScript web client
├─ shared/ # Contracts and generators common to both tiers
├─ docker-compose.yml # One-command local stack
├─ .github/workflows/ # CI pipelines (build, test, Docker push)
└─ docs/ # Design notes, diagrams, sprint journal
```

---

## 2. Back-end (`backend/`)

| Path                               | Purpose                                                |
| ---------------------------------- | ------------------------------------------------------ |
| `backend/api/`                     | **Spring Boot app** (multi-module friendly)            |
| `api/src/main/java/com/focushive/` | Java packages                                          |
| &nbsp;&nbsp;• `hive/`              | Domain: `Hive`, `HiveMember`, `FocusSession`, `Streak` |
| &nbsp;&nbsp;• `user/`              | Auth & profile (`User`, `Role`, JWT filters)           |
| &nbsp;&nbsp;• `websocket/`         | STOMP config + controllers for real-time presence      |
| &nbsp;&nbsp;• `common/`            | Exceptions, DTO mappers, utility classes               |
| `api/src/main/resources/`          | `application.yml`, Flyway migrations                   |
| `Dockerfile`                       | Builds OCI image via Spring Boot Buildpacks            |
| `build.gradle.kts`                 | Gradle build (Java 21, Spring Dependency Mgmt)         |

**Key starters**

* `spring-boot-starter-web`
* `spring-boot-starter-websocket`
* `spring-boot-starter-data-jpa`
* `spring-boot-starter-security`
* `springdoc-openapi-starter-webmvc-ui`

---

## 3. Front-end (`frontend/`)

| Path                   | Purpose                                            |
| ---------------------- | -------------------------------------------------- |
| `frontend/web/`        | **React (Vite)** single-page app                   |
| `web/src/app/`         | App shell, routing, global store initialisation    |
| `web/src/features/`    | Feature-sliced modules                             |
| &nbsp;&nbsp;• `hive/`  | Hive board, timers, presence grid                  |
| &nbsp;&nbsp;• `auth/`  | Login, register, JWT refresh logic                 |
| `web/src/shared/`      | Reusable pieces                                    |
| &nbsp;&nbsp;• `api/`   | _Generated_ RTK Query endpoints from OpenAPI       |
| &nbsp;&nbsp;• `hooks/` | `usePresence`, `useAuth`, etc.                     |
| &nbsp;&nbsp;• `ui/`    | Tailwind + shadcn/ui components                    |
| `vite.config.ts`       | Path aliases, env vars, proxy `/api` → Spring Boot |
| `package.json`         | Scripts (`dev`, `build`, `lint`, `test`)           |

Tech stack: **React 18 + TypeScript + Tailwind CSS + Zustand (or Redux Toolkit)**.

---

## 4. Shared contracts (`shared/`)

```
shared/
├─ openapi/
│ └─ hive.yaml # Single source of truth for REST schema
├─ protos/ # (optional) gRPC/Kafka schemas
└─ scripts/ # Code-gen helpers (OpenAPI Generator CLI)
```

* CI step runs `openapi-generator` to create:
  * Java interfaces (`backend/api-client/`)
  * Typed TS clients (`frontend/web/src/shared/api/`)

---

## 5. Local development stack

`docker-compose.yml`

```yaml
services:
  db:        # PostgreSQL 16
  redis:     # Presence pub/sub
  backend:   # Spring Boot (built from ./backend)
  web:       # Vite dev server (hot reload)
```

1. `docker compose up --build`
2. Visit **[http://localhost:5173](http://localhost:5173/)** (React) and **/swagger-ui.html** for API docs.

------

## 6. Continuous Integration (GitHub Actions)

```
.github/workflows/ci.yml
```

- **Jobs**
  1. `backend-test` → Gradle unit & integration tests (Testcontainers)
  2. `frontend-test` → Vitest + React Testing Library
  3. `docker-build-push` → Buildpacks image, push to GHCR
- Status checks must pass before PR merge.

------

## 7. development timeline

| Phase      | Milestone                                |
| ---------- | ---------------------------------------- |
| **W1–2**   | Skeleton apps + health check             |
| **W3–4**   | JWT auth, hive CRUD                      |
| **W5–6**   | Redis pub/sub + WebSocket presence       |
| **W7–8**   | Gamification (streaks, goals)            |
| **W9–10**  | Analytics dashboard & polish             |
| **W11–12** | Performance test, final report, demo day |