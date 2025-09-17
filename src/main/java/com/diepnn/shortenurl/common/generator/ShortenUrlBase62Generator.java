package com.diepnn.shortenurl.common.generator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Base62 encoder for numeric identifiers.
 *
 * <p>This component converts a non-negative {@link Long} value to a compact Base62 string using the
 * alphabet {@code 0-9A-Za-z}. It is registered as a Spring {@link Component} and is conditionally
 * enabled when the application property {@code app.shorten.url.encode.strategy} is set to
 * {@code base62} (this is also the default when the property is missing).</p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Stateless and thread-safe.</li>
 *   <li>Uses iterative division to build the representation and then reverses it.</li>
 *   <li>Input is expected to be non-null and non-negative. For {@code 0}, the method returns {@code "0"}.</li>
 * </ul>
 * </p>
 *
 * <p>Example:
 * <pre>{@code
 * // 125 -> "21"
 * String encoded = shortenUrlBase62Generator.generate(125L);
 * }</pre>
 * </p>
 *
 * @see ShortenUrlGenerator
 */
@Component
@ConditionalOnProperty(name = "app.shorten.url.encode.strategy", havingValue = "base62", matchIfMissing = true)
public class ShortenUrlBase62Generator implements ShortenUrlGenerator<Long, String> {
    /**
     * Base62 alphabet used for encoding: digits, uppercase letters, then lowercase letters.
     * The character index corresponds to the digit value in base 62.
     */
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Generates a Base62-encoded string for the given non-negative number.
     *
     * <p>Returns {@code "0"} when {@code original} is {@code 0}.</p>
     *
     * <p>Preconditions:
     * <ul>
     *   <li>{@code original} must be non-null.</li>
     *   <li>{@code original} must be greater than or equal to 0.</li>
     * </ul>
     * </p>
     *
     * @param original the non-negative number to encode
     * @return the Base62 representation of {@code original}
     * @throws IllegalArgumentException when {@code original} is negative
     */
    @Override
    public String generate(Long original) {
        if (original == null || original < 0) {
            throw new IllegalArgumentException("Invalid input: " + original);
        }

        if (original == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (original != 0) {
            int remainder = (int) (original % 62);
            sb.append(BASE62.charAt(remainder));
            original /= 62;
        }

        return sb.reverse().toString();
    }
}
