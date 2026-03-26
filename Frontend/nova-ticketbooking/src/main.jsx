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


// Lắng nghe thông báo foreground (nhẹ nhàng bằng toast)
import { onMessageListener } from "@/utils/firebase";
onMessageListener().then(payload => {
  console.log("Foreground notification:", payload);
  toast(payload.notification.body, {
    icon: '🎬',
    style: { borderRadius: '10px', background: '#333', color: '#fff' }
  });
}).catch(err => console.log('failed: ', err));

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

      {import.meta.env.DEV && 
      ( <ReactQueryDevtools initialIsOpen={false} 
        buttonPosition="bottom-left"
        />)
      }
    </QueryClientProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>,
);
