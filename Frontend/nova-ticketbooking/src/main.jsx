import React from "react";
import ReactDOM from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Toaster, toast } from "react-hot-toast";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { router } from "@/router";
import { useThemeStore } from "@/stores/themeStore";
import "@/styles/globals.css";
import "./i18n";

// Apply saved theme preferences on startup
useThemeStore.getState().init();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1 minute
      gcTime: 10 * 60 * 1000, // 10 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});

// Service Worker registration is now handled in utils/firebase.js during token request


// Foreground notification listener
import { onMessageListener } from "@/utils/firebase";
onMessageListener()
  .then((payload) => {
    console.log("Foreground notification received:", payload);
    const data = payload.data || {};
    const notification = payload.notification || {};
    const title = notification.title || data.title || data.Title || "Nova Ticket";
    const body = notification.body || data.body || data.message || data.Body || "Bạn có thông báo mới";

    toast.custom((t) => (
      <div className={`${t.visible ? 'animate-enter' : 'animate-leave'} max-w-md w-full bg-white shadow-lg rounded-xl pointer-events-auto flex ring-1 ring-black ring-opacity-5 border-l-4 border-brand-500`}>
        <div className="flex-1 w-0 p-4">
          <div className="flex items-start">
            <div className="flex-shrink-0 pt-0.5">
              <img className="h-10 w-10 rounded-full object-cover" src="/logo.png" alt="Logo" />
            </div>
            <div className="ml-3 flex-1">
              <p className="text-sm font-bold text-gray-900">{title}</p>
              <p className="mt-1 text-sm font-medium text-gray-500">{body}</p>
            </div>
          </div>
        </div>
        <div className="flex border-l border-gray-100">
          <button
            onClick={() => toast.dismiss(t.id)}
            className="w-full border border-transparent rounded-none rounded-r-lg p-4 flex items-center justify-center text-xs font-bold text-brand-600 hover:text-brand-500 uppercase tracking-wider"
          >
            Đóng
          </button>
        </div>
      </div>
    ), { duration: 6000 });
  })
  .catch(err => console.error('Notification listener failed:', err));

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />

      {/* Toast notifications */}
      <Toaster
        position="top-right"
        gutter={8}
        containerStyle={{ top: 80 }}
        toastOptions={{
          duration: 4000,
          style: {
            background: "#1a1a24",
            color: "#F9FAFB",
            border: "1px solid rgba(255,255,255,0.08)",
            borderRadius: "12px",
            fontFamily: '"DM Sans", sans-serif',
            fontSize: "14px",
            boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
          },
          success: {
            iconTheme: { primary: "#16a34a", secondary: "#F9FAFB" },
          },
          error: {
            iconTheme: { primary: "#E50914", secondary: "#F9FAFB" },
          },
        }}
      />

      {/* React Query Devtools đã được gỡ bỏ theo yêu cầu */}
    </QueryClientProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>,
);
