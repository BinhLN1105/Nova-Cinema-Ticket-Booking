import { api } from "./client";

// ── Auth ──────────────────────────────────────
export const authApi = {
  login: (email, password) => api.post("/auth/login", { email, password }),
  register: (data) => api.post("/auth/register", data),
  socialLogin: ({ idToken, provider }) =>
    api.post("/auth/social-login", { idToken, provider }),
  logout: (refreshToken) => api.post("/auth/logout", { refreshToken }),
  refresh: (refreshToken) => api.post("/auth/refresh", { refreshToken }),
  me: () => api.get("/auth/me"),
};

// ── Movies ────────────────────────────────────
export const movieApi = {
  getAll: (params) => api.get("/movies", params),
  getNowShowing: (page = 0) =>
    api.get("/movies", { status: "NOW_SHOWING", page, size: 12 }),
  getComingSoon: (page = 0) =>
    api.get("/movies", { status: "COMING_SOON", page, size: 12 }),
  getById: (id) => api.get(`/movies/${id}`),
  search: (q, page = 0) => api.get("/movies/search", { q, page, size: 12 }),
  getGenres: () => api.get("/movies/genres"),
  // Admin
  create: (data) => api.post("/movies", data),
  update: (id, data) => api.put(`/movies/${id}`, data),
  delete: (id) => api.delete(`/movies/${id}`),
  updateStatus: (id, status) =>
    api.patch(`/movies/${id}/status`, { status }),
};

// ── Cinemas ───────────────────────────────────
export const cinemaApi = {
  getAll: (city) => api.get("/cinemas", { city }),
  getById: (id) => api.get(`/cinemas/${id}`),
  // Admin
  create: (data) => api.post("/admin/cinemas", data),
  update: (id, data) => api.put(`/admin/cinemas/${id}`, data),
  delete: (id) => api.delete(`/admin/cinemas/${id}`),
};

// ── Showtimes ─────────────────────────────────
export const showtimeApi = {
  getByMovie: (movieId, cinemaId, date) =>
    api.get("/showtimes", { movieId, cinemaId, date }),
  getSeatMap: (showtimeId) => api.get(`/showtimes/${showtimeId}/seats`),
  getCombos: () => api.get("/showtimes/combos"),
  // Admin
  create: (data) => api.post("/admin/showtimes", data),
  update: (id, data) => api.put(`/admin/showtimes/${id}`, data),
  delete: (id) => api.delete(`/admin/showtimes/${id}`),
};

// ── Vouchers ──────────────────────────────────
export const voucherApi = {
  validate: (code) => api.post("/vouchers/validate", { code }),
};

// ── Bookings ──────────────────────────────────
export const bookingApi = {
  create: (data) => api.post("/bookings", data),
  getMyAll: (page = 0, size = 10) => api.get("/bookings/me", { page, size }),
  getById: (id) => api.get(`/bookings/${id}`),
  cancel: (id) => api.patch(`/bookings/${id}/cancel`),
  createPayment: (bookingId, method = "VNPAY") =>
    api.post(`/bookings/${bookingId}/payment`, { method }),
  // Admin/Staff
  getAll: (params) => api.get("/admin/bookings", params),
  verifyQr: (qrCode) => api.post("/staff/bookings/verify", { qrCode }),
  checkIn: (bookingId) => api.patch(`/staff/bookings/${bookingId}/checkin`),
};

// ── Reviews ───────────────────────────────────
export const reviewApi = {
  getByMovie: (movieId, page = 0) =>
    api.get(`/movies/${movieId}/reviews`, { page, size: 10 }),
  create: (movieId, data) => api.post(`/movies/${movieId}/reviews`, data),
  delete: (id) => api.delete(`/reviews/${id}`),
};

// ── Notifications ─────────────────────────────
export const notificationApi = {
  getAll: (page = 0) => api.get("/notifications", { page, size: 20 }),
  getUnread: () => api.get("/notifications/unread-count"),
  markRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllRead: () => api.patch("/notifications/read-all"),
};

// ── User (Admin) ──────────────────────────────
export const userApi = {
  getAll: (params) => api.get("/admin/users", params),
  getById: (id) => api.get(`/admin/users/${id}`),
  updateRole: (id, role) => api.patch(`/admin/users/${id}/role`, { role }),
  ban: (id) => api.patch(`/admin/users/${id}/ban`),
};

// ── Dashboard ─────────────────────────────────
export const dashboardApi = {
  getStats: () => api.get("/admin/dashboard/stats"),
  getRevenue: (period) => api.get(`/admin/dashboard/revenue`, { period }),
};
