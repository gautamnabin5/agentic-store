# Agentic Store — Agent Instructions

## Project Overview

Fullstack e-commerce/store application with an agentic/AI-driven twist.

- **Frontend**: React + shadcn/ui (Vite)
- **Backend**: Java Spring Boot (REST API)
- **Database**: PostgreSQL
- **Deployment**: All layers are dockerized; frontend served via Nginx

## Repository Structure

```
agentic-store/
├── frontend/          # React + Vite + shadcn/ui
├── backend/           # Spring Boot (Maven or Gradle)
├── docker/            # Shared Docker assets (init SQL, nginx config, etc.)
├── docs/
│   ├── specs/         # Design specs — one file per feature/domain, named YYYY-MM-DD-<topic>-design.md. This is committed to source control so new/exsting features can reference it for knowledge / memory 
│   └── plans/         # Implementation plans — one file per feature, named YYYY-MM-DD-<topic>-plan.md. Do not reference this if the plan is unrelated to what is currently being built. Use the specs files for that since this is not in git history
├── docker-compose.yml # Orchestrates all services locally
└── CLAUDE.md
```

## Tech Stack

### Frontend (`frontend/`)
- React 18+, TypeScript
- Vite as build tool
- shadcn/ui for component library (Tailwind CSS under the hood)
- React Router for navigation
- Fetch or Axios for API calls
- Production: Nginx serving the static build

### Backend (`backend/`)
- Java 21+
- Spring Boot 3.x
- Spring Data JPA + Hibernate
- Spring Security (for auth if needed)
- Maven (`pom.xml`) as build tool
- Exposed on port `8080`

### Database
- PostgreSQL 16+
- Managed via Docker in development
- Schema migrations via Flyway (`backend/src/main/resources/db/migration/`)

## Development Setup

### Prerequisites
- Docker + Docker Compose
- Node.js 20+ / npm
- Java 21+ / Maven

### Running Locally

```bash
# Start all services
docker compose up --build

# Or run each layer independently:

# Database only
docker compose up db

# Backend (from backend/)
./mvnw spring-boot:run

# Frontend (from frontend/)
npm install
npm run dev
```

Service ports (default):
| Service   | Port  |
|-----------|-------|
| Frontend  | 5173 (dev) / 80 (Docker) |
| Backend   | 8080  |
| PostgreSQL| 5432  |

## Docker

Each layer has its own `Dockerfile`:

- `frontend/Dockerfile` — multi-stage: `node` build → `nginx:alpine` serve
- `backend/Dockerfile` — multi-stage: `maven` build → `eclipse-temurin` run
- `docker-compose.yml` — wires all three together with a shared network

Environment variables are passed via `.env` (never committed) or Docker Compose `environment:` blocks.

## Environment Variables

### Backend (`backend/src/main/resources/application.properties` or `.env`)
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=agenticstore
DB_USERNAME=postgres
DB_PASSWORD=secret
```

### Frontend
```
VITE_API_BASE_URL=http://localhost:8080
```

## Code Conventions

### Frontend
- Components in `frontend/src/components/` — one file per component
- Pages in `frontend/src/pages/`
- Shared hooks in `frontend/src/hooks/`
- API layer in `frontend/src/api/`
- Use shadcn/ui primitives; do not reimplement what shadcn already provides
- TypeScript strict mode on

### Backend
- Architecture must follow SOLID principles — single responsibility per class, dependencies injected via interfaces, open for extension without modifying existing classes
- Standard Spring layering: `controller` → `service` → `repository`
- DTOs in `dto/`, entities in `model/` or `entity/`
- REST endpoints under `/api/v1/`
- Return `ResponseEntity<T>` from controllers
- All service methods return `Result<T>` (a sealed type wrapping success/failure) — never throw checked exceptions across layer boundaries; let the controller unwrap and map to HTTP status
- Use `@Transactional` at the service layer, not controller

### Database
- All schema changes go through Flyway migrations, never raw DDL
- Migration files: `V{n}__{description}.sql`
- Snake_case for table and column names

## Testing

### Frontend
```bash
cd frontend && npm test
```

### Backend
```bash
cd backend && ./mvnw test
```

- Spring Boot: use `@SpringBootTest` for integration tests, `@WebMvcTest` for controller unit tests
- Use `Testcontainers` for database-backed tests (spin up a real PostgreSQL container)

## Git Workflow

- Branch from `main`
- Feature branches: `feature/<short-description>`
- Bug fixes: `fix/<short-description>`
- PRs must pass all tests before merge
- Never commit `.env` files or secrets

## UI Design Philosophy

Derived from Figma's core UI design principles. Apply these when building or reviewing any frontend component or page.

### 1. Hierarchy
Guide users to the most important information first. Use font size, weight, contrast, and spacing intentionally. Be deliberate about what appears above the fold versus what requires scrolling.

### 2. Progressive Disclosure
Don't overwhelm users with everything at once. Reveal complexity gradually — especially in multi-step flows and onboarding. Always orient the user: show progress and what remains.

### 3. Consistency
Identical components behave identically everywhere. Buttons, inputs, and patterns should look and work the same across all pages. Any visual deviation signals intentional meaning — don't create accidental variation. Use shadcn/ui primitives to enforce this automatically.

### 4. Contrast
Use contrast to direct attention. High contrast for critical actions or information; muted tones for secondary actions. Destructive actions (delete, remove) use warning colors; safe/passive actions stay neutral.

### 5. Accessibility
Follow WCAG 2.1 AA as a baseline. Every interactive element must be keyboard-navigable. All images need alt text. Color alone must never be the only signal. Test with shadcn/ui's built-in ARIA support — don't remove accessibility attributes.

### 6. Proximity
Visually group what belongs together. Related controls sit close to each other; unrelated controls have clear separation. Prevents accidental mis-clicks and communicates relationship without labels.

### 7. Alignment
Use a grid. Everything aligns to something. Consistent spacing scale (Tailwind's spacing system) — avoid arbitrary pixel values. Clean alignment signals professionalism and makes layouts predictable.

## Documentation Conventions

All design and planning documents live in `docs/` and are committed to git.

- **Before implementing any non-trivial feature**: check `docs/specs/` for an existing spec. If none exists, create one before writing code.
- **Specs** (`docs/specs/YYYY-MM-DD-<topic>-design.md`): capture the *what* and *why* — data model, API surface, business rules, constraints, out-of-scope decisions.
- **Plans** (`docs/plans/YYYY-MM-DD-<topic>-plan.md`): capture the *how* — ordered implementation steps, file targets, migration names, test strategy.
- Keep docs up to date as decisions change. A stale doc is worse than no doc — update or delete it.
- Do not write ephemeral notes or task lists into `docs/`; those belong in the conversation or a task tracker.

## Notes for AI Agents

- Prefer modifying existing files over creating new ones
- Do not add unnecessary comments or documentation unless the WHY is non-obvious
- Keep responses concise; skip trailing summaries
- When touching the frontend, always verify shadcn/ui has a component before building a custom one
- When touching the backend, respect the controller → service → repository separation
- Database changes always go through Flyway — never alter schema directly
