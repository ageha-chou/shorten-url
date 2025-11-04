package com.diepnn.shortenurl.helper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class BaseIntegrationTest {
    @Container
    static GenericContainer<?> redis;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired(required = false)
    protected RedisTemplate<String, Object> redisTemplate;

    static {
        // Start container in static block BEFORE @DynamicPropertySource
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        System.out.println("Redis container started on port: " + redis.getMappedPort(6379));

        // Ensure cleanup on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (redis.isRunning()) {
                redis.stop();
            }
        }));
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @AfterEach
    void cleanStores() {
        try {
            // Clean in parallel for better performance
            Thread dbCleanThread = new Thread(this::cleanDatabase);
            Thread redisCleanThread = new Thread(this::cleanRedis);

            dbCleanThread.start();
            redisCleanThread.start();

            // Wait for both to complete with timeout
            dbCleanThread.join(TimeUnit.SECONDS.toMillis(10));
            redisCleanThread.join(TimeUnit.SECONDS.toMillis(10));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cleanup interrupted", e);
        }
    }

    private void cleanDatabase() {
        try {
            truncateAllTables();
        } catch (Exception e) {
            System.err.println("Error cleaning database: " + e.getMessage());
            // Don't let cleanup errors fail the test
        }
    }

    private void cleanRedis() {
        if (redisTemplate == null) {
            return;
        }

        try {
            // Use execute to properly manage connections
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                try {
                    connection.serverCommands().flushDb(); // Use flushDb instead of flushAll for isolation
                    return null;
                } catch (Exception e) {
                    System.err.println("Error flushing Redis: " + e.getMessage());
                    return null;
                }
            });
        } catch (Exception e) {
            System.err.println("Error cleaning Redis: " + e.getMessage());
            // Don't let cleanup errors fail the test
        }
    }

    private void truncateAllTables() {
        List<String> excludeTables = List.of("flyway_schema_history", "databasechangelog", "databasechangeloglock");

        try {
            // Disable foreign key checks
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 0;");

            // Collect all tables in the current schema
            List<String> tables = jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                AND table_type = 'BASE TABLE'
                """, String.class);

            // Truncate all non-excluded tables
            for (String table : tables) {
                if (!excludeTables.contains(table.toLowerCase())) {
                    try {
                        jdbc.execute("TRUNCATE TABLE `" + table + "`;");
                    } catch (Exception e) {
                        System.err.println("Failed to truncate table " + table + ": " + e.getMessage());
                    }
                }
            }
        } finally {
            // Always re-enable foreign key checks
            try {
                jdbc.execute("SET FOREIGN_KEY_CHECKS = 1;");
            } catch (Exception e) {
                System.err.println("Failed to re-enable foreign key checks: " + e.getMessage());
            }
        }
    }
}
