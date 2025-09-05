package com.diepnn.shortenurl.utils;

import com.diepnn.shortenurl.dto.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.FORWARDED_HEADER;
import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.X_FORWARDED_FOR_HEADER;
import static com.diepnn.shortenurl.common.constant.HttpHeadersConstants.X_REAL_IP_HEADER;

/**
 * The helper class to extract user information from the request
 */
@Component
public class UserInfoRequestExtractor {
    /**
     * <p>Extract user IP from the request.</p>
     * <p>For X-Forwarded-For, X-Real-IP and Forwarded headers, the first IP will be returned.</p>
     * <p>If not found, the remote IP will be returned.</p>
     * @param request the request object
     * @return user IP
     */
    public String extractUserIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (StringUtils.isNotBlank(forwardedFor)) {
            // X-Forwarded-For: client, proxy1, proxy2
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }

        String realIp = request.getHeader(X_REAL_IP_HEADER); // some proxies use this
        if (StringUtils.isNotBlank(realIp)) {
            return realIp.trim();
        }

        String forwarded = request.getHeader(FORWARDED_HEADER); // RFC 7239: for=...
        if (StringUtils.isNotBlank(forwarded)) {
            // naive parsing: look for for= value; proper parsing should handle quotes/obfs
            for (String part : forwarded.split("[;,]")) {
                String p = part.trim();
                if (p.toLowerCase(Locale.ROOT).startsWith("for=")) {
                    String val = getIpFromForwarded(p);
                    if (StringUtils.isNotBlank(val)) {
                        return val;
                    }
                }
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract the IP from the forwarded header part.
     * @param p the forwarded header part
     * @return the IP
     */
    private String getIpFromForwarded(String p) {
        String val = p.substring(4).trim();
        if (val.contains("::")) { //check if IPv6 in Forwarded header
            if (val.startsWith("\"[") && val.endsWith("\"")) {
                int end = val.indexOf("]");
                if (end > 0) {
                    val = val.substring(2, end);
                } else {
                    val = null;
                }
            } else {
                val = null;
            }
        }

        return val;
    }

    /**
     * Extract the user agent from the request. The value will be truncated to 512 chars.
     * @param request the request object
     * @return user agent. Return null if not found.
     */
    public String extractUserAgent(HttpServletRequest request) {
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        if (StringUtils.isBlank(ua)) return null;

        // Optionally cap to a reasonable size (e.g., 512 chars) to avoid DB issues.
        return ua.length() > 512 ? ua.substring(0, 512) : ua;
    }

    /**
     * Extract user info from the request.
     *
     * @param request the request object
     * @return user info
     */
    public UserInfo getUserInfo(HttpServletRequest request) {
        String userIp = extractUserIp(request);
        String userAgent = extractUserAgent(request);

        return UserInfo.builder()
                       .ipAddress(userIp)
                       .userAgent(userAgent)
                       .visitedDatetime(LocalDateTime.now())
                       .build();
    }
}
