package com.diepnn.shortenurl.common.generator;

import com.diepnn.shortenurl.common.properties.SnowflakeProperties;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Snowflake-style ID generator.
 *
 * <p>Builds 64-bit identifiers by packing multiple fields into fixed-width bit segments:
 * <pre>
 *  [signBits][epochBits (time since custom epoch)][dataCenterBits][machineBits][sequenceBits]
 * </pre>
 * The bit-widths, node identifiers, and epoch are provided by {@link SnowflakeProperties}.</p>
 *
 * <h3>Thread-safety and contention</h3>
 * <p>This generator serializes access to the critical section so that the tuple
 * {@code (lastTimestamp, sequence)} is updated atomically. A fair {@code ReentrantLock}
 * is used to minimize latency spikes and preserve ordering under contention.
 * Acquisition uses an interruptible policy so threads can be canceled while waiting.</p>
 *
 * <h3>Clock behavior</h3>
 * <ul>
 *   <li>If the system clock moves backward (current time before the previous timestamp),
 *       the generator waits until the clock catches up to preserve monotonic IDs.</li>
 *   <li>When multiple IDs are requested within the same millisecond, the {@code sequence}
 *       is incremented. On sequence overflow, the generator waits for the next millisecond.</li>
 * </ul>
 *
 * <h3>Backpressure</h3>
 * <p>Implementations may choose to throw
 * {@link com.diepnn.shortenurl.exception.TooManyRequestException} instead of waiting,
 * but by default this generator waits for the next millisecond to maximize throughput
 * without failing calls.</p>
 *
 * <h3>Field validation and packing</h3>
 * <p>Datacenter and machine identifiers are validated at initialization to ensure they
 * fit their configured bit-widths. IDs are composed using left shifts and bitwise ORs
 * in the canonical Snowflake order.</p>
 */
@Component
@ConditionalOnProperty(name = "app.shorten.id.generate.strategy", havingValue = "snowflake", matchIfMissing = true)
@RequiredArgsConstructor
public class SnowflakeGenerator implements IdGenerator {
    private final SnowflakeProperties snowflakeProps;

    /**
     * Maximum sequence value for a single time unit (e.g., millisecond),
     * derived from the configured {@code sequenceBits} as {@code (1 << sequenceBits) - 1}.
     */
    private int maxSeq;

    /**
     * The last time offset (in the epoch unit, typically milliseconds since the custom epoch)
     * for which an ID was generated. Used to detect same-tick requests and clock regressions.
     */
    private long lastTimestamp;

    /**
     * Per-time-unit counter used when generating multiple IDs within the same timestamp.
     * Resets to zero when the timestamp advances.
     */
    private int sequence;

