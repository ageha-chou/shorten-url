package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.GlobalExceptionHandler;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.mapper.translator.ShortUrlMappings;
import com.diepnn.shortenurl.service.UrlInfoService;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UrlInfoController.class)
@Import(GlobalExceptionHandler.class)
public class UrlInfoControllerTests {
    private static final String CREATE_ENDPOINT = "/api/v1/url-infos/create";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlInfoService urlInfoService;

    @MockitoBean
    private UrlInfoMapper urlInfoMapper;

    @MockitoBean
    private UserInfoRequestExtractor userInfoRequestExtractor;

    @MockitoBean
    private ShortUrlMappings shortUrlMappings;

    @BeforeEach
    void setUp() {
        when(userInfoRequestExtractor.getUserInfo(any())).thenReturn(new UserInfo("127.0.0.1", "Mozilla/5.0", null, null));
    }

    private UrlInfo mockUrlInfo(String shortCode, String originalUrl) {
        UrlInfo u = new UrlInfo();
        u.setShortCode(shortCode);
        u.setOriginalUrl(originalUrl);
        return u;
    }

    private UrlInfoDTO mockUrlInfoDTO(UrlInfo u) {
        return UrlInfoDTO.builder()
                         .shortUrl(u.getShortCode())
                         .originalUrl(u.getOriginalUrl())
                         .build();
    }

    @Nested
    @DisplayName("POST /api/v1/url-infos/create")
    class CreateShortUrl {
        @Test
        @DisplayName("POST create: returns 200 and calls service with extracted user info")
        void create_success() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", null);
            UrlInfo urlInfo = mockUrlInfo("abc123", "https://example.com");
            UrlInfoDTO dto = mockUrlInfoDTO(urlInfo);

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class))).thenReturn(urlInfo);
            when(urlInfoMapper.toDto(any(UrlInfo.class))).thenReturn(dto);

            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())));

            verify(userInfoRequestExtractor).getUserInfo(any());
            verify(urlInfoService).create(any(UrlInfoRequest.class), any(UserInfo.class));
        }

        @Test
        @DisplayName("POST create: duplicate alias -> mapped to client error (conflict request)")
        void create_duplicateAlias() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "abc123");

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class)))
                    .thenThrow(new AliasAlreadyExistsException("Alias in use"));

            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isConflict())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                   .andExpect(jsonPath("$.message", is("Alias in use")));
        }

        @Test
        @DisplayName("POST create: interrupted when generating id -> 429 Too Many Requests")
        void create_tooManyRequests() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", null);

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class)))
                    .thenThrow(new TooManyRequestException("Id generation conflict"));

            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isTooManyRequests())
                   .andExpect(jsonPath("$.status", is(HttpStatus.TOO_MANY_REQUESTS.value())))
                   .andExpect(jsonPath("$.message", is("Id generation conflict")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/url-infos/create: validation")
    class UrlInfoRequestValidation {
        @Test
        @DisplayName("POST create: urlInfoRequest is null -> return 400")
        void create_urlInfoRequestIsNull_returns400() throws Exception {
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(null))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: blank request body -> return 400")
        void create_requestBodyIsBlank_returns400() throws Exception {
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString("     "))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: originalUrl is null -> return 400")
        void create_originalUrlIsNull_returns400() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest(null, null);
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'originalUrl' && @.message == 'Original URL is required')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }

        @ParameterizedTest
        @DisplayName("POST create: originalUrl is blank -> return 400")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void create_originalUrlIsBlank_returns400(String input) throws Exception {
            UrlInfoRequest request = new UrlInfoRequest(input, null);
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'originalUrl' && @.message == 'Original URL is required')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }

        @ParameterizedTest
        @DisplayName("POST create: invalid originalUrl -> return 400")
        @ValueSource(strings = {"http: //example.com", "javascript://test.com", "ftp://example.com"})
        void create_invalidOriginalUrl_returns400(String originalUrl) throws Exception {
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(new UrlInfoRequest(originalUrl, null)))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: alias only contains hyphen -> return 400")
        void create_aliasOnlyContainsHyphen_returns400() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "-");
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'alias' && @.message == 'Alias must be 5-30 chars, letters, digits or hyphens')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: last character of alias is hyphen -> return 400")
        void create_lastAliasCharacterIsHyphen_returns400() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "abcde-");
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'alias' && @.message == 'Alias must be 5-30 chars, letters, digits or hyphens')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: first character of alias is hyphen -> return 400")
        void create_firstAliasCharacterIsHyphen_returns400() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "-abcde");
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'alias' && @.message == 'Alias must be 5-30 chars, letters, digits or hyphens')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }

        @Test
        @DisplayName("POST create: alias contains / -> return 400")
        void create_aliasContainsOtherCharacters_returns400() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "mydomain.com/mydomain.com");
            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'alias' && @.message == 'Alias must be 5-30 chars, letters, digits or hyphens')]").exists());

            verify(urlInfoService, never()).create(any(), any());
        }
    }
}
