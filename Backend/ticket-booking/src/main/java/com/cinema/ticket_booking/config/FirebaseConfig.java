package com.cinema.ticket_booking.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Firebase initialization — hỗ trợ 2 chế độ:
 *
 * [PROD - Azure] Đọc từ biến env FIREBASE_CREDENTIALS_JSON (raw JSON hoặc Base64-encoded).
 *   → Không cần file vật lý trong container, bảo mật hơn.
 *
 * [DEV - Local] Fallback về đọc file từ classpath (src/main/resources/).
 *   → Như cũ, không cần đổi gì ở local.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    /**
     * Biến env FIREBASE_CREDENTIALS_JSON: Paste raw JSON hoặc Base64 của serviceAccountKey.json.
     * Nếu không set, sẽ fallback về đọc file qua firebase.credential.path.
     */
    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String credentialsJson;

    /** Đường dẫn file classpath — dùng cho local dev */
    @Value("${firebase.credential.path:novaTicket-serviceAccountKey.json}")
    private String credentialPath;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[Firebase] Already initialized, skipping.");
            return;
        }
        try {
            InputStream credentialStream = resolveCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialStream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("[Firebase] Initialized successfully ✓");
        } catch (IOException e) {
            // Không crash app — chỉ log warning. FCM sẽ không hoạt động nhưng app vẫn chạy.
            log.warn("[Firebase] Failed to initialize (FCM disabled): {}", e.getMessage());
        }
    }

    /**
     * Ưu tiên: env var JSON > env var Base64 > file classpath
     */
    private InputStream resolveCredentials() throws IOException {
        if (StringUtils.hasText(credentialsJson)) {
            // Thử parse như JSON thường trước
            if (credentialsJson.trim().startsWith("{")) {
                log.info("[Firebase] Loading credentials from env var (raw JSON).");
                return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            }
            // Nếu không phải JSON → thử decode Base64
            try {
                byte[] decoded = Base64.getDecoder().decode(credentialsJson.trim());
                log.info("[Firebase] Loading credentials from env var (Base64).");
                return new ByteArrayInputStream(decoded);
            } catch (IllegalArgumentException ignored) {
                log.warn("[Firebase] FIREBASE_CREDENTIALS_JSON không phải JSON hoặc Base64 hợp lệ, fallback về file.");
            }
        }
        // Fallback: đọc từ classpath (local dev)
        log.info("[Firebase] Loading credentials from classpath file: {}", credentialPath);
        return new ClassPathResource(credentialPath).getInputStream();
    }
}
