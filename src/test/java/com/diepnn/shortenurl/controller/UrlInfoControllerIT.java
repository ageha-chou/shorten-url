package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.helper.BaseControllerIT;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.repository.UsersRepository;
import com.diepnn.shortenurl.security.JwtCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class UrlInfoControllerIT extends BaseControllerIT {
    private static final String CREATE_ENDPOINT = "/api/v1/url-infos/create";
    private static final String GET_BY_USER_ID_ENDPOINT = "/api/v1/url-infos";
    private static final String UPDATE_ORIGINAL_URL_ENDPOINT = "/api/v1/url-infos/{id}/update-original-url";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private UrlInfoRepository urlInfoRepository;

    @Autowired
    private JwtCacheService jwtCacheService;

    private Users testUser;
    private Users otherUser;
    private UrlInfo testUrlInfo;
    private String validToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = Users.builder()
                        .email("testuser@example.com")
                        .username("testuser")
                        .password("p@ssw0rd")
                        .build();

        testUser = userRepository.saveAndFlush(testUser);

        otherUser = Users.builder()
                         .email("otheruser@example.com")
                         .username("otheruser")
                         .password("p@ssw0rd")
                         .build();

        otherUser = userRepository.saveAndFlush(otherUser);

        // Create test URL info
        testUrlInfo = UrlInfo.builder()
                             .id(10L)
                             .userId(testUser.getId())
                             .originalUrl("https://example.com/original")
                             .shortCode("abc123")
                             .createdDatetime(LocalDateTime.now())
                             .updatedDatetime(LocalDateTime.now())
                             .build();

        testUrlInfo = urlInfoRepository.saveAndFlush(testUrlInfo);

        // Generate JWT tokens
        validToken = generateToken(testUser);
        otherUserToken = generateToken(otherUser);
    }

    @AfterEach
    void tearDown() {
        jwtCacheService.remove("testuser");
        jwtCacheService.remove("otheruser");
    }

    @Nested
    @DisplayName("POST /api/v1/url-infos/create")
    class createTests {
        @Test
        void shouldSucceed_WithValidToken() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com/test", null);

            mockMvc.perform(post(CREATE_ENDPOINT)
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())))
                   .andExpect(jsonPath("$.message").value("Shorten URL created successfully"))
                   .andExpect(jsonPath("$.data.original_url").value("https://example.com/test"));
        }

        @Test
        void shouldSucceed_WithoutToken_AsAnonymousUser() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest();
            request.setOriginalUrl("https://example.com/anonymous");

            mockMvc.perform(post(CREATE_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())))
                   .andExpect(jsonPath("$.data.original_url").value("https://example.com/anonymous"));
        }

        @Test
        void shouldFail_WithInvalidToken() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest();
            request.setOriginalUrl("https://example.com/test");

            // When & Then
            mockMvc.perform(post(CREATE_ENDPOINT)
                                    .header("Authorization", "Bearer invalid-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldFail_WithExpiredToken() throws Exception {
            String expiredToken = generateToken(testUser);
            UrlInfoRequest request = new UrlInfoRequest("https://example.com/test", null);

            Thread.sleep(1000);

            mockMvc.perform(post(CREATE_ENDPOINT)
                                    .header("Authorization", "Bearer " + expiredToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }
    }

    @Nested
    class GetByUserIdTests {
        @Test
        void shouldSucceed_WithValidToken() throws Exception {
            mockMvc.perform(get(GET_BY_USER_ID_ENDPOINT)
                                    .header("Authorization", "Bearer " + validToken))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
                   .andExpect(jsonPath("$.message").value("Found"))
                   .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void shouldFail_WithoutToken() throws Exception {
            mockMvc.perform(get(GET_BY_USER_ID_ENDPOINT))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldFail_WithInvalidToken() throws Exception {
            mockMvc.perform(get(GET_BY_USER_ID_ENDPOINT)
                                    .header("Authorization", "Bearer invalid-token"))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldOnlyReturnCurrentUserUrls() throws Exception {
            UrlInfo otherUserUrl = new UrlInfo();
            otherUserUrl.setId(1000L);
            otherUserUrl.setUserId(otherUser.getId());
            otherUserUrl.setOriginalUrl("https://example.com/other");
            otherUserUrl.setShortCode("xyz789");
            otherUserUrl.setCreatedDatetime(LocalDateTime.now());
            otherUserUrl.setUpdatedDatetime(LocalDateTime.now());
            urlInfoRepository.saveAndFlush(otherUserUrl);

            mockMvc.perform(get(GET_BY_USER_ID_ENDPOINT)
                                    .header("Authorization", "Bearer " + generateToken(testUser)))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
                   .andExpect(jsonPath("$.data[?(@.short_url == 'xyz789')]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/url-infos/{id}/update-original-url")
    class UpdateOriginalUrlTests {
        @Test
        void shouldSucceed_WithValidTokenAndOwnership() throws Exception {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUrlInfo.getId())
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
                   .andExpect(jsonPath("$.message").value("Updated successfully"))
                   .andExpect(jsonPath("$.data.original_url").value("https://example.com/updated"));
        }

        @Test
        void shouldFail_WithoutToken() throws Exception {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUrlInfo.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldFail_WithInvalidToken() throws Exception {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUrlInfo.getId())
                                    .header("Authorization", "Bearer invalid-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldFail_WhenUserDoesNotOwnUrl() throws Exception {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUrlInfo.getId())
                                    .header("Authorization", "Bearer " + otherUserToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message").value("URL not belongs to current user"));
        }

        @Test
        void shouldFail_WithExpiredToken() throws Exception {
            String expiredToken = generateToken(testUser);
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            Thread.sleep(1000);

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUrlInfo.getId())
                                    .header("Authorization", "Bearer " + expiredToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        void shouldFail_WhenUrlDoesNotExist() throws Exception {
            Long nonExistentId = 99999L;
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/updated");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, nonExistentId)
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())));
        }

        // ==================== CROSS-USER AUTHORIZATION TESTS ====================

        @Test
        void shouldPreventUserFromAccessingOtherUsersData() throws Exception {
            UrlInfo testUserUrl = new UrlInfo();
            testUserUrl.setId(100L);
            testUserUrl.setUserId(testUser.getId());
            testUserUrl.setOriginalUrl("https://example.com/private");
            testUserUrl.setShortCode("private1");
            testUserUrl.setCreatedDatetime(LocalDateTime.now());
            testUserUrl.setUpdatedDatetime(LocalDateTime.now());
            testUserUrl = urlInfoRepository.saveAndFlush(testUserUrl);

            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/unauthorized");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, testUserUrl.getId())
                                    .header("Authorization", "Bearer " + generateToken(otherUser))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message").value("URL not belongs to current user"));
        }

        @Test
        void shouldAllowUserToUpdateTheirOwnUrl() throws Exception {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/my-update");

            mockMvc.perform(patch("/api/v1/url-infos/{id}/update-original-url", testUrlInfo.getId())
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
                   .andExpect(jsonPath("$.data.original_url").value("https://example.com/my-update"));
        }
    }
}
