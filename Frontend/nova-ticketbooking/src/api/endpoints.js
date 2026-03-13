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
  getScreens: (cinemaId) => api.get(`/cinemas/${cinemaId}/screens`),
  // Admin
  create: (data) => api.post("/cinemas", data),
  update: (id, data) => api.put(`/cinemas/${id}`, data),
  delete: (id) => api.delete(`/cinemas/${id}`),
  createScreen: (cinemaId, data) => api.post(`/cinemas/${cinemaId}/screens`, data),
  updateScreen: (cinemaId, screenId, data) => api.put(`/cinemas/${cinemaId}/screens/${screenId}`, data),
  deleteScreen: (cinemaId, screenId) => api.delete(`/cinemas/${cinemaId}/screens/${screenId}`),
};

// ── Showtimes ─────────────────────────────────
export const showtimeApi = {
  getByMovie: (movieId, cinemaId, date) =>
    api.get("/showtimes", { movieId, cinemaId, date }),
  getSeatMap: (showtimeId) => api.get(`/showtimes/${showtimeId}/seats`),
  getCombos: () => api.get("/combos"),
  // Admin
  getAll: (params) => api.get("/showtimes/admin", params),
  create: (data) => api.post("/showtimes", data),
  delete: (id) => api.delete(`/showtimes/${id}`),
  overrideSeatPrices: (id, showtimeSeatIds, newPrice) => 
    api.put(`/showtimes/${id}/seats/price`, { showtimeSeatIds, newPrice })
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
  cancelRequest: (id) => api.post(`/bookings/${id}/cancel-request`),
  cancelConfirm: (token, bookingId) => api.post(`/bookings/cancel-confirm?token=${token}&bookingId=${bookingId}`),
  createPayment: (bookingId) =>
    api.post(`/payments`, { bookingId }),
  payWithWallet: (bookingId) => api.post(`/payments/wallet/${bookingId}`),
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
  getAnalytics: () => api.get("/admin/dashboard/analytics"),
};

// ── Vouchers (Admin) ──────────────────────────
export const adminVoucherApi = {
  getAll:   (params) => api.get('/admin/vouchers', params),
  getById:  (id)     => api.get(`/admin/vouchers/${id}`),
  create:   (data)   => api.post('/admin/vouchers', data),
  update:   (id, data) => api.put(`/admin/vouchers/${id}`, data),
  delete:   (id)     => api.delete(`/admin/vouchers/${id}`),
  toggleActive: (id) => api.patch(`/admin/vouchers/${id}/toggle`),
};

// ── Promotions / Banners ──────────────────────
export const promotionApi = {
  // Public
  getActive: () => api.get('/promotions/active'),
  getById:   (id) => api.get(`/promotions/${id}`),
  // Admin
  getAll:    (params) => api.get('/admin/promotions', params),
  create:    (data)   => api.post('/admin/promotions', data),
  update:    (id, data) => api.put(`/admin/promotions/${id}`, data),
  delete:    (id)     => api.delete(`/admin/promotions/${id}`),
  toggleActive: (id)  => api.patch(`/admin/promotions/${id}/toggle`),
};

// ── Pricing Rules ─────────────────────────────
export const ruleApi = {
  getAll: (params) => api.get('/admin/pricing-rules', params),
  getById: (id) => api.get(`/admin/pricing-rules/${id}`),
  create: (data) => api.post('/admin/pricing-rules', data),
  update: (id, data) => api.put(`/admin/pricing-rules/${id}`, data),
  delete: (id) => api.delete(`/admin/pricing-rules/${id}`),
  toggleActive: (id) => api.patch(`/admin/pricing-rules/${id}/toggle`),
};

// ── Wallet ────────────────────────────────────
export const walletApi = {
  topup: (amount) => api.post("/wallet/topup", { amount }),
};
