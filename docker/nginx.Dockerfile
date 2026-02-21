# docker/nginx.Dockerfile

# 1) build vue-front
FROM node:20-alpine AS front-build
WORKDIR /app

COPY vue-front/package*.json ./
RUN npm ci

COPY vue-front/ .
RUN npm run build

# 2) nginx serve dist + reverse proxy
FROM nginx:1.27-alpine
COPY backend/nginx/default.conf /etc/nginx/conf.d/default.conf

# dist -> nginx html
COPY --from=front-build /app/dist /usr/share/nginx/html

EXPOSE 80