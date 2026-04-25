import axios from "axios";
import toast from "react-hot-toast";
import { useAuthStore } from "@/stores/authStore";

const BASE_URL = import.meta.env.VITE_API_URL || "/api/v1";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
});


// Request interceptor — inject access token
apiClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().tokens?.accessToken;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error),
);

// Response interceptor — handle 401, refresh token
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token) => {
  failedQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(token),
  );
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          return apiClient(original);
        }).catch(err => Promise.reject(err));
      }

      original._retry = true;
      isRefreshing = true;

      const refreshToken = useAuthStore.getState().tokens?.refreshToken;
      if (!refreshToken) {
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        const newToken = data.data.accessToken;
        useAuthStore.getState().setTokens(data.data);
        processQueue(null, newToken);
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      } catch (err) {
        processQueue(err);
        useAuthStore.getState().logout();
        // Chỉ hiện toast 1 lần để tránh spam
        if (!window._isSessionExpiredToasted) {
          window._isSessionExpiredToasted = true;
          toast.error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
          setTimeout(() => { window._isSessionExpiredToasted = false; }, 5000);
        }
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    // Chỉ báo lỗi nếu không phải 401 và không phải 404
    if (error.response?.status !== 401 && error.response?.status !== 404) {
      const message = error.response?.data?.message || "Đã xảy ra lỗi";
      toast.error(message);
    }

    return Promise.reject(error);
  },
);

// Typed API helper
export const api = {
  get: (url, params) => apiClient.get(url, { params }).then((r) => r.data.data),
  post: (url, data, config) => apiClient.post(url, data, config).then((r) => r.data.data),
  put: (url, data, config) => apiClient.put(url, data, config).then((r) => r.data.data),
  patch: (url, data, config) => apiClient.patch(url, data, config).then((r) => r.data.data),
  delete: (url, config) => apiClient.delete(url, config).then((r) => r.data.data),
};

