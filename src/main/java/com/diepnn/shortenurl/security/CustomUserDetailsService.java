package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The custom user details service for Spring Security.
 */
@Service
@RequiredArgsConstructor
@Primary
public class CustomUserDetailsService implements UserDetailsService {
    private final UsersRepository usersRepository;

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsernameWithAuthProviders(username)
                                    .or(() -> usersRepository.findByEmailWithAuthProviders(username))
                                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user, List.of());
    }
}
