### STAGE 1: Build ###
FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

### STAGE 2: Run ###
FROM nginx:alpine
COPY --from=build /app/dist/sky-view /usr/share/nginx/html
