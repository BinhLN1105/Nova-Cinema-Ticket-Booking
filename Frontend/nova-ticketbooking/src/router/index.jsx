import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { lazy, Suspense } from "react";
import { useAuthStore } from "@/stores/authStore";
import { PageLoader } from "@/components/common/feedback/PageLoader";

// ── Layouts ──────────────────────────────────
import { CustomerLayout } from "@/layouts/CustomerLayout";
import { AdminLayout } from "@/layouts/AdminLayout";
import { StaffLayout } from "@/layouts/StaffLayout";
import { AuthLayout } from "@/layouts/AuthLayout";

// ── Route Guards ─────────────────────────────
function RequireAuth({ role }) {
  const { isAuthenticated, user } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/auth/login" replace />;
  if (role && user?.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}

function RedirectIfAuth() {
  const { isAuthenticated, user } = useAuthStore();
  if (!isAuthenticated) return <Outlet />;
  if (user?.role === "ADMIN") return <Navigate to="/admin/dashboard" replace />;
  if (user?.role === "STAFF") return <Navigate to="/staff/dashboard" replace />;
  return <Navigate to="/" replace />;
}

// ── Lazy Pages ────────────────────────────────
const lazy_ = (factory) => {
  const Component = lazy(factory);
  return (
    <Suspense fallback={<PageLoader />}>
      <Component />
    </Suspense>
  );
};

// Customer
const HomePage = () => lazy_(() => import("@/pages/customer/home/HomePage"));
const MoviesPage = () =>
  lazy_(() => import("@/pages/customer/movies/MoviesPage"));
const MovieDetailPage = () =>
  lazy_(() => import("@/pages/customer/movies/MovieDetailPage"));
const PromotionsPage   = () => 
  lazy_(() => import('@/pages/customer/promotions/PromotionsPage'))
const SelectShowtime = () =>
  lazy_(() => import("@/pages/customer/booking/SelectShowtime"));
const SelectSeat = () =>
  lazy_(() => import("@/pages/customer/booking/SelectSeat"));
const SelectCombo = () =>
  lazy_(() => import("@/pages/customer/booking/SelectCombo"));
const ConfirmBooking = () =>
  lazy_(() => import("@/pages/customer/booking/ConfirmBooking"));
const PaymentPage = () =>
  lazy_(() => import("@/pages/customer/booking/PaymentPage"));
const BookingResult = () =>
  lazy_(() => import("@/pages/customer/booking/BookingResult"));
const CancelConfirmPage = () =>
  lazy_(() => import("@/pages/customer/bookings/CancelConfirmPage"));
const TicketsPage = () =>
  lazy_(() => import("@/pages/customer/tickets/TicketsPage"));
const TicketDetail = () =>
  lazy_(() => import("@/pages/customer/tickets/TicketDetail"));
const ProfilePage = () =>
  lazy_(() => import("@/pages/customer/profile/ProfilePage"));
const GiftCardPage = () =>
  lazy_(() => import("@/pages/customer/gift-cards/GiftCardPage"));
const GiftCardResult = () =>
  lazy_(() => import("@/pages/customer/gift-cards/GiftCardResultPage"));

// Auth
const LoginPage = () => lazy_(() => import("@/pages/customer/auth/LoginPage"));
const RegisterPage = () =>
  lazy_(() => import("@/pages/customer/auth/RegisterPage"));
const ForgotPasswordPage = () =>
  lazy_(() => import("@/pages/customer/auth/ForgotPasswordPage"));
const ResetPasswordPage = () =>
  lazy_(() => import("@/pages/customer/auth/ResetPasswordPage"));
const VerifyOtpPage = () =>
  lazy_(() => import("@/pages/customer/auth/VerifyOtpPage"));

// Admin
const AdminDashboard = () =>
  lazy_(() => import("@/pages/admin/dashboard/DashboardPage"));
const AdminMovies = () =>
  lazy_(() => import("@/pages/admin/movies/MoviesPage"));
const AdminCinemas = () =>
  lazy_(() => import("@/pages/admin/cinemas/CinemasPage"));
const AdminBookings = () =>
  lazy_(() => import("@/pages/admin/bookings/BookingsPage"));
const AdminUsers = () => lazy_(() => import("@/pages/admin/users/UsersPage"));
const AdminPromotions  = () => lazy_(() => import('@/pages/admin/promotions/PromotionsPage'))
const AdminReports = () =>
  lazy_(() => import("@/pages/admin/reports/ReportsPage"));
const AdminSettings = () =>
  lazy_(() => import("@/pages/admin/settings/SettingsPage"));
const AdminShowtimes = () =>
  lazy_(() => import("@/pages/admin/showtimes/ShowtimesPage"));
const AdminCheckIn = () =>
  lazy_(() => import("@/pages/admin/checkin/CheckInPage"));
const AdminPricingRules = () =>
  lazy_(() => import("@/pages/admin/pricing-rules/PricingRulesPage"));
const AdminNotifications = () =>
  lazy_(() => import("@/pages/admin/notifications/AdminNotificationsPage"));
const AdminCombos = () =>
  lazy_(() => import("@/pages/admin/combos/CombosPage"));

// Staff
const StaffDashboard = () =>
  lazy_(() => import("@/pages/staff/dashboard/DashboardPage"));
const StaffBookings = () =>
  lazy_(() => import("@/pages/staff/bookings/BookingsPage"));
const StaffCheckIn = () =>
  lazy_(() => import("@/pages/staff/checkin/CheckInPage"));
const POSPage = () =>
  lazy_(() => import("@/pages/staff/pos/POSPage"));

export const router = createBrowserRouter([
  // ── Customer Portal ────────────────────────
  {
    path: "/",
    element: <CustomerLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "movies", element: <MoviesPage /> },
      { path: "movies/:id", element: <MovieDetailPage /> },
      { path: 'promotions',      element: <PromotionsPage /> },
      { path: "gift-cards",      element: <GiftCardPage /> },
      { path: "gift-cards/result", element: <GiftCardResult /> },
      // Protected customer routes
      {
        element: <RequireAuth role="CUSTOMER" />,
        children: [
          { path: "booking/showtime/:movieId", element: <SelectShowtime /> },
          { path: "booking/seats/:showtimeId", element: <SelectSeat /> },
          { path: "booking/combo", element: <SelectCombo /> },
          { path: "booking/confirm", element: <ConfirmBooking /> },
          { path: "booking/payment/:id", element: <PaymentPage /> },
          { path: "booking/result", element: <BookingResult /> },
          { path: "booking/cancel-confirm", element: <CancelConfirmPage /> },
          { path: "tickets", element: <TicketsPage /> },
          { path: "tickets/:id", element: <TicketDetail /> },
          { path: "profile", element: <ProfilePage /> },
        ],
      },
    ],
  },

  // ── Auth ───────────────────────────────────
  {
    path: "/auth",
    element: <RedirectIfAuth />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: "login", element: <LoginPage /> },
          { path: "register", element: <RegisterPage /> },
          { path: "forgot-password", element: <ForgotPasswordPage /> },
          { path: "verify-otp", element: <VerifyOtpPage /> },
          { path: "reset-password", element: <ResetPasswordPage /> },
        ],
      },
    ],
  },

  // ── Admin Portal ───────────────────────────
  {
    path: "/admin",
    element: <RequireAuth role="ADMIN" />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <Navigate to="/admin/dashboard" replace /> },
          { path: "dashboard", element: <AdminDashboard /> },
          { path: "movies", element: <AdminMovies /> },
          { path: "cinemas", element: <AdminCinemas /> },
          { path: "bookings", element: <AdminBookings /> },
          { path: "users", element: <AdminUsers /> },
          { path: "checkin", element: <AdminCheckIn /> },
          { path: 'promotions',     element: <AdminPromotions /> },
          { path: 'showtimes',      element: <AdminShowtimes /> },
          { path: 'pricing-rules',  element: <AdminPricingRules /> },
          { path: 'combos',         element: <AdminCombos /> },
          { path: "reports", element: <AdminReports /> },
          { path: "settings", element: <AdminSettings /> },
          { path: "notifications", element: <AdminNotifications /> },
          { path: "pos", element: <POSPage /> },
        ],
      },
    ],
  },

  // ── Staff Portal ───────────────────────────
  {
    path: "/staff",
    element: <RequireAuth role="STAFF" />,
    children: [
      {
        element: <StaffLayout />,
        children: [
          { index: true, element: <Navigate to="/staff/dashboard" replace /> },
          { path: "dashboard", element: <StaffDashboard /> },
          { path: "bookings", element: <StaffBookings /> },
          { path: "checkin", element: <StaffCheckIn /> },
          { path: "pos", element: <POSPage /> },
        ],
      },
    ],
  },

  // ── 404 ────────────────────────────────────
  { path: "*", element: <Navigate to="/" replace /> },
]);
