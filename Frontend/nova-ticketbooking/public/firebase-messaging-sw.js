importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

// Config cố định (Tránh lỗi query params khi register tự động)
const firebaseConfig = {
  apiKey: "AIzaSyC_z2bEHc5Laq1EL5xQPujl3TYE5tQUPD8",
  authDomain: "novaticket-804bb.firebaseapp.com",
  projectId: "novaticket-804bb",
  storageBucket: "novaticket-804bb.firebasestorage.app",
  messagingSenderId: "1028959237971",
  appId: "1:1028959237971:web:2bf502be6e6ece178d93a6",
};

if (firebaseConfig.apiKey) {
  firebase.initializeApp(firebaseConfig);
  const messaging = firebase.messaging();

  messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] Background message received ', payload);
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
      body: payload.notification.body,
      icon: '/logo.png',
      data: payload.data
    };
    self.registration.showNotification(notificationTitle, notificationOptions);
  });
}