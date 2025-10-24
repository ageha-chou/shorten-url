package com.diepnn.shortenurl.helper;

import com.diepnn.shortenurl.common.properties.JwtProperties;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@ActiveProfiles("test")
public abstract class BaseControllerIT {
    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected JwtProperties jwtProperties;

    protected String generateToken(Users user) {
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        return jwtService.generateToken(authentication);
    }

    // Generate a token that is already expired (independent of global TTL)
    protected String generateExpiredToken(Users user) {
        String subject = user.getUsername();
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expired = new Date(now.getTime() - 1000); // 1s in the past

        return Jwts.builder()
                   .subject(subject)
                   .issuedAt(new Date(now.getTime() - 2000))
                   .expiration(expired)
                   .signWith(secretKey, Jwts.SIG.HS256)
                   .compact();
    }
}
