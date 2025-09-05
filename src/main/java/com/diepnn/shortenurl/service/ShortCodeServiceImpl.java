package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.generator.IdGenerator;
import com.diepnn.shortenurl.common.generator.ShortenUrlGenerator;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates short-code creation by combining an {@code IdGenerator} with a
 * {@code ShortenUrlGenerator}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>generateId(): produce a unique internal identifier (used for alias flows or persistence keys).</li>
 *   <li>generateShortCode(): produce a compact short code from a freshly generated ID.</li>
 * </ul>
 * </p>
 *
 * <p>Thread-safety: This service is stateless. Overall safety depends on the injected generators'
 * implementations.</p>
 *
 * <p>Backpressure: If the underlying ID generator enforces rate limits or sequencing and cannot
 * produce an ID at the moment, a {@code TooManyRequestException} may be thrown.</p>
 */
@Service
@RequiredArgsConstructor
public class ShortCodeServiceImpl implements ShortCodeService {
    private final IdGenerator idGenerator;
    private final ShortenUrlGenerator<Long, String> urlGenerator;

    /**
     * Generates a new short code by first obtaining a unique ID and then encoding it.
     *
     * @return a compact, URL-safe short code
     * @throws TooManyRequestException if the underlying ID generator is unable to allocate an ID
     *                                 due to rate limiting or sequence exhaustion
     */
    @Override
    public String generateShortCode() throws TooManyRequestException {
        return urlGenerator.generate(generateId());
    }

    @Override
    public String generateShortCode(long id) {
        return urlGenerator.generate(id);
    }

    /**
     * Generates and returns a unique identifier without producing a short code.
     * Useful for cases where a user-specified alias will be stored instead of a generated code.
     *
     * @return a newly generated unique identifier
     * @throws TooManyRequestException if the underlying ID generator is unable to allocate an ID
     *                                 due to rate limiting or sequence exhaustion
     */
    @Override
    public Long generateId() throws TooManyRequestException {
        return idGenerator.generate();
    }
}
