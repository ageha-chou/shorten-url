package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql( // <-- add this annotation
      statements = {
              "SET FOREIGN_KEY_CHECKS=0",
              "TRUNCATE TABLE url_visit",
              "TRUNCATE TABLE url_info",
              "SET FOREIGN_KEY_CHECKS=1"
      },
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)

public class UrlInfoServiceImplIT {
    @Autowired
    private UrlInfoService urlInfoService;

    @Autowired
    private UrlInfoRepository urlInfoRepository;

    private void initUrlInfo() {
        UrlInfo urlInfo = UrlInfo.builder()
                                 .id(123456789L)
                                 .shortCode("abcde")
                                 .originalUrl( "https://google.com")
                                 .status(UrlInfoStatus.ACTIVE)
                                 .createdDatetime(LocalDateTime.now())
                                 .build();

        urlInfoRepository.save(urlInfo);
    }

    private UserInfo mockUserInfo(String ipAddress, String userAgent) {
        return new UserInfo(ipAddress, userAgent, LocalDateTime.now(), null);
    }

    @Test
    void create_whenShortCodeIsExisted_throwException() {
        UserInfo userInfo = mockUserInfo("127.0.0.1", "Mozilla/5.0");
        initUrlInfo();
        UrlInfoRequest userRequest = new UrlInfoRequest("https://netflix.com", "abcde");
        AliasAlreadyExistsException ex = assertThrows(AliasAlreadyExistsException.class,
                                                      () -> urlInfoService.create(userRequest, userInfo));

        String expectedMessage = String.format("The alias '%s' is already in use.", userRequest.getAlias());
        String actualMessage = ex.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }
}
