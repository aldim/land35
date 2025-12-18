# Многостадийная сборка для React приложения

# Стадия сборки
FROM node:18-alpine AS build
WORKDIR /app

# Копируем package.json и package-lock.json для кеширования зависимостей
COPY package*.json ./
RUN npm ci

# Копируем исходный код
COPY . .

# Собираем приложение
# REACT_APP_WS_URL не задаем здесь - используется динамическое определение
# через getWebSocketUrl() в коде, который использует window.location.hostname
RUN npm run build

# Стадия production с nginx для раздачи статических файлов
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Удаляем дефолтные файлы nginx
RUN rm -rf ./*

# Копируем собранное приложение
COPY --from=build /app/build .

# Копируем конфигурацию nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

