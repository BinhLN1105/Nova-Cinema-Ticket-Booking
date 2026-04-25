import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";
import { api } from "@/api/client";
import toast from "react-hot-toast";

// Config từ Firebase Console
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

const VAPID_KEY = import.meta.env.VITE_FIREBASE_VAPID_KEY;

export const requestFirebaseToken = async () => {
  try {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
      toast.error("Bạn đã từ chối quyền nhận thông báo");
      return null;
    }

    // Regiser Service Worker thủ công để tránh lỗi scope/fetch
    const registration = await navigator.serviceWorker.register("/firebase-messaging-sw.js", {
      scope: "/",
    });

    const token = await getToken(messaging, { 
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration,
    });
    if (token) {
      console.log("FCM Token:", token);
      // Gửi token lên backend
      await api.patch("/users/me/fcm-token", { fcmToken: token });
      return token;
    } else {
      console.log("No registration token available. Request permission to generate one.");
      return null;
    }
  } catch (err) {
    console.error("An error occurred while retrieving token. ", err);
    return null;
  }
};

// Lắng nghe thông báo khi app đang mở (Foreground)
export const onMessageListener = () =>
  new Promise((resolve) => {
    onMessage(messaging, (payload) => {
      console.log("Payload received in foreground:", payload);
      resolve(payload);
    });
  });
