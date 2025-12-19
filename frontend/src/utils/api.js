// Утилита для определения URL API
export const getApiUrl = () => {
  const envUrl = process.env.REACT_APP_API_URL;
  if (envUrl && typeof envUrl === 'string' && envUrl.trim() !== '') {
    return envUrl;
  }
  
  // Динамически определяем URL на основе текущего хоста
  const host = window.location.hostname;
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
  const port = window.location.port;
  
  // Если фронтенд на порту 3000 или 80, бэкенд на 8080
  // Если фронтенд на другом порту, используем тот же порт для бэкенда (для разработки)
  if (port === '3000' || port === '80' || !port) {
    return `${protocol}//${host}:8080`;
  }
  
  // Для других случаев (например, при разработке на другом порту)
  return `${protocol}//${host}:8080`;
};

export default getApiUrl;

