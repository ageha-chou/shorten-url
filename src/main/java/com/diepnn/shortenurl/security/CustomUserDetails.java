package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.entity.Users;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The custom user details implementation for Spring Security.
 */
public class CustomUserDetails implements UserDetails, OAuth2User, CredentialsContainer {
    private final Users user;
    private final List<? extends GrantedAuthority> authorities;

    @JsonIgnore
    private Map<String, Object> attributes;

    public CustomUserDetails() {
        this.user = null;
        this.authorities = List.of();
        this.attributes = Collections.emptyMap();
    }

    @JsonCreator
    public CustomUserDetails(@JsonProperty("user") Users user, @JsonProperty("authorities") List<String> authorities) {
        this.user = user;
        authorities = Optional.ofNullable(authorities).orElse(List.of());
        this.authorities = authorities.stream()
                                      .map(SimpleGrantedAuthority::new)
                                      .toList();
    }

    // Constructor for OAuth2 authentication
    public CustomUserDetails(Users user, List<String> authorities, Map<String, Object> attributes) {
        this(user, authorities);
        this.attributes = attributes;
    }

    // Factory method for regular authentication
    public static CustomUserDetails create(Users user) {
        return new CustomUserDetails(user, Collections.emptyList());
    }

    // Factory method for OAuth2 authentication
    public static CustomUserDetails create(Users user, Map<String, Object> attributes) {
        return new CustomUserDetails(user, Collections.emptyList(), attributes);
    }

    @JsonProperty("user")
    public Users getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return Optional.ofNullable(user.getUsername())
                       .or(() -> Optional.ofNullable(user.getEmail()))
                       .orElse(null);
    }

    @Override
    public void eraseCredentials() {
        user.setPassword(null);
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @JsonIgnore
    @Override
    public String getName() {
        return String.valueOf(user.getEmail());
    }

    @JsonIgnore
    public Long getId() {
        return user.getId();
    }
}
