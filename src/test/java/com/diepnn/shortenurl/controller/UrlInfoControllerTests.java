package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import com.diepnn.shortenurl.helper.BaseControllerTest;
import com.diepnn.shortenurl.helper.MvcTest;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.mapper.translator.ShortUrlMappings;
import com.diepnn.shortenurl.security.CustomUserDetails;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MvcTest(UrlInfoController.class)
public class UrlInfoControllerTests extends BaseControllerTest {
    private static final String CREATE_ENDPOINT = "/api/v1/url-infos/create";
    private static final String UPDATE_ORIGINAL_URL_ENDPOINT = "/api/v1/url-infos/{id}/update-original-url";

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

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class), nullable(Long.class))).thenReturn(dto);
            when(urlInfoMapper.toDto(any(UrlInfo.class))).thenReturn(dto);

            mockMvc.perform(
                           post(CREATE_ENDPOINT)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(objectMapper.writeValueAsString(request))
                           )
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())));

            verify(userInfoRequestExtractor).getUserInfo(any());
            verify(urlInfoService).create(any(UrlInfoRequest.class), any(UserInfo.class), nullable(Long.class));
        }

        @Test
        @DisplayName("POST create: duplicate alias -> mapped to client error (conflict request)")
        void create_duplicateAlias() throws Exception {
            UrlInfoRequest request = new UrlInfoRequest("https://example.com", "abc123");

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class), nullable(Long.class)))
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

            when(urlInfoService.create(any(UrlInfoRequest.class), any(UserInfo.class), nullable(Long.class)))
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

            verify(urlInfoService, never()).create(any(), any(), any());
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
                   .andExpect(jsonPath("$.errors[?(@.field == 'original_url' && @.message == 'Original URL is required')]").exists());

            verify(urlInfoService, never()).create(any(), any(), any());
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
                   .andExpect(jsonPath("$.errors[?(@.field == 'original_url')]").exists());

            verify(urlInfoService, never()).create(any(), any(), any());
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

            verify(urlInfoService, never()).create(any(), any(), any());
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

            verify(urlInfoService, never()).create(any(), any(), any());
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

            verify(urlInfoService, never()).create(any(), any(), any());
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

            verify(urlInfoService, never()).create(any(), any(), any());
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

            verify(urlInfoService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/url-infos/{id}/update-original-url")
    class UpdateOriginalUrlTests {
        private UrlInfoDTO mockUrlInfoDTO;
        private UpdateOriginalUrl validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new UpdateOriginalUrl("https://example.com/updated");

            mockUrlInfoDTO = new UrlInfoDTO();
            mockUrlInfoDTO.setId(1L);
            mockUrlInfoDTO.setOriginalUrl("https://example.com/updated");
            mockUrlInfoDTO.setShortUrl("http://abc123");
            mockUrlInfoDTO.setCreatedDatetime(LocalDateTime.now());
        }

        @Test
        void shouldUpdateOriginalUrlSuccessfully() throws Exception {
            Long urlId = 1L;
            when(urlInfoService.updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class)))
                    .thenReturn(mockUrlInfoDTO);

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(200)))
                   .andExpect(jsonPath("$.message", is("Updated successfully")))
                   .andExpect(jsonPath("$.data.id", is(1)))
                   .andExpect(jsonPath("$.data.original_url", is("https://example.com/updated")))
                   .andExpect(jsonPath("$.data.short_url", is("http://abc123")));

            verify(urlInfoService, times(1)).updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnNotFoundWhenUrlDoesNotExist() throws Exception {
            Long nonExistentUrlId = 999L;
            when(urlInfoService.updateOriginalUrl(eq(nonExistentUrlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class)))
                    .thenThrow(new NotFoundException("URL not found"));

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, nonExistentUrlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                   .andDo(print())
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())));

            verify(urlInfoService, times(1)).updateOriginalUrl(eq(nonExistentUrlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnBadRequestWhenUrlNotBelongsToUser() throws Exception {
            Long urlId = 1L;
            when(urlInfoService.updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class)))
                    .thenThrow(new IllegalArgumentException("URL not belongs to current user"));

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

            verify(urlInfoService, times(1)).updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnBadRequestWhenOriginalUrlIsNull() throws Exception {
            Long urlId = 1L;
            UpdateOriginalUrl invalidRequest = new UpdateOriginalUrl();
            invalidRequest.setOriginalUrl(null);

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

            verify(urlInfoService, never()).updateOriginalUrl(anyLong(), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnBadRequestWhenOriginalUrlIsEmpty() throws Exception {
            Long urlId = 1L;
            UpdateOriginalUrl invalidRequest = new UpdateOriginalUrl();
            invalidRequest.setOriginalUrl("");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

            verify(urlInfoService, never()).updateOriginalUrl(anyLong(), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnBadRequestWhenOriginalUrlIsInvalidFormat() throws Exception {
            Long urlId = 1L;
            UpdateOriginalUrl invalidRequest = new UpdateOriginalUrl();
            invalidRequest.setOriginalUrl("test%");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

            verify(urlInfoService, never()).updateOriginalUrl(anyLong(), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            // Given
            Long urlId = 1L;

            // When & Then
            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                   .andDo(print())
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

            verify(urlInfoService, never()).updateOriginalUrl(anyLong(), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }

        @Test
        void shouldHandleMultipleUpdatesForSameUrl() throws Exception {
            Long urlId = 1L;

            UrlInfoDTO firstUpdate = new UrlInfoDTO();
            firstUpdate.setOriginalUrl("https://example.com/first");

            UrlInfoDTO secondUpdate = new UrlInfoDTO();
            secondUpdate.setOriginalUrl("https://example.com/second");

            when(urlInfoService.updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class)))
                    .thenReturn(firstUpdate)
                    .thenReturn(secondUpdate);

            // First update
            UpdateOriginalUrl firstRequest = new UpdateOriginalUrl();
            firstRequest.setOriginalUrl("https://example.com/first");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(firstRequest)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(200)))
                   .andExpect(jsonPath("$.data.original_url", is("https://example.com/first")));

            // Second update
            UpdateOriginalUrl secondRequest = new UpdateOriginalUrl();
            secondRequest.setOriginalUrl("https://example.com/second");

            mockMvc.perform(patch(UPDATE_ORIGINAL_URL_ENDPOINT, urlId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(secondRequest)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is(200)))
                   .andExpect(jsonPath("$.data.original_url", is("https://example.com/second")));

            verify(urlInfoService, times(2)).updateOriginalUrl(eq(urlId), any(UpdateOriginalUrl.class), any(CustomUserDetails.class));
        }
    }
}
