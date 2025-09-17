package com.diepnn.shortenurl.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.FORWARDED_HEADER;
import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.X_FORWARDED_FOR_HEADER;
import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.X_REAL_IP_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserInfoExtractorTests {
    private final UserInfoRequestExtractor userInfoRequestExtractor = new UserInfoRequestExtractor();

    private MockHttpServletRequest createRequest(String ...headers) {
        if (headers.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be in pairs");
        }

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        for (int i = 0; i < headers.length; i += 2) {
            mockRequest.addHeader(headers[i], headers[i + 1]);
        }

        return mockRequest;
    }

    @Nested
    @DisplayName("Test extractUserIp function")
    class ExtractUserIp {
        @Test
        @DisplayName("return IP when there is an IP in X-Forwarded-For")
        public void whenXForwardedForIsNotBlank_returnIp() {
            MockHttpServletRequest mockRequest = createRequest(X_FORWARDED_FOR_HEADER, "192.168.1.1");
            String userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("192.168.1.1", userIp, "Wrong user IP");
        }

        @Test
        @DisplayName("return IP when there are multiple IPs in X-Forwarded-For")
        public void whenXForwardedForIsNotBlank_returnFirstIp() {
            MockHttpServletRequest mockRequest = createRequest(X_FORWARDED_FOR_HEADER, "203.0.113.195, 2001:db8:85a3:8d3:1319:8a2e:370:7348");
            String userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("203.0.113.195", userIp, "Wrong user IP");
        }

        @Test
        @DisplayName("return IP when there is an IP in X-Real-IP and X-Forwarded-For is blank")
        public void whenXForwardedForNotFoundAndXRealIpIsNotBlank_returnIp() {
            MockHttpServletRequest mockRequest = createRequest(X_REAL_IP_HEADER, "192.168.1.1");
            String userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("192.168.1.1", userIp, "Wrong user IP");

            mockRequest = createRequest(X_FORWARDED_FOR_HEADER, " , 2001:db8:85a3:8d3:1319:8a2e:370:7348",
                                        X_REAL_IP_HEADER, "192.168.1.1");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("192.168.1.1", userIp, "Wrong user IP");
        }

        @Test
        @DisplayName("return IP when there is an IP in Forwarded, X-Real-IP and X-Forwarded-For is blank")
        public void whenXForwardedForAndXRealIpAreBlankAndForwardedIsNotBlank_returnIp() {
            MockHttpServletRequest mockRequest = createRequest(FORWARDED_HEADER, "proto=http;by=203.0.113.43;for=192.0.2.60");
            String userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("192.0.2.60", userIp, "Wrong user IP");

            mockRequest = createRequest(FORWARDED_HEADER, "for=\"[2001:db8:cafe::17]:4711\";proto=http;by=203.0.113.43,for=198.51.100.17");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("2001:db8:cafe::17", userIp, "Wrong user IP");

            mockRequest = createRequest(FORWARDED_HEADER, "for=\"[2001:db8:cafe::17]:4711;proto=http;by=203.0.113.43,for=198.51.100.17");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals("198.51.100.17", userIp, "Wrong user IP");
        }

        @Test
        @DisplayName("return remote address when there is no IP in X-Forwarded-For, X-Real-IP and Forwarded")
        public void whenXForwardedForAndXRealIpAndForwardedAreBlank_returnRemoteAddr() {
            MockHttpServletRequest mockRequest = createRequest(FORWARDED_HEADER, " for= ");
            String userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals(mockRequest.getRemoteAddr(), userIp, "Wrong user IP");

            mockRequest = createRequest(FORWARDED_HEADER, " ");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals(mockRequest.getRemoteAddr(), userIp, "Wrong user IP");

            mockRequest = createRequest(FORWARDED_HEADER, "for=\"[2001:db8:cafe::17:4711\"");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals(mockRequest.getRemoteAddr(), userIp, "Wrong user IP");

            mockRequest = createRequest(FORWARDED_HEADER, "for=2001:db8:cafe::17:4711\"");
            userIp = userInfoRequestExtractor.extractUserIp(mockRequest);
            assertEquals(mockRequest.getRemoteAddr(), userIp, "Wrong user IP");
        }
    }

    @Nested
    @DisplayName("test extractUserAgent function")
    class ExtractUserAgent {
        @Test
        public void whenUserAgentIsNotBlank_returnAgent() {
            String expected = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
            MockHttpServletRequest mockRequest = createRequest(HttpHeaders.USER_AGENT, expected);
            String userAgent = userInfoRequestExtractor.extractUserAgent(mockRequest);
            assertEquals(expected, userAgent, "Wrong user agent");

            expected = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; (R1 1.6); SLCC1; .NET CLR 2.0.50727; InfoPath.2; OfficeLiveConnector.1.3; OfficeLivePatch.0.0; .NET CLR 3.5.30729; .NET CLR 3.0.30618; 66760635803; runtime 11.00294; 876906799603; 97880703; 669602703; 9778063903; 877905603; 89670803; 96690803; 8878091903; 7879040603; 999608065603; 799808803; 6666059903; 669602102803; 888809342903; 696901603; 788907703; 887806555703; 97690214703; 66760903; 968909903; 796802422703; 8868026703; 889803611803; 898706903;";
            mockRequest = createRequest(HttpHeaders.USER_AGENT, expected);
            userAgent = userInfoRequestExtractor.extractUserAgent(mockRequest);
            assertEquals(expected.substring(0, 512), userAgent, "Wrong user agent");
        }

        @Test
        public void whenUserAgentIsBlank_returnNull() {
            MockHttpServletRequest mockRequest = createRequest();
            String userAgent = userInfoRequestExtractor.extractUserAgent(mockRequest);
            assertNull(userAgent, "Wrong user agent");
        }
    }
}
