package com.diepnn.shortenurl.common.generator;

import com.diepnn.shortenurl.exception.TooManyRequestException;

/**
 * Abstraction for ID generation strategies.
 *
 * <p>Implementations must ensure that IDs are unique within their scope and
 * should strive for monotonicity when possible. Thread-safety guarantees are
 * implementation-specific and should be documented by each implementation.</p>
 *
 * <p>Typical usage is within request handling paths or persistence layers that
 * require compact, unique identifiers. Implementations may employ different
 * techniques (e.g., Snowflake, random, database-backed sequences).</p>
 */
public interface IdGenerator {
    /**
     * Generates a new unique identifier.
     *
     * <p>Implementations may block briefly under contention to preserve atomicity
     * or ordering guarantees. When a generator is configured with per-millisecond
     * sequencing, high burst rates can temporarily exhaust the available sequence
     * space; an implementation may either wait for the next time unit to continue,
     * or signal backpressure via an exception.</p>
     *
     * @return a newly generated unique ID
     * @throws com.diepnn.shortenurl.exception.TooManyRequestException
     *         if the implementation chooses to signal backpressure (e.g., sequence
     *         space exhausted for the current time unit or lock acquisition policy failed)
     */
    long generate() throws TooManyRequestException;
}
