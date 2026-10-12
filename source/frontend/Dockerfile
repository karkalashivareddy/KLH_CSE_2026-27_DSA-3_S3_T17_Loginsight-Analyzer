FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . ./
RUN npm run build

FROM nginx:1.27-alpine AS runtime
RUN addgroup -S app \
    && adduser -S -G app -H -s /sbin/nologin app
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build --chown=app:app /app/dist /usr/share/nginx/html
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["nginx", "-g", "daemon off;"]
