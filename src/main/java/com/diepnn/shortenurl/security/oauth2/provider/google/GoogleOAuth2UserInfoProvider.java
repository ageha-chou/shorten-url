package com.diepnn.shortenurl.security.oauth2.provider.google;

import com.diepnn.shortenurl.security.oauth2.OAuth2UserInfo;
import com.diepnn.shortenurl.security.oauth2.provider.OAuth2UserInfoProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Google OAuth2 User Info Provider
 * Spring component that creates {@link GoogleOAuth2UserInfo} instances
 */
@Component("googleOAuth2UserInfoProvider")
public class GoogleOAuth2UserInfoProvider implements OAuth2UserInfoProvider {
    @Override
    public OAuth2UserInfo create(Map<String, Object> attributes) {
        return new GoogleOAuth2UserInfo(attributes);
    }
}
