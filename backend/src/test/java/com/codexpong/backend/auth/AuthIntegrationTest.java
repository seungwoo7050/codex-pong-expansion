package com.codexpong.backend.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.auth.dto.LoginRequest;
import com.codexpong.backend.auth.dto.RegisterRequest;
import com.codexpong.backend.user.dto.ProfileUpdateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/auth/AuthIntegrationTest.java
 * 설명:
 *   - 회원가입, 로그인, 프로필 조회/수정까지 v0.2.0 인증 흐름이 동작하는지 검증한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private String token;
    private String username;

    @BeforeEach
    void setupUser() throws Exception {
        username = "tester" + COUNTER.incrementAndGet();
        RegisterRequest registerRequest = new RegisterRequest(username, "password123", "테스터", null);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is(username)))
                .andReturn();
        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        token = (String) response.get("token");
    }

    @Test
    void 로그인_후_내_프로필을_조회하고_수정한다() throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.nickname", is("테스터")))
                .andReturn();
        Map<String, Object> loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        String loginToken = (String) loginResponse.get("token");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)));

        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest("새닉네임", "https://example.com/avatar.png");
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + loginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname", is("새닉네임")))
                .andExpect(jsonPath("$.avatarUrl", is("https://example.com/avatar.png")));
    }

    @Test
    void 토큰_없이_프로필을_요청하면_거부된다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
