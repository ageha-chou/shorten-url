package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.exception.GlobalExceptionHandler;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.helper.BaseControllerTest;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.service.ResolveUrlService;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResolveUrlController.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResolveUrlControllerTests extends BaseControllerTest {
    private static final String ACCESS_ENDPOINT = "/{shortCode}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResolveUrlService resolveUrlService;

    @MockitoBean
    private UrlInfoMapper urlInfoMapper;

    @MockitoBean
    private UserInfoRequestExtractor userInfoRequestExtractor;

    @BeforeEach
    void setUp() {
        when(userInfoRequestExtractor.getUserInfo(any())).thenReturn(new UserInfo("127.0.0.1", "Mozilla/5.0", null, null));
    }

    @Nested
    class ResolveShortUrl {
        @Test
        @DisplayName("GET resolve: returns 200 and logs visit via service")
        void resolve_success() throws Exception {
            String code = "abc123";
            String originalUrl = "https://example.com";

            when(resolveUrlService.resolve(eq(code), any(UserInfo.class))).thenReturn(originalUrl);

            mockMvc.perform(get(ACCESS_ENDPOINT, code))
                   .andExpect(status().isTemporaryRedirect())
                   .andExpect(header().exists("Location"))
                   .andExpect(header().string("Location", "https://example.com"));

            verify(resolveUrlService).resolve(eq(code), any(UserInfo.class));
        }

        @Test
        @DisplayName("GET resolve: not found -> 404")
        void resolve_notFound() throws Exception {
            String code = "missing";

            when(resolveUrlService.resolve(eq(code), any(UserInfo.class))).thenThrow(new NotFoundException("Not found"));

            mockMvc.perform(get(ACCESS_ENDPOINT, code))
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())))
                   .andExpect(jsonPath("$.message", is("Not found")));
        }
    }

    @Nested
    class AccessShortCodeRegex {
        @Test
        @DisplayName("GET /{shortCode}: invalid character '_' is rejected by regex -> 404 Not Found (no handler)")
        void access_invalidShortCodeUnderscore_returns404() throws Exception {
            String invalidCode = "abc_123"; // underscore isn't allowed by [A-Za-z0-9-]+

            mockMvc.perform(get(ACCESS_ENDPOINT, invalidCode))
                   .andExpect(status().isNotFound());

            verify(resolveUrlService, never()).resolve(any(), any());
        }

        @Test
        @DisplayName("GET /{shortCode}: invalid characters like '+' are rejected -> 404 Not Found")
        void access_invalidShortCodePlus_returns404() throws Exception {
            String invalidCode = "abc+123";

            mockMvc.perform(get(ACCESS_ENDPOINT, invalidCode))
                   .andExpect(status().isNotFound());

            verify(resolveUrlService, never()).resolve(any(), any());
        }
    }
}
