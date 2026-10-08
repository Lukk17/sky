### STAGE 1: Build ###
FROM node:26-alpine AS build
WORKDIR /app
COPY apps/frontend/sky-view/package.json apps/frontend/sky-view/package-lock.json ./
# --legacy-peer-deps: primeng 21 declares a peer on Angular ^21 while this app
# runs Angular 22, so strict peer resolution fails inside a clean image.
RUN npm ci --legacy-peer-deps
COPY apps/frontend/sky-view/ ./
RUN npm run build -- --configuration=production

### STAGE 2: Run ###
FROM nginx:alpine
COPY --from=build /app/dist/sky-view/browser /usr/share/nginx/html
COPY apps/frontend/sky-view/nginx.conf /etc/nginx/conf.d/default.conf
COPY apps/frontend/sky-view/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
