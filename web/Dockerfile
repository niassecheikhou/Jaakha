# --- Etape 1 : build Vite ---
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# URL de l'API injectee au build (Vite remplace import.meta.env.* a la compilation).
# Par defaut "/api" : Nginx relaie vers le backend, donc pas de CORS en production.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# --- Etape 2 : service statique Nginx (non-root) ---
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Adresse du backend vue depuis le conteneur web (nom du service compose par defaut).
ENV BACKEND_URL=http://backend:8080

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -q --spider http://127.0.0.1:8080/healthz || exit 1
