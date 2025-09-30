package com.diepnn.shortenurl.security.oauth2;

import com.diepnn.shortenurl.security.oauth2.provider.OAuth2UserInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating {@link OAuth2UserInfo} instances.
 */
@Component
@RequiredArgsConstructor
public class OAuth2UserInfoFactory {
    private final Map<String, OAuth2UserInfoProvider> providers;

    public OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        String providerBeanName = registrationId.toLowerCase() + "OAuth2UserInfoProvider";
        OAuth2UserInfoProvider provider = providers.get(providerBeanName);

        if (provider == null) {
            throw new OAuth2AuthenticationException("Sorry! Login with " + registrationId + " is not supported yet.");
        }

        return provider.create(attributes);
    }
}
