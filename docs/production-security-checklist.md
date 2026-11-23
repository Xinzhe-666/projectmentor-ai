# PMAI Production Security Checklist

Complete this checklist for every production deployment. Store real values only in the server's secret manager or untracked `.env` file.

## Required secrets

- `JWT_SECRET` is unique to the environment, generated from at least 32 random bytes, and is not an example value. Changing it invalidates existing login tokens, so rotate it deliberately.
- Application startup rejects missing, short, whitespace-containing, legacy fallback and template JWT values. For direct local startup, provide a separate development JWT secret explicitly.
- `MYSQL_ROOT_PASSWORD` is unique, high entropy, and different from development or template values. Restrict access to the production secret file.
- `AI_API_KEY` is configured only when `AI_ENABLED=true`. If no production key is available, keep AI disabled so requests fail closed instead of repeatedly calling the provider without credentials.

## Network policy

- `CORS_ALLOWED_ORIGINS` contains only the exact production HTTPS origins. Do not include localhost, wildcard origins, raw IP addresses, or plain HTTP.
- Verify `SPRING_PROFILES_ACTIVE` includes `prod` before deploying a new backend (the current repository Compose sets it by default). Older server-side Compose files and image-only overrides do not inherit this change. The profile rejects missing/unsafe origins; do not copy local CORS values to production.
- Default Actuator web exposure is health only; the production profile also excludes info, metrics and Prometheus. Keep Nginx's Actuator block and use `/api/health` for the existing health checks. Do not publish the backend's port for monitoring.
- MySQL, Redis, and the backend remain on the internal Compose network; only Nginx publishes host ports.
- TLS certificates for `projectmentorai.com` and `www.projectmentorai.com` exist before Nginx starts.

## Release checks

- `VITE_WORKBENCH_EXPERIENCE_ENABLED=true` is present when the frontend image is built. This is a build-time value; changing a running container's environment does not rewrite the frontend bundle.
- Use the standard `docker-compose.yml` for production. Do not use `docker-compose.fast.yml`, which expects a separately prebuilt backend JAR.
- Back up MySQL and inspect `flyway_schema_history` before starting the new backend.
- Run frontend build, backend tests, container health checks, login checks, and Classic/Workbench route smoke tests before declaring the release complete.
- Check that the running backend uses the existing valid signing secret without printing it. If an example or previously public value was used, rotate it in a planned release; expect active sessions to require a fresh login.
