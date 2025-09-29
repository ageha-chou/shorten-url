package com.diepnn.shortenurl.security.oauth2.provider;

import com.diepnn.shortenurl.security.oauth2.OAuth2UserInfo;

import java.util.Map;

/**
 * Interface for OAuth2 User Info providers
 */
public interface OAuth2UserInfoProvider {
    OAuth2UserInfo create(Map<String, Object> attributes);
}
