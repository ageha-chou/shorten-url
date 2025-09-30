package com.diepnn.shortenurl.utils;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordEncoderMapperHelper {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Named("encodePassword")
    public String encode(String password) {
        return bCryptPasswordEncoder.encode(password);
    }
}
