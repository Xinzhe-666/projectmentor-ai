FROM node:lts-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_WORKBENCH_EXPERIENCE_ENABLED=false
ENV VITE_WORKBENCH_EXPERIENCE_ENABLED=${VITE_WORKBENCH_EXPERIENCE_ENABLED}
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
