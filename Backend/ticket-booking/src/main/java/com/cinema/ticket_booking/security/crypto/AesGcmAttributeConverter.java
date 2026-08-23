package com.cinema.ticket_booking.security.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
@Slf4j
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV chuẩn cho AES-GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag
    private static final String PREFIX = "ENC:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile SecretKeySpec staticKeySpec;

    @Value("${app.security.encryption.aes-key:}")
    private String secretKeyString;

    private SecretKeySpec instanceKeySpec;

    @PostConstruct
    public void validateAndInitKey() {
        if (secretKeyString != null && !secretKeyString.isBlank()) {
            initKey(secretKeyString);
        } else if (staticKeySpec == null) {
            throw new IllegalStateException(
                    "AUDIT_LOG_ENCRYPTION_KEY is required and must be provided via environment variable or configuration.");
        }
    }

    public void initKey(String keyString) {
        if (keyString == null || keyString.isBlank()) {
            throw new IllegalStateException(
                    "AUDIT_LOG_ENCRYPTION_KEY is required and must not be empty.");
        }
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "AUDIT_LOG_ENCRYPTION_KEY must be exactly 32 bytes (256 bits) for AES-256-GCM encryption. Actual length: "
                            + keyBytes.length + " bytes.");
        }
        this.instanceKeySpec = new SecretKeySpec(keyBytes, "AES");
        staticKeySpec = this.instanceKeySpec;
        log.info("[AesGcmConverter] Initialized 256-bit AES-GCM encryption key successfully.");
    }

    private SecretKeySpec getActiveKeySpec() {
        SecretKeySpec key = instanceKeySpec != null ? instanceKeySpec : staticKeySpec;
        if (key == null) {
            throw new IllegalStateException(
                    "Encryption key is not initialized. Please configure app.security.encryption.aes-key");
        }
        return key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getActiveKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherWithTag = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LENGTH + cipherWithTag.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherWithTag, 0, combined, GCM_IV_LENGTH, cipherWithTag.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("[AesGcmConverter] Encryption failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to encrypt sensitive data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.isEmpty()) {
            return "";
        }
        // Hỗ trợ tương thích ngược với log cũ chưa mã hóa
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }
        try {
            String base64Payload = dbData.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);
            if (combined.length < GCM_IV_LENGTH + 16) {
                log.warn("[AesGcmConverter] Ciphertext too short, returning raw data.");
                return dbData;
            }

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, combined, 0, GCM_IV_LENGTH);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getActiveKeySpec(), spec);

            byte[] decrypted = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[AesGcmConverter] Decryption failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to decrypt sensitive data", e);
        }
    }
}
