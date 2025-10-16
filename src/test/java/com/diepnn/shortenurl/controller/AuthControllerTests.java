package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.request.AccessTokenRequest;
import com.diepnn.shortenurl.helper.BaseControllerTest;
import com.diepnn.shortenurl.helper.MvcTest;
import com.diepnn.shortenurl.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MvcTest(AuthController.class)
public class AuthControllerTests extends BaseControllerTest {
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/access-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    class AccessTokenRequestTests {
        static Stream<AccessTokenRequest> invalidRequests() {
            return Stream.of(
                    new AccessTokenRequest("", "P@ssw0rd"),
                    new AccessTokenRequest("ageha-chou", ""),
                    new AccessTokenRequest("", ""),
                    new AccessTokenRequest(null, null)
                            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        void whenRequestBodyIsEmpty_return400(String input) throws Exception {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(input)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));
        }

        @ParameterizedTest
        @MethodSource(value = "invalidRequests")
        void whenRequestIsInvalid_return400(AccessTokenRequest input) throws Exception {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(input)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));
        }
    }

    @Nested
    class LoginWithUsernameAndPasswordTests {
        @Test
        void whenRequestIsValid_returnAccessToken() throws Exception {
            AccessTokenRequest request = new AccessTokenRequest("ageha-chou", "P@ssw0rd");

            Authentication mockAuth = new UsernamePasswordAuthenticationToken("ageha-chou", null);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(jwtService.generateToken(mockAuth)).thenReturn("mock-jwt-token");

            mockMvc.perform(post(LOGIN_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.message").value("Token generated successfully"))
                   .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
        }
    }
}
