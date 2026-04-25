package com.cinema.ticket_booking.service.social;

public interface FacebookTokenVerifier {

    /**
     * Xác minh Facebook accessToken và trả về thông tin user.
     *
     * @param accessToken Token lấy từ Facebook Login SDK trên Android
     * @return SocialUserInfo nếu hợp lệ
     * @throws com.cinema.ticket_booking.exception.UnauthorizedException nếu token
     *                                                                   không hợp
     *                                                                   lệ
     */
    SocialUserInfo verify(String accessToken);
}
