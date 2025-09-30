package com.diepnn.shortenurl.config;

import com.diepnn.shortenurl.security.CustomOAuth2UserService;
import com.diepnn.shortenurl.security.JwtCacheService;
import com.diepnn.shortenurl.security.JwtService;
import com.diepnn.shortenurl.security.OAuth2AuthenticationSuccessHandler;
import com.diepnn.shortenurl.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

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
                                                  .requestMatchers("/oauth2/**", "/login/oauth2/**")
                                                  .permitAll()
                                                  .anyRequest().authenticated()
                                  )
            .oauth2Login(oauth2 ->
                                 oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                                       .successHandler(oAuth2AuthenticationSuccessHandler)
                                       .failureHandler((request, response, exception) -> {
                                           log.error("OAuth2 error: {}", exception.getMessage());
                                           response.sendRedirect("/login?error=");
                                       })
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
        return new ProviderManager(
                daoAuthenticationProvider(),
                oauth2LoginAuthenticationProvider()
        );
    }

    @Bean
    public OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider() {
        return new OAuth2LoginAuthenticationProvider(
                oauth2AccessTokenResponseClient(),
                customOAuth2UserService
        );
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oauth2AccessTokenResponseClient() {
        return new RestClientAuthorizationCodeTokenResponseClient();
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
