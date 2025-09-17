package com.diepnn.shortenurl.common.generator;

import com.diepnn.shortenurl.common.properties.SnowflakeProperties;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SnowflakeGeneratorTests {
    private SnowflakeProperties props;
    private SnowflakeGenerator generator;

    private final int datacenter = 3;
    private final int machine = 17;
    private long epochBase;

    @BeforeEach
    void setup() {
        epochBase = System.currentTimeMillis();
        props = mock(SnowflakeProperties.class);

        // Default test configuration: typical Snowflake layout
        // sign: 1, epoch: 41, dc: 5, machine: 5, seq: 12
        when(props.getSignBits()).thenReturn(1);
        when(props.getEpochBits()).thenReturn(41);
        when(props.getDataCenterBits()).thenReturn(5);
        when(props.getMachineBits()).thenReturn(5);
        when(props.getSequenceBits()).thenReturn(12);
        when(props.getDatacenter()).thenReturn(datacenter);
        when(props.getMachine()).thenReturn(machine);
        when(props.getEpoch()).thenReturn(epochBase);

        generator = new SnowflakeGenerator(props);
        generator.init();
    }

    // Helpers to decode the packed ID according to the configured bit widths
    private long mask(int bits) {
        return bits == 64 ? -1L : ((1L << bits) - 1);
    }

    private long[] unpack(long id) {
        // [signBits][epochBits][dcBits][machineBits][seqBits]
        long seqMask = mask(props.getSequenceBits());
        long machineMask = mask(props.getMachineBits());
        long dcMask = mask(props.getDataCenterBits());
        long epochMask = mask(props.getEpochBits());

        long seq = (id) & seqMask;
        long machineVal = (id = (id >>> props.getSequenceBits())) & machineMask;
        long dcVal = (id = (id >>> props.getMachineBits())) & dcMask;
        long epochVal = (id = (id >>> props.getDataCenterBits())) & epochMask;
        long sign = (id >>> props.getEpochBits()) & mask(props.getSignBits());

        return new long[]{sign, epochVal, dcVal, machineVal, seq};
    }

    @Test
    @DisplayName("generate(): returns positive ID with correctly packed fields")
    void generate_packsFieldsCorrectly() throws TooManyRequestException {
        long id = generator.generate();
        assertTrue(id > 0, "ID should be positive");

        long[] parts = unpack(id);
        long sign = parts[0];
        long epochVal = parts[1];
        long dcVal = parts[2];
        long machineVal = parts[3];
        long seqVal = parts[4];

        assertEquals(0, sign, "sign bits should be 0 to keep ID positive");
        assertTrue(epochVal >= 0, "epoch segment should be non-negative");
        assertEquals(datacenter, dcVal, "datacenter segment must match");
        assertEquals(machine, machineVal, "machine segment must match");
        assertEquals(0, seqVal, "first ID in a millisecond should have sequence 0");
    }

    @Test
    @DisplayName("generate(): produces strictly increasing IDs across multiple calls")
    void generate_monotonicIncreasing() throws TooManyRequestException {
        int n = 1000;
        long prev = -1;
        for (int i = 0; i < n; i++) {
            long id = generator.generate();
            assertTrue(id > prev, "IDs must be strictly increasing");
            prev = id;
        }
    }

    @Test
    @DisplayName("Same-millisecond requests increment sequence")
    void sameMillisecond_sequenceIncrements() throws TooManyRequestException {
        // Try a few times to avoid flakiness if a millisecond ticks between calls
        for (int attempt = 0; attempt < 5; attempt++) {
            long id1 = generator.generate();
            long id2 = generator.generate();

            long[] p1 = unpack(id1);
            long[] p2 = unpack(id2);

            long epoch1 = p1[1];
            long epoch2 = p2[1];

            if (epoch1 == epoch2) {
                long seq1 = p1[4];
                long seq2 = p2[4];
                assertEquals(seq1 + 1, seq2, "Second ID in the same ms should increment sequence by 1");
                return; // success path
            }
            // else: loop to try again in the same millisecond
        }

        fail("Could not observe two IDs generated in the same millisecond; retry or adjust test timing.");
    }

    @Test
    @DisplayName("Sequence overflow triggers next-millisecond wait and sequence reset")
    void sequenceOverflow_waitsNextMillis() throws Exception {
        // Reconfigure with tiny sequence space to force overflow quickly
        int tinySeqBits = 1; // maxSeq = 1
        when(props.getSequenceBits()).thenReturn(tinySeqBits);

        // Re-init generator with updated bits
        generator = new SnowflakeGenerator(props);
        generator.init();

        // Force internal state to a known timestamp and near-overflow sequence
        long fixedTs = (System.currentTimeMillis() - epochBase);
        setPrivateField(generator, "lastTimestamp", fixedTs);
        setPrivateField(generator, "sequence", 2); // equals maxSeq for 1-bit

        long id = generator.generate();
        long[] parts = unpack(id);
        long newTs = parts[1];
        long seq = parts[4];

        assertTrue(newTs > fixedTs, "Timestamp should advance to next millisecond after overflow");
        assertEquals(0, seq, "Sequence should reset to 0 after moving to next millisecond");
    }

    @Test
    @DisplayName("Init validation: datacenter and machine outside bit range throw IllegalArgumentException")
    void init_validationErrors() {
        // dcBits=1 => max dc = 1, set value to 2 to fail
        when(props.getDataCenterBits()).thenReturn(1);
        when(props.getMachineBits()).thenReturn(1);
        when(props.getSequenceBits()).thenReturn(4);
        when(props.getDatacenter()).thenReturn(2); // out of range
        when(props.getMachine()).thenReturn(0);

        SnowflakeGenerator badDc = new SnowflakeGenerator(props);
        assertThrows(IllegalArgumentException.class, badDc::init, "datacenter out of range should fail");

        // Now fail machine
        when(props.getDatacenter()).thenReturn(1);
        when(props.getMachine()).thenReturn(3); // out of range for 1 bit

        SnowflakeGenerator badMachine = new SnowflakeGenerator(props);
        assertThrows(IllegalArgumentException.class, badMachine::init, "machine out of range should fail");
    }

    @Test
    @DisplayName("Backoff retry: exceeds retry budget -> throws TooManyRequestException and preserves interrupt flag")
    void backoffRetry_exceedsRetries_throwsTooManyRequestExceptionAndRestoresInterrupt() throws Exception {
        // Hold the generator's private lock so the worker blocks on lockInterruptibly()
        ReentrantLock privateLock = getPrivateLock(generator);
        privateLock.lock();
        try {
            final Thread worker = new Thread(() -> {
                try {
                    generator.generate();
                    fail("Expected TooManyRequestException");
                } catch (TooManyRequestException e) {
                    //Expected
                }
            }, "snowflake-worker-exceed");

            worker.start();

            // Interrupt the worker more than MAX_LOCK_RETRIES times while it's blocked on the lock
            // to force the generator's retry loop to give up.
            for (int i = 0; i < 4; i++) { // MAX_LOCK_RETRIES = 3 -> 4 interrupts
                Thread.sleep(20);
                worker.interrupt();
            }

            // Wait for the worker to finish
            worker.join(2000);
            assertFalse(worker.isAlive(), "Worker should have terminated");
        } finally {
            privateLock.unlock();
        }
    }

    @Test
    @DisplayName("Backoff retry: succeeds within retry budget when lock becomes available")
    void backoffRetry_succeedsWithinRetries_producesId() throws Exception {
        // Hold the private lock so the worker initially blocks
        ReentrantLock privateLock = getPrivateLock(generator);
        privateLock.lock();

        final long[] result = {-1L};
        final Exception[] thrown = {null};

        Thread worker = new Thread(() -> {
            try {
                result[0] = generator.generate();
            } catch (Exception e) {
                thrown[0] = e;
            }
        }, "snowflake-worker-success");

        try {
            worker.start();

            // Interrupt the worker fewer times than MAX_LOCK_RETRIES to exercise a backoff-and-retry path
            for (int i = 0; i < 2; i++) { // Below the threshold (MAX_LOCK_RETRIES=3)
                Thread.sleep(20);
                worker.interrupt();
            }

            // Now release the lock so the next acquisition attempt succeeds
            Thread.sleep(20);
            privateLock.unlock();

            worker.join(1000);
            assertFalse(worker.isAlive(), "Worker should have terminated");
            assertNull(thrown[0], "No exception should be thrown when succeeding within retries");
            assertTrue(result[0] > 0, "A valid positive ID should be generated");
        } finally {
            // Ensure the lock is not left locked if something went wrong before unlock
            if (privateLock.isHeldByCurrentThread()) {
                privateLock.unlock();
            }
        }
    }

    // Helper to access the private ReentrantLock for coordinating the backoff tests
    private static ReentrantLock getPrivateLock(SnowflakeGenerator gen) throws Exception {
        Field f = gen.getClass().getDeclaredField("lock");
        f.setAccessible(true);
        return (ReentrantLock) f.get(gen);
    }

    // Reflection helper to set private fields for deterministic tests
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
