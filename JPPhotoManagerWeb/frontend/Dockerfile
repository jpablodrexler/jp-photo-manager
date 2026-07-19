FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci --quiet --ignore-scripts
COPY . .
RUN npm run build:prod

FROM nginx:alpine
COPY --from=build /app/dist/jp-photo-manager-ui/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
