package com.diepnn.shortenurl.common.annotation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@NotNull(message = "Original URL is required")
@Pattern(
        regexp = "^(?:(https?)://)?(?:[a-zA-Z0-9.-]+(?::[0-9]+)?|\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?)(?:/.*)?$",
        flags = {Pattern.Flag.CASE_INSENSITIVE},
        message = "Invalid URL"
)
public @interface OriginalUrl {
    String message() default "Invalid URL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        OriginalUrl[] value();
    }
}
