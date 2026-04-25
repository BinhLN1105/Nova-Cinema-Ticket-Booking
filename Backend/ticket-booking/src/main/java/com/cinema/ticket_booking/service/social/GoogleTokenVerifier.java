package com.cinema.ticket_booking.service.social;

public interface GoogleTokenVerifier {

    /**
     * Xác minh Google idToken và trả về thông tin user.
     *
     * @param idToken JWT token lấy từ Google Sign-In SDK trên Android
     * @return SocialUserInfo nếu hợp lệ
     * @throws com.cinema.ticket_booking.exception.UnauthorizedException nếu token
     *                                                                   không hợp
     *                                                                   lệ / hết
     *                                                                   hạn / sai
     *                                                                   audience
     */
    SocialUserInfo verify(String idToken);
}