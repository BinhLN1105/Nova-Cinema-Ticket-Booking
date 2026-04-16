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
  forgotPassword: (email) => api.post("/auth/forgot-password", { email }),
  verifyOtp: (email, otp) => api.post("/auth/verify-otp", { email, otp }),
  resetPassword: (token, newPassword) => api.post("/auth/reset-password", { token, newPassword }),
};

// ── Movies ────────────────────────────────────
export const movieApi = {
  getAll: (params) => api.get("/movies", params),
  adminGetAll: (params) => api.get("/movies/admin", params),
  getNowShowing: (page = 0) =>
    api.get("/movies", { status: "NOW_SHOWING", page, size: 12 }),
  getComingSoon: (page = 0) =>
    api.get("/movies", { status: "COMING_SOON", page, size: 12 }),
  getById: (id) => api.get(`/movies/${id}`),
  search: (q, page = 0) => api.get("/movies/search", { q, page, size: 12 }),
  getGenres: () => api.get("/movies/genres"),
  canReview: (id) => api.get(`/movies/${id}/can-review`),
  // Admin
  create: (data) => api.post("/movies", data),
  update: (id, data) => api.put(`/movies/${id}`, data),
  delete: (id) => api.delete(`/movies/${id}`),
  updateStatus: (id, status) =>
    api.patch(`/movies/${id}/status`, { status }),
  // Image Uploads
  uploadPoster: (id, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/movies/${id}/poster`, formData);
  },

  uploadPosterUrl: (id, url) => api.post(`/movies/${id}/poster-url`, { url }),
  uploadBackdrop: (id, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/movies/${id}/backdrop`, formData);
  },

  uploadBackdropUrl: (id, url) => api.post(`/movies/${id}/backdrop-url`, { url }),
};

// ── Cinemas ───────────────────────────────────
export const cinemaApi = {
  getAll: (city) => api.get("/cinemas", { city }),
  adminGetAll: () => api.get("/cinemas/admin"),
  getById: (id) => api.get(`/cinemas/${id}`),
  getScreens: (cinemaId) => api.get(`/cinemas/${cinemaId}/screens`),
  // Admin
  create: (data) => api.post("/cinemas", data),
  update: (id, data) => api.put(`/cinemas/${id}`, data),
  toggleStatus: (id) => api.patch(`/cinemas/${id}/status`),
  delete: (id) => api.delete(`/cinemas/${id}`),
  createScreen: (cinemaId, data) => api.post(`/cinemas/${cinemaId}/screens`, data),
  updateScreen: (cinemaId, screenId, data) => api.put(`/cinemas/${cinemaId}/screens/${screenId}`, data),
  deleteScreen: (cinemaId, screenId, type = "soft") => api.delete(`/cinemas/${cinemaId}/screens/${screenId}?type=${type}`),
  getScreensForAdmin: (cinemaId) => api.get(`/cinemas/${cinemaId}/admin/screens`),
  // Seat Layout Builder
  getScreenSeats: (id, screenId) => api.get(`/cinemas/${id}/screens/${screenId}/seats`),
  saveCustomLayout: (id, screenId, data) => api.put(`/cinemas/${id}/screens/${screenId}/seats`, data),
  // Image Uploads
  uploadImage: (id, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/cinemas/${id}/image`, formData);
  },

  uploadImageUrl: (id, url) => api.post(`/cinemas/${id}/image-url`, { url }),
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
  getActive: () => api.get("/vouchers/active"),
  // Admin
  adminGetAll: (params) => api.get("/admin/vouchers", params),
  create: (data) => api.post("/admin/vouchers", data),
  update: (id, data) => api.put(`/admin/vouchers/${id}`, data),
  delete: (id) => api.delete(`/admin/vouchers/${id}`),
  toggle: (id) => api.patch(`/admin/vouchers/${id}/toggle`),
};

// ── Notifications ─────────────────────────────
export const notificationApi = {
  // Mobile / Personal
  getMyNotifications: (params) => api.get("/notifications/me", params),
  getUnreadCount: () => api.get("/notifications/unread-count"),
  markRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllRead: () => api.post("/notifications/me/read-all"),
  
  // Admin
  createCampaign: (data) => api.post("/admin/notifications/campaign", data),
  getCampaigns: (params) => api.get("/admin/notifications/campaigns", params),
};

// ── Bookings ──────────────────────────────────
export const bookingApi = {
  create: (data) => api.post("/bookings", data),
  getQuote: (data) => api.post("/bookings/quote", data),
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
  create: (data) => api.post("/reviews", data),
  delete: (id) => api.delete(`/reviews/${id}`),
};



// ── User (Admin) ──────────────────────────────
export const userApi = {
  getAll: (params) => api.get("/admin/users", params),
  getById: (id) => api.get(`/admin/users/${id}`),
  updateRole: (id, role) => api.patch(`/admin/users/${id}/role`, { role }),
  ban: (id) => api.patch(`/admin/users/${id}/ban`),
  // Staff management
  createStaff: (data) => api.post("/admin/staff", data),
  assignCinema: (userId, cinemaId) => api.patch(`/admin/staff/${userId}/cinema`, { cinemaId }),
  // Profile / Avatar (Me)
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post("/users/me/avatar", formData);
  },

  uploadAvatarUrl: (url) => api.post("/users/me/avatar-url", { url }),
};

// ── Admin Check-In (QR image upload) ─────────
export const adminCheckInApi = {
  // POST /bookings/check-in?qrCode=xxx  (same endpoint, admin bypasses cinema validation)
  checkIn: (qrCode) => api.post(`/bookings/check-in?qrCode=${encodeURIComponent(qrCode)}`),
};

// ── Dashboard ─────────────────────────────────
export const dashboardApi = {
  getStats: (params) => api.get("/admin/dashboard/stats", params),
  getRevenue: (period) => api.get(`/admin/dashboard/revenue`, { period }),
  getAnalytics: (params) => api.get("/admin/dashboard/analytics", params),
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
  // Image Uploads
  uploadImage: (id, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/admin/promotions/${id}/image`, formData);
  },

  uploadImageUrl: (id, url) => api.post(`/admin/promotions/${id}/image-url`, { url }),
};

// ── System Configs ────────────────────────────
export const systemConfigApi = {
  getAll: () => api.get('/admin/configs'),
  update: (key, value, description) => api.put(`/admin/configs/${key}`, { value, description }),
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

// ── Gift Cards ────────────────────────────────
export const giftCardApi = {
  buy: (price, returnUrlBase) => api.post("/gift-cards/buy", { price, returnUrlBase }),
  redeem: (code) => api.post("/gift-cards/redeem", { code }),
  getMyAll: (page = 0, size = 10) => api.get("/gift-cards/me", { page, size }),
};

// ── Chatbot ───────────────────────────────────
export const chatbotApi = {
  chat: (userMessage) => api.post("/chatbot/chat", { userMessage }),
  clearSession: () => api.post("/chatbot/session/clear"),
};

// ── Combos ───────────────────────────────────
export const comboApi = {
  getAvailable: () => api.get("/combos"),
  // Admin
  create: (data) => api.post("/combos", data),
  update: (id, data) => api.put(`/combos/${id}`, data),
  delete: (id) => api.delete(`/combos/${id}`),
  uploadImage: (id, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/combos/${id}/image`, formData);
  },
  uploadImageUrl: (id, url) => api.post(`/combos/${id}/image-url`, { url }),
};
