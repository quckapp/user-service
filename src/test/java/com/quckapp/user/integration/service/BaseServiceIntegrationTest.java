package com.quckapp.user.integration.service;

import com.quckapp.user.kafka.UserEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for service layer integration tests.
 * Uses @SpringBootTest to load full Spring context with all beans.
 * Connects to external MySQL from docker-compose (port 3307).
 * Mocks Redis and Kafka to avoid requiring those services for tests.
 *
 * Prerequisites:
 * - Run 'docker compose up -d mysql' from user-service directory
 * - MySQL will be available on localhost:3307
 */
@SpringBootTest(properties = {
    // Exclude Redis auto-configurations
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@Import(BaseServiceIntegrationTest.TestCacheConfig.class)
@TestPropertySource(properties = {
    // MySQL connection
    "spring.datasource.url=jdbc:mysql://localhost:3307/quckapp_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=root_secret",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR",

    // Disable Kafka auto-configuration
    "spring.kafka.bootstrap-servers=",
    "spring.kafka.consumer.group-id=test",

    // Disable Redis health indicator
    "management.health.redis.enabled=false",

    // Logging
    "logging.level.com.quckapp.user=DEBUG"
})
public abstract class BaseServiceIntegrationTest {

    /**
     * Test configuration that provides a simple in-memory cache manager
     * to replace the Redis-based CacheConfig.
     */
    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("users");
        }

        @Bean
        @Primary
        public RedisConnectionFactory mockRedisConnectionFactory() {
            return org.mockito.Mockito.mock(RedisConnectionFactory.class);
        }
    }

    // EntityManager for flushing/clearing persistence context in tests
    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Flushes pending changes and clears the persistence context.
     * Use this between operations that create entities and operations that
     * load them with JOIN FETCH to avoid DuplicateKeyException.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // Mock Redis templates to avoid connection issues
    @MockBean
    protected RedisTemplate<String, Object> redisTemplate;

    @MockBean
    protected StringRedisTemplate stringRedisTemplate;

    // Mock Kafka event publisher
    @MockBean
    protected UserEventPublisher userEventPublisher;
}
