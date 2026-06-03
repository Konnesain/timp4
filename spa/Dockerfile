FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine
RUN apk add --no-cache tzdata
ENV TZ=Asia/Novosibirsk
WORKDIR /app
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/vite.config.js .
COPY server.crt server.key ./
EXPOSE 2000
CMD ["npx", "vite", "preview", "--host", "0.0.0.0", "--port", "2000"]
