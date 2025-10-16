package com.diepnn.shortenurl.helper;

import com.diepnn.shortenurl.config.JacksonConfig;
import com.diepnn.shortenurl.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for controller tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
public @interface MvcTest {
    /**
     * Specify which controller to load for this test.
     * This is aliased to the 'controllers' attribute of @WebMvcTest.
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?> value();
}
