package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.entity.Users;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The custom user details implementation for Spring Security.
 */
public class CustomUserDetails implements UserDetails, CredentialsContainer {
    private final Users user;
    private final List<? extends GrantedAuthority> authorities;

    @JsonCreator
    public CustomUserDetails(@JsonProperty("user") Users user, @JsonProperty("authorities") List<String> authorities) {
        this.user = user;
        authorities = Optional.ofNullable(authorities).orElse(List.of());
        this.authorities = authorities.stream()
                                      .map(SimpleGrantedAuthority::new)
                                      .toList();
    }

    @JsonProperty("user")
    public Users getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

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
}
