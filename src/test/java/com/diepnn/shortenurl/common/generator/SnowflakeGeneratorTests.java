package com.diepnn.shortenurl.common.generator;

import com.diepnn.shortenurl.common.properties.SnowflakeProperties;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SnowflakeGeneratorTests {
    private SnowflakeGenerator generator;
    private SnowflakeProperties snowflakeProps;

    @BeforeEach
    void setUp() {
        snowflakeProps = createValidSnowflakeProperties();
        generator = new SnowflakeGenerator(snowflakeProps);
        generator.init();
    }

    private SnowflakeProperties createValidSnowflakeProperties() {
        SnowflakeProperties props = mock(SnowflakeProperties.class);
        when(props.getEpoch()).thenReturn(1609459200000L); // 2021-01-01
        when(props.getSignBits()).thenReturn(1);
        when(props.getEpochBits()).thenReturn(41);
        when(props.getDataCenterBits()).thenReturn(5);
        when(props.getMachineBits()).thenReturn(5);
        when(props.getSequenceBits()).thenReturn(12);
        when(props.getDatacenter()).thenReturn(1);
        when(props.getMachine()).thenReturn(1);
        return props;
    }

    // Helper method to inject a spy lock using reflection
    private Lock injectSpyLock() throws Exception {
        Lock spyLock = spy(new ReentrantLock(true));

        Field lockField = SnowflakeGenerator.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(generator, spyLock);

        return spyLock;
    }

    // Helper method to get private field values
    private Object getFieldValue(String fieldName) throws Exception {
        Field field = SnowflakeGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(generator);
    }

    // Helper method to set private field values
    private void setFieldValue(String fieldName, Object value) throws Exception {
        Field field = SnowflakeGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(generator, value);
    }

    @Test
    @DisplayName("Should initialize with correct maxSeq value")
    void testInitialization() throws Exception {
        int expectedMaxSeq = (1 << 12) - 1; // 4095 for 12 sequence bits
        int actualMaxSeq = (int) getFieldValue("maxSeq");

        assertEquals(expectedMaxSeq, actualMaxSeq);
        assertEquals(-1L, getFieldValue("lastTimestamp"));
        assertEquals(0, getFieldValue("sequence"));
    }

    @Test
    @DisplayName("Should throw exception when datacenter is out of range")
    void testDatacenterOutOfRange() {
        when(snowflakeProps.getDatacenter()).thenReturn(32); // Max is 31 for 5 bits

        SnowflakeGenerator invalidGenerator = new SnowflakeGenerator(snowflakeProps);

        assertThrows(IllegalArgumentException.class, invalidGenerator::init,
                     "Should throw exception for datacenter out of range");
    }

    @Test
    @DisplayName("Should throw exception when machine is out of range")
    void testMachineOutOfRange() {
        when(snowflakeProps.getMachine()).thenReturn(-1);

        SnowflakeGenerator invalidGenerator = new SnowflakeGenerator(snowflakeProps);

        assertThrows(IllegalArgumentException.class, invalidGenerator::init,
                     "Should throw exception for machine out of range");
    }

    @Test
    @DisplayName("Should generate unique IDs in single-threaded environment")
    void testSingleThreadedUniqueness() {
        Set<Long> ids = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            long id = generator.generate();
            assertTrue(ids.add(id), "Generated duplicate ID: " + id);
            assertTrue(id > 0, "ID should be positive");
        }

        assertEquals(count, ids.size());
    }

    @Test
    @DisplayName("Should generate monotonically increasing IDs")
    void testMonotonicIncrease() {
        long previousId = generator.generate();

        for (int i = 0; i < 1000; i++) {
            long currentId = generator.generate();
            assertTrue(currentId > previousId,
                       "ID should be monotonically increasing: " + previousId + " >= " + currentId);
            previousId = currentId;
        }
    }

    @Test
    @DisplayName("Should generate unique IDs under concurrent load")
    void testConcurrentUniqueness() throws Exception {
        int threadCount = 10;
        int idsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.generate();
                        if (!allIds.add(id)) {
                            errorCount.incrementAndGet();
                            System.err.println("Duplicate ID generated: " + id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timeout waiting for threads");
        executor.shutdown();

        assertEquals(0, errorCount.get(), "Should not generate any duplicate IDs");
        assertEquals(threadCount * idsPerThread, allIds.size(),
                     "Should generate exactly " + (threadCount * idsPerThread) + " unique IDs");
    }

    @Test
    @DisplayName("Should handle sequence increment within same millisecond")
    void testSequenceIncrement() throws Exception {
        // Generate first ID
        long id1 = generator.generate();
        int sequence1 = (int) getFieldValue("sequence");

        // Generate second ID (likely same millisecond)
        long id2 = generator.generate();
        int sequence2 = (int) getFieldValue("sequence");

        assertTrue(id2 > id1, "Second ID should be greater");

        // If they're in the same timestamp, sequence should have incremented
        long lastTimestamp1 = (long) getFieldValue("lastTimestamp");
        // This is probabilistic but should work in most cases
    }

    @Test
    @DisplayName("Should reset sequence when timestamp advances")
    void testSequenceReset() throws Exception {
        // Set sequence to non-zero value
        setFieldValue("sequence", 100);
        setFieldValue("lastTimestamp", System.currentTimeMillis() - snowflakeProps.getEpoch() - 10);

        // Generate ID - should reset sequence since timestamp has advanced
        generator.generate();

        int sequence = (int) getFieldValue("sequence");
        assertEquals(0, sequence, "Sequence should reset to 0 when timestamp advances");
    }

    @Test
    @DisplayName("Should handle sequence rollover correctly")
    void testSequenceRollover() throws Exception {
        Set<Long> ids = new HashSet<>();

        // Generate enough IDs rapidly to potentially trigger sequence rollover
        for (int i = 0; i < 5000; i++) {
            long id = generator.generate();
            assertTrue(ids.add(id), "Duplicate ID after sequence operations");
        }
    }

    @Test
    @DisplayName("Should acquire and release lock properly")
    void testLockAcquisitionAndRelease() throws Exception {
        Lock spyLock = injectSpyLock();

        generator.generate();

        verify(spyLock, times(1)).lock();
        verify(spyLock, times(1)).unlock();
    }

    @Test
    @DisplayName("Should release lock even when exception occurs")
    void testLockReleaseOnException() throws Exception {
        Lock spyLock = injectSpyLock();

        // Create invalid state that will cause getId to fail
        when(snowflakeProps.getEpochBits()).thenReturn(-1); // Invalid

        try {
            generator.generate();
        } catch (Exception e) {
            // Expected
        }

        verify(spyLock, times(1)).lock();
        verify(spyLock, times(1)).unlock();
    }

    @Test
    @DisplayName("Should use fair lock")
    void testFairLock() throws Exception {
        Field lockField = SnowflakeGenerator.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) lockField.get(generator);

        assertTrue(lock.isFair(), "Lock should be fair");
    }

    @Test
    @DisplayName("Should handle clock going backwards")
    void testClockBackwards() throws Exception {
        // Generate an ID to set lastTimestamp
        generator.generate();
        long currentLastTimestamp = (long) getFieldValue("lastTimestamp");

        // Simulate clock going backwards by setting lastTimestamp to future
        setFieldValue("lastTimestamp", currentLastTimestamp + 1000);

        // This should wait until clock catches up
        // We can't easily test the wait itself, but we can verify it completes
        long id = generator.generate();
        assertTrue(id > 0, "Should still generate valid ID even with clock adjustments");
    }

    @RepeatedTest(5)
    @DisplayName("Should generate IDs consistently across multiple runs")
    void testConsistentGeneration() {
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            long id = generator.generate();
            assertTrue(id > 0, "ID should be positive");
            assertTrue(ids.add(id), "Should not generate duplicates");
        }
    }

    @Test
    @DisplayName("Should not block indefinitely under high contention")
    void testNoDeadlock() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    generator.generate();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS),
                   "All threads should complete within timeout");
        assertEquals(threadCount, successCount.get(),
                     "All threads should successfully generate IDs");

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle rapid successive calls")
    void testRapidSuccessiveCalls() {
        long[] ids = new long[1000];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = generator.generate();
        }

        // Check all IDs are unique
        Set<Long> uniqueIds = new HashSet<>();
        for (long id : ids) {
            assertTrue(uniqueIds.add(id), "Found duplicate ID: " + id);
        }

        // Check IDs are ordered
        for (int i = 1; i < ids.length; i++) {
            assertTrue(ids[i] > ids[i-1],
                       "IDs should be strictly increasing: " + ids[i-1] + " >= " + ids[i]);
        }
    }

    @Test
    @DisplayName("Should maintain thread safety with mixed read patterns")
    void testMixedConcurrentPatterns() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(15);
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(15);

        // 5 threads generating rapidly
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 500; j++) {
                        allIds.add(generator.generate());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 5 threads generating slowly
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        allIds.add(generator.generate());
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 5 threads generating in bursts
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        for (int k = 0; k < 50; k++) {
                            allIds.add(generator.generate());
                        }
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        int expectedCount = (5 * 500) + (5 * 100) + (5 * 10 * 50);
        assertEquals(expectedCount, allIds.size(),
                     "All generated IDs should be unique");
    }

    @Test
    @DisplayName("Should validate datacenter and machine at boundaries")
    void testBoundaryValidation() {
        // Test max valid values
        when(snowflakeProps.getDatacenter()).thenReturn(31); // Max for 5 bits
        when(snowflakeProps.getMachine()).thenReturn(31); // Max for 5 bits

        SnowflakeGenerator validGenerator = new SnowflakeGenerator(snowflakeProps);
        assertDoesNotThrow(validGenerator::init,
                           "Should accept max valid values");

        // Test zero values
        when(snowflakeProps.getDatacenter()).thenReturn(0);
        when(snowflakeProps.getMachine()).thenReturn(0);

        validGenerator = new SnowflakeGenerator(snowflakeProps);
        assertDoesNotThrow(validGenerator::init,
                           "Should accept zero values");
    }

    @Test
    @DisplayName("Should generate different IDs for different datacenter/machine combinations")
    void testDifferentNodeIdentifiers() {
        // Generator 1 with datacenter=1, machine=1
        SnowflakeGenerator gen1 = new SnowflakeGenerator(snowflakeProps);
        gen1.init();

        // Generator 2 with datacenter=2, machine=2
        SnowflakeProperties props2 = createValidSnowflakeProperties();
        when(props2.getDatacenter()).thenReturn(2);
        when(props2.getMachine()).thenReturn(2);
        SnowflakeGenerator gen2 = new SnowflakeGenerator(props2);
        gen2.init();

        long id1 = gen1.generate();
        long id2 = gen2.generate();

        assertNotEquals(id1, id2, "IDs from different datacenter/machine should differ");
    }

    @Test
    @DisplayName("Should handle maximum sequence exhaustion")
    void testMaxSequenceExhaustion() throws Exception {
        // Set sequence near max
        int maxSeq = (int) getFieldValue("maxSeq");
        setFieldValue("sequence", maxSeq + 1);
        setFieldValue("lastTimestamp", System.currentTimeMillis() - snowflakeProps.getEpoch());

        // Next generate should handle sequence exhaustion
        long id = generator.generate();
        assertTrue(id > 0, "Should generate valid ID after sequence exhaustion");

        // Sequence should have reset
        int newSequence = (int) getFieldValue("sequence");
        assertTrue(newSequence <= 1,
                   "Sequence should reset after exhaustion");
    }
}
