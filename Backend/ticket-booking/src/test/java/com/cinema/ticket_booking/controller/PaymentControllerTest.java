package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PaymentRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private User mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Payment Tester")
                .email("payment@test.com")
                .build();

        ReflectionTestUtils.setField(paymentController, "frontendUrl", "https://localhost:5173");

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testCreatePayment_Success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(UUID.randomUUID().toString());
        request.setReturnUrl("https://localhost:5173/booking/result");

        PaymentResponse response = PaymentResponse.builder()
                .paymentUrl("https://vnpay.vn/pay")
                .build();

        when(paymentService.createPaymentUrl(eq(mockUser.getId()), any(PaymentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testPayWithWallet_Success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder().build();

        when(paymentService.payWithWallet(eq(mockUser.getId()), eq(bookingId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/wallet/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testVnpayCallback_Web_Success() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay/callback")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TxnRef", "TXN123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:5173/booking/result?status=success&vnp_ResponseCode=00"));
    }

    @Test
    void testVnpayCallback_Mobile_Redirect() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay/callback")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TxnRef", "TXN123")
                .param("source", "mobile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("cinema://payment/result?vnp_ResponseCode=00&status=success"));
    }

    @Test
    void testGetByBooking_Success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder().build();

        when(paymentService.getByBookingId(eq(mockUser.getId()), eq(bookingId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/booking/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
