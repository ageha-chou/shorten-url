package com.diepnn.shortenurl.config;

import com.diepnn.shortenurl.security.JwtCacheService;
import com.diepnn.shortenurl.security.JwtService;
import com.diepnn.shortenurl.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        InMemoryUserDetailsManager swaggerUser = new InMemoryUserDetailsManager(
                User.withUsername("swagger-admin")
                    .password(bCryptPasswordEncoder.encode("swagger-pass"))
                    .roles("SWAGGER")
                    .build()
        );

        http.securityMatcher("/swagger-ui.html",
                             "/swagger-ui/**",
                             "/v3/api-docs/**",
                             "/swagger-resources/**",
                             "/webjars/**"
                            )
            .authorizeHttpRequests(request -> request.anyRequest().hasRole("SWAGGER"))
            .httpBasic(Customizer.withDefaults())
            .authenticationProvider(swaggerAuthenticationProvider(swaggerUser))
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurity(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.authorizeHttpRequests(request ->
                                           request.requestMatchers(HttpMethod.GET,
                                                                   "/*")
                                                  .permitAll()
                                                  .requestMatchers(HttpMethod.POST,
                                                                   "/api/*/url-infos/create",
                                                                   "/api/*/users/signup",
                                                                   "/api/*/auth/access-token")
                                                  .permitAll()
                                                  .anyRequest().authenticated()
                                  )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationManager(authenticationManager())
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, JwtCacheService jwtCacheService,
                                                           UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, jwtCacheService, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
        return daoAuthenticationProvider;
    }

    private DaoAuthenticationProvider swaggerAuthenticationProvider(UserDetailsService swaggerUser) {
        DaoAuthenticationProvider swaggerProvider = new DaoAuthenticationProvider(swaggerUser);
        swaggerProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return swaggerProvider;
    }
}
