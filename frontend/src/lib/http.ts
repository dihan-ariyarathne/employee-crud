import axios from 'axios';
import { auth } from './firebase.ts';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Attach Firebase ID token if logged in
http.interceptors.request.use(async (config) => {
  try {
    const user = auth.currentUser;
    if (user) {
      const token = await user.getIdToken();
      if (token) {
        config.headers = config.headers ?? {};
        (config.headers as any).Authorization = `Bearer ${token}`;
      }
    }
  } catch {
    // ignore token fetch errors; request proceeds unauthenticated
  }
  return config;
});

// Refresh ID token once on 401 and retry the request
http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { response, config } = error || {};
    if (response && response.status === 401 && config && !(config as any)._retry) {
      try {
        const user = auth.currentUser;
        if (user) {
          const fresh = await user.getIdToken(true);
          if (fresh) {
            (config as any)._retry = true;
            config.headers = config.headers ?? {};
            (config.headers as any).Authorization = `Bearer ${fresh}`;
            return http.request(config);
          }
        }
      } catch {
        // fall through to reject
      }
    }
    return Promise.reject(error);
  }
);

export default http;