    /**
     * Lock guarding atomic updates to {@code lastTimestamp} and {@code sequence}.
     * Acquired interruptibly to remain responsive to thread cancellation.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    // Retry policy for interrupted lock acquisition
    private static final int MAX_LOCK_RETRIES = 3;
    private static final long INITIAL_RETRY_BACKOFF_MS = 1L;

    @PostConstruct
    public void init() {
        maxSeq = (1 << snowflakeProps.getSequenceBits()) - 1;
        lastTimestamp = -1L;
        sequence = 0;

        // Optional: validate ranges once at startup
        int dcMax = (1 << snowflakeProps.getDataCenterBits()) - 1;
        int machineMax = (1 << snowflakeProps.getMachineBits()) - 1;
        if (snowflakeProps.getDatacenter() < 0 || snowflakeProps.getDatacenter() > dcMax) {
            throw new IllegalArgumentException("datacenter out of range: " + snowflakeProps.getDatacenter());
        }

        if (snowflakeProps.getMachine() < 0 || snowflakeProps.getMachine() > machineMax) {
            throw new IllegalArgumentException("machine out of range: " + snowflakeProps.getMachine());
        }
    }

    /**
     * Generates a new Snowflake ID.
     *
     * <p>Algorithm outline:
     * <ol>
     *   <li>Acquire the lock interruptibly to serialize state updates.</li>
     *   <li>Compute the current timestamp as {@code now - epoch}.</li>
     *   <li>If the clock moved backward, wait until it reaches {@code lastTimestamp}.</li>
     *   <li>If timestamp equals {@code lastTimestamp}, increment {@code sequence};
     *       on overflow, wait for the next timestamp and reset {@code sequence}.</li>
     *   <li>Pack fields into a 64-bit value in the order:
     *       sign → epoch → datacenter → machine → sequence.</li>
     * </ol>
     * The method guarantees uniqueness and preserves monotonic ordering per node,
     * assuming a non-regressing clock or successful waiting for catch-up.</p>
     *
     * @return a 64-bit, time-ordered unique identifier
     * @throws com.diepnn.shortenurl.exception.TooManyRequestException
     *         if the implementation policy elects to signal backpressure (e.g., unable to
     *         acquire the lock within a retry budget or other configured limits)
     */
    @Override
    public long generate() throws TooManyRequestException {
        int attempts = 0;
        long backoff = INITIAL_RETRY_BACKOFF_MS;
        boolean acquired = false;

        while (!acquired) {
            try {
                lock.lockInterruptibly();
                acquired = true;
            } catch (InterruptedException ie) {
                ++attempts;
                // Clear interrupt for retry; we'll restore if we ultimately give up
                Thread.interrupted();
                if (attempts > MAX_LOCK_RETRIES) {
                    // Restore interrupted flag and fail
                    Thread.currentThread().interrupt();
                    throw new TooManyRequestException("Interrupted while acquiring lock for ID generation after retries");
                }

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ignored) {
                    // Clear and continue with exponential backoff
                    Thread.interrupted();
                }

                backoff = Math.min(backoff << 1, 16L); // cap small backoff
            }
        }

        try {
            long now = System.currentTimeMillis();
            long timestamp = now - snowflakeProps.getEpoch();

            if (timestamp < lastTimestamp) {
                timestamp = waitNextMillis(lastTimestamp);
            }

            if (timestamp == lastTimestamp) {
                if (sequence > maxSeq) {
                    timestamp = waitNextMillis(lastTimestamp);
                    sequence = 0;
                    lastTimestamp = timestamp;
                } else {
                    ++sequence;
                }
            } else {
                sequence = 0;
                lastTimestamp = timestamp;
            }

            return getId(timestamp);
        } finally {
            lock.unlock();
        }
    }

    private long getId(long timestamp) {
        int signBits = snowflakeProps.getSignBits();
        int epochBits = snowflakeProps.getEpochBits();
        int dcBits = snowflakeProps.getDataCenterBits();
        int machineBits = snowflakeProps.getMachineBits();
        int seqBits = snowflakeProps.getSequenceBits();

        long epochMask = mask(epochBits);
        long dcMask = mask(dcBits);
        long machineMask = mask(machineBits);
        long seqMask = mask(seqBits);

        long id = 0L;
        id <<= signBits; // keep sign bit(s) at 0 to remain positive
        id = (id << epochBits) | (timestamp & epochMask);
        id = (id << dcBits) | (snowflakeProps.getDatacenter() & dcMask);
        id = (id << machineBits) | (snowflakeProps.getMachine() & machineMask);
        id = (id << seqBits) | (sequence & seqMask);
        return id;
    }

    /**
     * Generates a bit mask for the given number of bits.
     *
     * @param bits the number of bits to mask
     * @return bit mask
     */
    private long mask(int bits) {
        return bits == 64 ? -1L : ((1L << bits) - 1);
    }

    /**
     * Busy-waits until the observed timestamp advances beyond the given value.
     *
     * <p>Used when the system clock regresses or when the per-timestamp sequence is exhausted,
     * to ensure monotonic ID ordering without failures.</p>
     *
     * @param lastTs the previous timestamp to surpass
     * @return the next available timestamp greater than {@code lastTs}
     */
    private long waitNextMillis(long lastTs) {
        long ts;
        do {
            ts = System.currentTimeMillis() - snowflakeProps.getEpoch();
        } while (ts <= lastTs);
        return ts;
    }
}
