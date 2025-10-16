package com.diepnn.shortenurl.helper;

import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public abstract class BaseControllerIT {
    @Autowired
    protected JwtService jwtService;

    protected String generateToken(Users user) {
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        return jwtService.generateToken(authentication);
    }
}
