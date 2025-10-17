package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.common.enums.ProviderType;
import com.diepnn.shortenurl.common.enums.UsersStatus;
import com.diepnn.shortenurl.entity.AuthProvider;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.repository.UsersRepository;
import com.diepnn.shortenurl.security.oauth2.OAuth2UserInfo;
import com.diepnn.shortenurl.security.oauth2.OAuth2UserInfoFactory;
import com.diepnn.shortenurl.security.oauth2.provider.google.GoogleOAuth2UserInfo;
import com.diepnn.shortenurl.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * The custom OAuth2 user service for Spring Security.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UsersRepository userRepository;
    private final OAuth2UserInfoFactory oauth2UserInfoFactory;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oauth2User = super.loadUser(userRequest);
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    /**
     * <p>Find the current user </p>
     *
     * @param userRequest the OAuth2 user request
     * @param oauth2User  the OAuth2 user
     * @return the processed OAuth2 user
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oauth2UserInfo = oauth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        if (!StringUtils.hasText(oauth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<Users> userOptional = userRepository.findByEmailWithAuthProviders(oauth2UserInfo.getEmail());
        Users user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            Optional<AuthProvider> authProviderOptional = user.getAuthProviders()
                                                              .stream()
                                                              .filter(x -> x.getProviderType().getValue().equals(registrationId)
                                                                           && x.getProviderUserId().equals(oauth2UserInfo.getId()))
                                                              .findFirst();

            if (authProviderOptional.isEmpty()) {
                throw new OAuth2AuthenticationException("Looks like you're signed up with " +
                                                        registrationId + " account. Please use your " + registrationId +
                                                        " account to login.");
            }

            user = updateExistedUser(user, authProviderOptional.get());
        } else {
            user = registerNewUser(userRequest, oauth2UserInfo);
        }

        return CustomUserDetails.create(user, oauth2User.getAttributes());
    }

    /**
     * Register a new user.
     *
     * @param userRequest    the user request
     * @param oauth2UserInfo the OAuth2 user info
     * @return the registered user
     */
    private Users registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oauth2UserInfo) {
        AuthProvider authProvider = AuthProvider.builder()
                                                .providerType(ProviderType.fromValue(userRequest.getClientRegistration().getRegistrationId()))
                                                .providerUserId(oauth2UserInfo.getId())
                                                .createdDatetime(DateUtils.nowTruncatedToSeconds())
                                                .lastAccessDatetime(DateUtils.nowTruncatedToSeconds())
                                                .build();

        Users user = Users.builder()
                          .email(oauth2UserInfo.getEmail())
                          .avatar(oauth2UserInfo.getImageUrl())
                          .status(UsersStatus.ACTIVE)
                          .createdDatetime(DateUtils.nowTruncatedToSeconds())
                          .authProviders(List.of(authProvider))
                          .build();


        if (oauth2UserInfo instanceof GoogleOAuth2UserInfo googleOAuth2) {
            user.setFirstName(googleOAuth2.getFirstName());
            user.setLastName(googleOAuth2.getLastName());
        }

        return userRepository.save(user);
    }

    private Users updateExistedUser(Users user, AuthProvider oauth2User) {
        oauth2User.setLastAccessDatetime(DateUtils.nowTruncatedToSeconds());
        return userRepository.save(user);
    }
}
