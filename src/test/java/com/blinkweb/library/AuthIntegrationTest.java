package com.blinkweb.library;

import com.blinkweb.library.dto.OtpSendRequest;
import com.blinkweb.library.dto.OtpVerifyRequest;
import com.blinkweb.library.service.EmailService;
import com.blinkweb.library.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("Should send OTP without requiring tenant header")
    void shouldSendOtpWithoutTenantHeader() throws Exception {
        org.mockito.Mockito.when(emailService.sendOtpEmail(anyString(), anyString())).thenReturn(true);

        OtpSendRequest request = new OtpSendRequest("freesubscribe897@gmail.com");

        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("A 6-digit security code has been sent to freesubscribe897@gmail.com. Enter it below to verify.")));
    }

    @Test
    @DisplayName("Should reject invalid OTP with 400 Bad Request")
    void shouldRejectInvalidOtp() throws Exception {
        otpService.seedOtp("test@example.com", "999999", 5);

        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "000000");

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_OTP")));
    }

    @Test
    @DisplayName("Should verify valid OTP, assign admin role to freesubscribe897@gmail.com, and return JWT")
    void shouldVerifyOtpAndAssignAdminRole() throws Exception {
        String email = "freesubscribe897@gmail.com";
        otpService.seedOtp(email, "654321", 5);

        OtpVerifyRequest request = new OtpVerifyRequest(email, "654321");

        String response = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.id", is("usr_admin")))
                .andExpect(jsonPath("$.user.email", is("freesubscribe897@gmail.com")))
                .andExpect(jsonPath("$.user.role", is("admin")))
                .andReturn().getResponse().getContentAsString();

        String jwtToken = com.jayway.jsonpath.JsonPath.read(response, "$.token");

        // Verify that /api/auth/me accepts this JWT
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.email", is("freesubscribe897@gmail.com")))
                .andExpect(jsonPath("$.user.role", is("admin")));

        // Verify single-use OTP: attempting to verify same OTP again must fail
        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_OTP")));
    }
}
