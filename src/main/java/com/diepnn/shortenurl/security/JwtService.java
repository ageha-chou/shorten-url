package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Service for generating and validating JWT tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    private SecretKey secretKey;
    private JwtParserBuilder jwtParserBuilder;

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        jwtParserBuilder = Jwts.parser().verifyWith(secretKey);
    }

    /**
     * Generate JWT token for the given user details.
     *
     * @param authentication authenticated user details.
     * @return generated JWT token
     */
    public String generateToken(Authentication authentication) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getTtl());

        String subject = getSubject(authentication);

        return Jwts.builder()
                   .subject(subject)
                   .issuedAt(now)
                   .expiration(expiryDate)
                   .signWith(secretKey, Jwts.SIG.HS256)
                   .compact();
    }

    /**
     * Extract the username from the given JWT token.
     *
     * @param token JWT token
     * @return username. Return null if the token is null or invalid.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validate the given JWT token.
     *
     * @param token JWT token
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // throws if tampered
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parse and validate token
    private Claims extractAllClaims(String token) {
        return jwtParserBuilder.build()
                               .parseSignedClaims(token)
                               .getPayload();
    }

    // Check if the token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Check if the given JWT token is expired.
     *
     * @param token JWT token.
     * @return true if the token is expired, false otherwise.
     */
    private boolean isExpired(String token) {
        Date expiration = jwtParserBuilder.build()
                                          .parseSignedClaims(token)
                                          .getPayload()
                                          .getExpiration();
        return expiration.before(new Date());
    }

    private String getSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String subject = "";

        if (principal instanceof UserDetails userDetails) {
            subject = userDetails.getUsername();
        }

        return subject;
    }
}
