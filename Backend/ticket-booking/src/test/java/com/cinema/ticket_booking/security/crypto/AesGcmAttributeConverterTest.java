package com.cinema.ticket_booking.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmAttributeConverterTest {

    private AesGcmAttributeConverter converter;
    private static final String VALID_32_BYTE_KEY = "12345678901234567890123456789012"; // 32 bytes

    @BeforeEach
    void setUp() {
        converter = new AesGcmAttributeConverter();
        converter.initKey(VALID_32_BYTE_KEY);
    }

    @Test
    @DisplayName("1. Roundtrip: Mã hóa và giải mã bảo toàn nguyên vẹn 100% nội dung tiếng Việt")
    void testEncryptionAndDecryptionRoundtrip() {
        String plainText = "Khách hàng khiếu nại về việc AI đặt nhầm suất chiếu 20:00 rạp Nguyễn Trãi! Ghế: G7, G8.";
        
        String cipherText = converter.convertToDatabaseColumn(plainText);
        assertNotNull(cipherText);
        assertTrue(cipherText.startsWith("ENC:"));
        assertNotEquals(plainText, cipherText);

        String decryptedText = converter.convertToEntityAttribute(cipherText);
        assertEquals(plainText, decryptedText);
    }

    @Test
    @DisplayName("2. Random IV Uniqueness: Mã hóa cùng một câu 10 lần cho ra 10 ciphertext hoàn toàn khác nhau")
    void testRandomIvUniqueness() {
        String sampleMessage = "Xin chào NovaTicket!";
        Set<String> ciphertexts = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            String encrypted = converter.convertToDatabaseColumn(sampleMessage);
            ciphertexts.add(encrypted);
        }

        // Cả 10 chuỗi mã hóa phải khác nhau 100% chứng minh mỗi lần đều sinh Random IV 12 bytes mới
        assertEquals(10, ciphertexts.size());
    }

    @Test
    @DisplayName("3. Fail-Fast Validation: Ném lỗi ngay lập tức khi key rỗng, null hoặc không đúng 32 bytes")
    void testFailFastOnMissingOrInvalidKey() {
        AesGcmAttributeConverter testConverter = new AesGcmAttributeConverter();

        // Key null
        assertThrows(IllegalStateException.class, () -> testConverter.initKey(null));

        // Key rỗng
        assertThrows(IllegalStateException.class, () -> testConverter.initKey("   "));

        // Key quá ngắn (16 bytes)
        assertThrows(IllegalStateException.class, () -> testConverter.initKey("shortKey12345678"));

        // Key quá dài (40 bytes)
        assertThrows(IllegalStateException.class, () -> testConverter.initKey("1234567890123456789012345678901234567890"));
    }

    @Test
    @DisplayName("4. Tampering Detection: Giả mạo 1 byte trong Ciphertext sẽ bị GCM Auth Tag phát hiện và từ chối")
    void testTamperedCiphertextDetection() {
        String original = "Nội dung cực kỳ quan trọng";
        String cipherText = converter.convertToDatabaseColumn(original);

        // Thay đổi 1 ký tự trong ciphertext (sau tiền tố ENC:)
        char corruptedChar = cipherText.charAt(10) == 'A' ? 'B' : 'A';
        String tamperedCipher = cipherText.substring(0, 10) + corruptedChar + cipherText.substring(11);

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(tamperedCipher));
    }

    @Test
    @DisplayName("5. Tương thích ngược: Chuỗi log cũ không có tiền tố ENC: được trả về nguyên bản")
    void testLegacyPlaintextBackwardCompatibility() {
        String legacyPlaintext = "Tin nhắn cũ lưu từ trước khi bật mã hóa";
        String result = converter.convertToEntityAttribute(legacyPlaintext);
        assertEquals(legacyPlaintext, result);
    }

    @Test
    @DisplayName("6. Xử lý an toàn giá trị null và empty")
    void testNullAndEmptyHandling() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));

        assertEquals("", converter.convertToDatabaseColumn(""));
        assertEquals("", converter.convertToEntityAttribute(""));
    }
}
