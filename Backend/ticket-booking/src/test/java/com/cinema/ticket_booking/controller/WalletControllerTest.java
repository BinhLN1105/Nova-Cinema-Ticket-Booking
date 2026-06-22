package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.TopUpRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.WalletService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private User mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(User.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockUser;
                    }
                })
                .build();

        ReflectionTestUtils.setField(walletController, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void testCreateTopUp_Success() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(50000));

        PaymentResponse paymentResponse = PaymentResponse.builder()
                .paymentUrl("http://vnpay.vn/pay")
                .build();

        when(walletService.createTopUpUrl(eq(mockUser.getId()), eq(request.getAmount()), anyString()))
                .thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentUrl").value("http://vnpay.vn/pay"));
    }

    @Test
    void testVnpayCallback_Success() throws Exception {
        doNothing().when(walletService).handleVnpayCallback(anyMap());

        mockMvc.perform(get("/api/v1/wallet/vnpay-return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "TXN123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/profile?topup=success&vnp_ResponseCode=00"));
    }

    @Test
    void testVnpayCallback_FailedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/vnpay-return")
                        .param("vnp_ResponseCode", "99")
                        .param("vnp_TxnRef", "TXN123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/profile?topup=failed&vnp_ResponseCode=99"));
    }

    @Test
    void testVnpayCallback_ExceptionThrown() throws Exception {
        doThrow(new RuntimeException("Database error")).when(walletService).handleVnpayCallback(anyMap());

        mockMvc.perform(get("/api/v1/wallet/vnpay-return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "TXN123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/profile?topup=success&vnp_ResponseCode=00"));
    }
}
