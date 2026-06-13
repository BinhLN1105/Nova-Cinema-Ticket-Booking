package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
// import com.cinema.ticket_booking.security.XssStringDeserializer;
import org.jsoup.safety.Safelist;
import org.jsoup.Jsoup;

import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 32, message = "Mật khẩu phải từ 6 đến 32 ký tự")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$", message = "Mật khẩu phải chứa cả chữ cái và chữ số")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? Jsoup.clean(fullName, Safelist.none()) : null;
    }

    @Size(max = 15, message = "Số điện thoại không quá 15 ký tự")
    private String phone;
}
