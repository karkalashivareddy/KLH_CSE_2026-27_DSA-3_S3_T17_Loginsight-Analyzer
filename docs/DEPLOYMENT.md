# Deployment

The repository includes a single-node Docker Compose deployment for evaluation and portfolio use. It is not a production platform: the backend is stateful only in memory, the frontend has no authentication layer and TLS/secret management belong outside this Compose file.

## Files

- `backend/Dockerfile`: multi-stage Maven build and non-root Java 21 runtime.
- `frontend/Dockerfile`: multi-stage Node build and non-root Nginx runtime.
- `frontend/nginx.conf`: static SPA server, `/api/` reverse proxy, SSE-friendly buffering and SPA fallback.
- `docker-compose.yml`: backend/frontend services, bridge network, healthchecks and host ports.
- `.dockerignore`, `backend/.dockerignore`, `frontend/.dockerignore`: build-context exclusions.
- `.env.example`: optional host-port and Compose project settings.

## Start

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

Open `http://localhost:8088`. The default mappings are:

| Service | Container port | Default host port |
|---|---:|---:|
| Backend API | 8080 | 8080 |
| Frontend Nginx | 8080 | 8088 |

Override `BACKEND_PORT` or `FRONTEND_PORT` in `.env` when those ports are occupied. Stop with `docker compose down`; add `-v` only if a future volume is introduced.

## Request routing

The browser always calls relative `/api` paths.

### Vite development

`frontend/vite.config.ts` preserves the local development proxy:

```text
browser /api -> Vite :5173 -> Spring Boot :8080
```

This proxy is intentionally separate from the deployment configuration and is not removed by the container build.

### Nginx deployment

Nginx listens on container port 8080 and routes:

- `/healthz` to a local `200 ok` response used by the frontend container healthcheck.
- `/api/` to `http://backend:8080` with the original URI preserved.
- Every other path to the built Vite assets, with `/index.html` as the SPA fallback.

The API location disables response and request buffering, uses HTTP/1.1 upstream keepalive and allows long-lived SSE responses. It does not add WebSocket upgrade handling because WebSockets are not implemented by the application.

## Healthchecks

Compose waits for the backend healthcheck before starting Nginx:

- Backend: `GET http://127.0.0.1:8080/api/health`.
- Frontend: `GET http://127.0.0.1:8080/healthz`.

The backend image installs `curl` for its probe. The Alpine Nginx image includes BusyBox `wget` for its probe. Healthchecks indicate process availability, not dataset readiness; use `/api/health/status` and `/api/health/dataset` for application state.

## Images

The backend builder downloads Maven dependencies and packages the Spring Boot jar. Compose intentionally uses the repository root as its backend build context so the sibling `sample-data/` directory can be copied into the runtime image. A direct equivalent build is `docker build -f backend/Dockerfile -t loginsight-backend:local .`; the frontend build context is `frontend`. The runtime image contains the JRE, the jar and the committed `sample-data/` directory. `LOGINSIGHT_SAMPLE_DATA_DIR=/app/sample-data` overrides the local relative sample path. Both runtime stages run as non-root users.

The frontend builder runs `npm ci` and `npm run build` on Node 22. Nginx serves `/app/dist` from the runtime image and runs as a non-root user on port 8080.

## Persistence and scaling

There is no database or persistent volume. A backend restart clears the current dataset and the 64-entry run history. Run one backend replica unless the application is redesigned around shared state. Multiple Nginx replicas can serve the same frontend, but multiple backend replicas would not share datasets or runs.

## Operations and security notes

- Put the frontend behind a TLS-terminating reverse proxy or load balancer for any non-local deployment.
- Add authentication, authorization, request-rate limits, secret management and durable storage before exposing the API beyond a trusted environment.
- The 64 MB upload limit is enforced by Spring and mirrored by Nginx; larger datasets are not supported by this deployment shape.
- The Demo Replay screen (route `/live`, navigation label `Live Replay`) is a bounded, oldest-first replay of the active dataset, not an external stream consumer. The Command Center topology and its 2.5D CSS depth mode are client-side SVG rendering and need no GPU, WebGL capability or extra container resource.
- Container image builds and runtime smoke tests were not available in the audit environment because the Docker daemon was not running. `docker compose config` did validate the Compose model. That limitation still holds, so the images remain unbuilt and unverified on this branch.

## Useful commands

```powershell
docker compose config
docker compose build
docker compose up -d
docker compose logs -f backend
docker compose logs -f frontend
curl http://localhost:8080/api/health
curl http://localhost:8088/healthz
```
