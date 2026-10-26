package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.GiftCardRequest;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.GiftCardService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GiftCardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GiftCardService giftCardService;

    @InjectMocks
    private GiftCardController giftCardController;

    private User mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@example.com")
                .build();

        ReflectionTestUtils.setField(giftCardController, "frontendUrl", "https://localhost:5173");

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

        mockMvc = MockMvcBuilders.standaloneSetup(giftCardController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testBuyGiftCard_Success() throws Exception {
        GiftCardRequest.Buy request = new GiftCardRequest.Buy();
        request.setPrice(BigDecimal.valueOf(100000));
        request.setReturnUrlBase("https://localhost:5173/gift-card/result");

        PaymentResponse response = PaymentResponse.builder()
                .paymentUrl("https://vnpay.vn/payment")
                .build();

        when(giftCardService.buyGiftCard(eq(mockUser.getId()), any(GiftCardRequest.Buy.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/gift-cards/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testVnpayReturn_OpenRedirectBlocked_RedirectsToFrontendUrl() throws Exception {
        mockMvc.perform(get("/api/v1/gift-cards/vnpay-return")
                .param("vnp_ResponseCode", "00")
                .param("vnp_ReturnUrl", "https://attacker.com/evil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:5173/gift-cards/result?vnp_ResponseCode=00"));
    }

    @Test
    void testVnpayReturn_ValidReturnUrl_RedirectsSafely() throws Exception {
        mockMvc.perform(get("/api/v1/gift-cards/vnpay-return")
                .param("vnp_ResponseCode", "00")
                .param("vnp_ReturnUrl", "https://localhost:5173/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:5173/gift-cards/result?vnp_ResponseCode=00"));
    }

    @Test
    void testVnpayReturn_InvalidResponseCode_SanitizesTo99() throws Exception {
        mockMvc.perform(get("/api/v1/gift-cards/vnpay-return")
                .param("vnp_ResponseCode", "evil<script>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:5173/gift-cards/result?vnp_ResponseCode=99"));
    }
}
