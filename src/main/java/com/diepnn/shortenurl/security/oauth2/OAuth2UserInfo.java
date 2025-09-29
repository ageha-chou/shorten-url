package com.diepnn.shortenurl.security.oauth2;

import java.util.Map;

/**
 * Base class for OAuth2 User Info
 */
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();

    public abstract String getFullName();

    public abstract String getEmail();

    public abstract String getImageUrl();

    public abstract String getFirstName();

    public abstract String getLastName();
}
