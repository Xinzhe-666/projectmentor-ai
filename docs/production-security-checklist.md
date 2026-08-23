# PMAI Production Security Checklist

Complete this checklist for every production deployment. Store real values only in the server's secret manager or untracked `.env` file.

## Required secrets

- `JWT_SECRET` is unique to the environment, generated from at least 32 random bytes, and is not an example value. Changing it invalidates existing login tokens, so rotate it deliberately.
- `MYSQL_ROOT_PASSWORD` is unique, high entropy, and different from development or template values. Restrict access to the production secret file.
- `AI_API_KEY` is configured only when `AI_ENABLED=true`. If no production key is available, keep AI disabled so requests fail closed instead of repeatedly calling the provider without credentials.

## Network policy

- `CORS_ALLOWED_ORIGINS` contains only the exact production HTTPS origins. Do not include localhost, wildcard origins, raw IP addresses, or plain HTTP.
- MySQL, Redis, and the backend remain on the internal Compose network; only Nginx publishes host ports.
- TLS certificates for `projectmentorai.com` and `www.projectmentorai.com` exist before Nginx starts.

## Release checks

- `VITE_WORKBENCH_EXPERIENCE_ENABLED=true` is present when the frontend image is built. This is a build-time value; changing a running container's environment does not rewrite the frontend bundle.
- Use the standard `docker-compose.yml` for production. Do not use `docker-compose.fast.yml`, which expects a separately prebuilt backend JAR.
- Back up MySQL and inspect `flyway_schema_history` before starting the new backend.
- Run frontend build, backend tests, container health checks, login checks, and Classic/Workbench route smoke tests before declaring the release complete.
