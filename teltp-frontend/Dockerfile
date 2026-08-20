# syntax=docker/dockerfile:1

# ---- build stage: produce the Angular production bundle ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# ---- serve stage: nginx serves the SPA and reverse-proxies /api to the backend ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/teltp-frontend/browser /usr/share/nginx/html
EXPOSE 80
