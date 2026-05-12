FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# npm ci is strict; react-scripts + React 19 often fails peer checks inside Alpine CI.
RUN npm install --legacy-peer-deps --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
