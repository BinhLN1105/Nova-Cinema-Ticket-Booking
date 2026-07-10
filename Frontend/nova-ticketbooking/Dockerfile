# === STAGE 1: Build Phase ===
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build

# === STAGE 2: Run Phase ===
FROM nginx:1.25-alpine
# Copy file cấu hình Nginx tùy chỉnh vào container
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy các tệp tĩnh đã build từ Stage 1 vào thư mục phục vụ của Nginx
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
