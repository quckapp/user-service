package com.quckapp.user.kafka;

import com.quckapp.user.domain.entity.User;
import com.quckapp.user.domain.entity.UserPreferences;
import com.quckapp.user.domain.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserEventPublisher
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventPublisher Tests")
class UserEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UserEventPublisher userEventPublisher;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        userEventPublisher = new UserEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(userEventPublisher, "userEventsTopic", "quckapp.users.events");

        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockKafkaSendSuccess() {
        CompletableFuture future = new CompletableFuture();
        future.complete(null);
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockKafkaSendFailure() {
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);
    }

    @Nested
    @DisplayName("publishUserCreated Tests")
    class PublishUserCreatedTests {

        @Test
        @DisplayName("should publish USER_CREATED event with correct data")
        void shouldPublishUserCreatedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserCreated(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("USER_CREATED");
            assertThat(event.get("userId")).isEqualTo(testUserId.toString());
            assertThat(event.get("source")).isEqualTo("user-service");
            assertThat(event.get("timestamp")).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("id")).isEqualTo(testUserId.toString());
            assertThat(data.get("email")).isEqualTo("test@example.com");
            assertThat(data.get("username")).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> userEventPublisher.publishUserCreated(testUser))
                    .doesNotThrowAnyException();

            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("publishUserUpdated Tests")
    class PublishUserUpdatedTests {

        @Test
        @DisplayName("should publish USER_UPDATED event with correct data")
        void shouldPublishUserUpdatedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserUpdated(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("USER_UPDATED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("id")).isEqualTo(testUserId.toString());
            assertThat(data.get("email")).isEqualTo("test@example.com");
            assertThat(data.get("displayName")).isEqualTo("Test User");
        }

        @Test
        @DisplayName("should handle null displayName")
        void shouldHandleNullDisplayName() {
            mockKafkaSendSuccess();
            testUser.setDisplayName(null);
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserUpdated(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("displayName")).isEqualTo("");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> userEventPublisher.publishUserUpdated(testUser))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishUserDeactivated Tests")
    class PublishUserDeactivatedTests {

        @Test
        @DisplayName("should publish USER_DEACTIVATED event with correct data")
        void shouldPublishUserDeactivatedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserDeactivated(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("USER_DEACTIVATED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("id")).isEqualTo(testUserId.toString());
            assertThat(data.get("email")).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> userEventPublisher.publishUserDeactivated(testUser))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishUserSuspended Tests")
    class PublishUserSuspendedTests {

        @Test
        @DisplayName("should publish USER_SUSPENDED event with correct data")
        void shouldPublishUserSuspendedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserSuspended(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("USER_SUSPENDED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("id")).isEqualTo(testUserId.toString());
            assertThat(data.get("email")).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> userEventPublisher.publishUserSuspended(testUser))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishProfileUpdated Tests")
    class PublishProfileUpdatedTests {

        @Test
        @DisplayName("should publish PROFILE_UPDATED event with custom status")
        void shouldPublishProfileUpdatedEventWithCustomStatus() {
            mockKafkaSendSuccess();
            UserProfile profile = UserProfile.builder()
                    .userId(testUserId)
                    .customStatus("Working from home")
                    .build();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishProfileUpdated(testUserId, profile);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("PROFILE_UPDATED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("userId")).isEqualTo(testUserId.toString());
            assertThat(data.get("customStatus")).isEqualTo("Working from home");
        }

        @Test
        @DisplayName("should publish PROFILE_UPDATED event without custom status")
        void shouldPublishProfileUpdatedEventWithoutCustomStatus() {
            mockKafkaSendSuccess();
            UserProfile profile = UserProfile.builder()
                    .userId(testUserId)
                    .customStatus(null)
                    .build();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishProfileUpdated(testUserId, profile);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("userId")).isEqualTo(testUserId.toString());
            assertThat(data).doesNotContainKey("customStatus");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();
            UserProfile profile = UserProfile.builder().userId(testUserId).build();

            assertThatCode(() -> userEventPublisher.publishProfileUpdated(testUserId, profile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishPreferencesUpdated Tests")
    class PublishPreferencesUpdatedTests {

        @Test
        @DisplayName("should publish PREFERENCES_UPDATED event with correct data")
        void shouldPublishPreferencesUpdatedEvent() {
            mockKafkaSendSuccess();
            UserPreferences preferences = UserPreferences.builder()
                    .userId(testUserId)
                    .theme("dark")
                    .build();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishPreferencesUpdated(testUserId, preferences);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("PREFERENCES_UPDATED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("userId")).isEqualTo(testUserId.toString());
            assertThat(data.get("theme")).isEqualTo("dark");
        }

        @Test
        @DisplayName("should publish with system theme")
        void shouldPublishWithSystemTheme() {
            mockKafkaSendSuccess();
            UserPreferences preferences = UserPreferences.builder()
                    .userId(testUserId)
                    .theme("system")
                    .build();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishPreferencesUpdated(testUserId, preferences);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("theme")).isEqualTo("system");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();
            UserPreferences preferences = UserPreferences.builder()
                    .userId(testUserId)
                    .theme("light")
                    .build();

            assertThatCode(() -> userEventPublisher.publishPreferencesUpdated(testUserId, preferences))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Event Structure Tests")
    class EventStructureTests {

        @Test
        @DisplayName("should include timestamp in all events")
        void shouldIncludeTimestampInAllEvents() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserCreated(testUser);

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("timestamp")).isNotNull();
            assertThat(event.get("timestamp").toString()).isNotEmpty();
        }

        @Test
        @DisplayName("should include source in all events")
        void shouldIncludeSourceInAllEvents() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            userEventPublisher.publishUserUpdated(testUser);

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("source")).isEqualTo("user-service");
        }

        @Test
        @DisplayName("should use userId as Kafka key")
        void shouldUseUserIdAsKafkaKey() {
            mockKafkaSendSuccess();

            userEventPublisher.publishUserDeactivated(testUser);

            verify(kafkaTemplate).send(eq("quckapp.users.events"), eq(testUserId.toString()), any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle KafkaTemplate exception gracefully")
        void shouldHandleKafkaTemplateExceptionGracefully() {
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willThrow(new RuntimeException("Kafka unavailable"));

            assertThatCode(() -> userEventPublisher.publishUserCreated(testUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle multiple consecutive failures")
        void shouldHandleMultipleConsecutiveFailures() {
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willThrow(new RuntimeException("Kafka unavailable"));

            assertThatCode(() -> {
                userEventPublisher.publishUserCreated(testUser);
                userEventPublisher.publishUserUpdated(testUser);
                userEventPublisher.publishUserDeactivated(testUser);
            }).doesNotThrowAnyException();

            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
        }
    }
}
