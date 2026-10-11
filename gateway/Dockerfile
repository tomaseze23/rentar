# Contexto de build: raíz del repo (docker build -f gateway/Dockerfile .)
FROM node:22-slim AS build
WORKDIR /app
COPY proto ./proto
COPY gateway/package*.json ./gateway/
WORKDIR /app/gateway
RUN npm ci
COPY gateway/ ./
RUN npm run build && npm prune --omit=dev

FROM node:22-slim
WORKDIR /app
COPY proto ./proto
COPY --from=build /app/gateway/node_modules ./gateway/node_modules
COPY --from=build /app/gateway/dist ./gateway/dist
COPY --from=build /app/gateway/src/graphql ./gateway/src/graphql
COPY --from=build /app/gateway/package.json ./gateway/package.json
WORKDIR /app/gateway
ENV NODE_ENV=production
EXPOSE 8080
CMD ["node", "dist/index.js"]