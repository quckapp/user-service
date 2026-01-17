package com.quckapp.user.integration.service;

import com.quckapp.user.domain.entity.User;
import com.quckapp.user.domain.entity.User.UserStatus;
import com.quckapp.user.domain.repository.UserPreferencesRepository;
import com.quckapp.user.domain.repository.UserProfileRepository;
import com.quckapp.user.domain.repository.UserRepository;
import com.quckapp.user.dto.UserDtos.*;
import com.quckapp.user.exception.DuplicateResourceException;
import com.quckapp.user.exception.ResourceNotFoundException;
import com.quckapp.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserService.
 * Tests user management operations using real MySQL database.
 */
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest extends BaseServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    @Nested
    @DisplayName("User Creation Operations")
    class UserCreationOperations {

        @Test
        @DisplayName("Should create user successfully")
        @Transactional
        void shouldCreateUser() {
            // Given
            String email = "create_" + System.currentTimeMillis() + "@test.com";
            String username = "createuser" + System.currentTimeMillis();
            CreateUserRequest request = CreateUserRequest.builder()
                    .email(email)
                    .username(username)
                    .displayName("Test User")
                    .timezone("America/New_York")
                    .locale("en-US")
                    .build();

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getEmail()).isEqualTo(email.toLowerCase());
            assertThat(response.getUsername()).isEqualTo(username.toLowerCase());
            assertThat(response.getDisplayName()).isEqualTo("Test User");
            assertThat(response.getTimezone()).isEqualTo("America/New_York");
            assertThat(response.getLocale()).isEqualTo("en-US");
            assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(response.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Should create user with default timezone and locale")
        @Transactional
        void shouldCreateUserWithDefaults() {
            // Given
            String email = "defaults_" + System.currentTimeMillis() + "@test.com";
            String username = "defaultsuser" + System.currentTimeMillis();
            CreateUserRequest request = CreateUserRequest.builder()
                    .email(email)
                    .username(username)
                    .build();

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertThat(response.getTimezone()).isEqualTo("UTC");
            assertThat(response.getLocale()).isEqualTo("en");
        }

        @Test
        @DisplayName("Should create profile and preferences for new user")
        @Transactional
        void shouldCreateProfileAndPreferencesForNewUser() {
            // Given
            String email = "withprofile_" + System.currentTimeMillis() + "@test.com";
            String username = "withprofileuser" + System.currentTimeMillis();
            CreateUserRequest request = CreateUserRequest.builder()
                    .email(email)
                    .username(username)
                    .build();

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertThat(profileRepository.findById(response.getId())).isPresent();
            assertThat(preferencesRepository.findById(response.getId())).isPresent();
        }

        @Test
        @DisplayName("Should fail to create user with duplicate email")
        @Transactional
        void shouldFailToCreateUserWithDuplicateEmail() {
            // Given
            String email = "duplicate_" + System.currentTimeMillis() + "@test.com";
            CreateUserRequest request1 = CreateUserRequest.builder()
                    .email(email)
                    .username("user1_" + System.currentTimeMillis())
                    .build();
            userService.createUser(request1);

            CreateUserRequest request2 = CreateUserRequest.builder()
                    .email(email)
                    .username("user2_" + System.currentTimeMillis())
                    .build();

            // When/Then
            assertThatThrownBy(() -> userService.createUser(request2))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("Should fail to create user with duplicate username")
        @Transactional
        void shouldFailToCreateUserWithDuplicateUsername() {
            // Given
            String username = "dupuser" + System.currentTimeMillis();
            CreateUserRequest request1 = CreateUserRequest.builder()
                    .email("email1_" + System.currentTimeMillis() + "@test.com")
                    .username(username)
                    .build();
            userService.createUser(request1);

            CreateUserRequest request2 = CreateUserRequest.builder()
                    .email("email2_" + System.currentTimeMillis() + "@test.com")
                    .username(username)
                    .build();

            // When/Then
            assertThatThrownBy(() -> userService.createUser(request2))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already exists");
        }
    }

    @Nested
    @DisplayName("User Retrieval Operations")
    class UserRetrievalOperations {

        @Test
        @DisplayName("Should get user by ID")
        @Transactional
        void shouldGetUserById() {
            // Given
            UserResponse created = createTestUser();

            // When
            UserResponse retrieved = userService.getUserById(created.getId());

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(created.getId());
            assertThat(retrieved.getEmail()).isEqualTo(created.getEmail());
        }

        @Test
        @DisplayName("Should get user by email")
        @Transactional
        void shouldGetUserByEmail() {
            // Given
            UserResponse created = createTestUser();

            // When
            UserResponse retrieved = userService.getUserByEmail(created.getEmail());

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("Should get user by email case-insensitively")
        @Transactional
        void shouldGetUserByEmailCaseInsensitive() {
            // Given
            UserResponse created = createTestUser();

            // When
            UserResponse retrieved = userService.getUserByEmail(created.getEmail().toUpperCase());

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("Should get user by username")
        @Transactional
        void shouldGetUserByUsername() {
            // Given
            UserResponse created = createTestUser();

            // When
            UserResponse retrieved = userService.getUserByUsername(created.getUsername());

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("Should throw when user not found by ID")
        @Transactional
        void shouldThrowWhenUserNotFoundById() {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw when user not found by email")
        @Transactional
        void shouldThrowWhenUserNotFoundByEmail() {
            // When/Then
            assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("User Update Operations")
    class UserUpdateOperations {

        @Test
        @DisplayName("Should update user display name")
        @Transactional
        void shouldUpdateUserDisplayName() {
            // Given
            UserResponse created = createTestUser();
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Updated Name")
                    .build();

            // When
            UserResponse updated = userService.updateUser(created.getId(), request);

            // Then
            assertThat(updated.getDisplayName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should update multiple user fields")
        @Transactional
        void shouldUpdateMultipleUserFields() {
            // Given
            UserResponse created = createTestUser();
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("New Name")
                    .phone("+1234567890")
                    .timezone("Europe/London")
                    .locale("en-GB")
                    .avatarUrl("https://example.com/avatar.png")
                    .build();

            // When
            UserResponse updated = userService.updateUser(created.getId(), request);

            // Then
            assertThat(updated.getDisplayName()).isEqualTo("New Name");
            assertThat(updated.getPhone()).isEqualTo("+1234567890");
            assertThat(updated.getTimezone()).isEqualTo("Europe/London");
            assertThat(updated.getLocale()).isEqualTo("en-GB");
            assertThat(updated.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        }

        @Test
        @DisplayName("Should only update provided fields")
        @Transactional
        void shouldOnlyUpdateProvidedFields() {
            // Given
            UserResponse created = createTestUser();
            String originalEmail = created.getEmail();
            String originalUsername = created.getUsername();

            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Partial Update")
                    .build();

            // When
            UserResponse updated = userService.updateUser(created.getId(), request);

            // Then
            assertThat(updated.getDisplayName()).isEqualTo("Partial Update");
            assertThat(updated.getEmail()).isEqualTo(originalEmail);
            assertThat(updated.getUsername()).isEqualTo(originalUsername);
        }

        @Test
        @DisplayName("Should throw when updating non-existent user")
        @Transactional
        void shouldThrowWhenUpdatingNonExistentUser() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Test")
                    .build();

            // When/Then
            assertThatThrownBy(() -> userService.updateUser(nonExistentId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("User Status Operations")
    class UserStatusOperations {

        @Test
        @DisplayName("Should deactivate user")
        @Transactional
        void shouldDeactivateUser() {
            // Given
            UserResponse created = createTestUser();
            assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // When
            userService.deactivateUser(created.getId());

            // Then
            UserResponse deactivated = userService.getUserById(created.getId());
            assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("Should suspend user")
        @Transactional
        void shouldSuspendUser() {
            // Given
            UserResponse created = createTestUser();
            assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // When
            userService.suspendUser(created.getId());

            // Then
            UserResponse suspended = userService.getUserById(created.getId());
            assertThat(suspended.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Should throw when deactivating non-existent user")
        @Transactional
        void shouldThrowWhenDeactivatingNonExistentUser() {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> userService.deactivateUser(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("User Search Operations")
    class UserSearchOperations {

        @Test
        @DisplayName("Should search users by query")
        @Transactional
        void shouldSearchUsersByQuery() {
            // Given
            String uniquePrefix = "searchtest" + System.currentTimeMillis();
            createTestUserWithUsername(uniquePrefix + "user1", uniquePrefix + "1@test.com");
            createTestUserWithUsername(uniquePrefix + "user2", uniquePrefix + "2@test.com");

            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query(uniquePrefix)
                    .page(0)
                    .size(10)
                    .build();

            // When
            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            // Then
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.getContent()).allMatch(u ->
                    u.getUsername().contains(uniquePrefix) || u.getDisplayName().contains(uniquePrefix));
        }

        @Test
        @DisplayName("Should search users with status filter")
        @Transactional
        void shouldSearchUsersWithStatusFilter() {
            // Given
            String uniquePrefix = "statustest" + System.currentTimeMillis();
            UserResponse active = createTestUserWithUsername(uniquePrefix + "active", uniquePrefix + "active@test.com");
            UserResponse toSuspend = createTestUserWithUsername(uniquePrefix + "suspended", uniquePrefix + "suspended@test.com");
            userService.suspendUser(toSuspend.getId());

            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query(uniquePrefix)
                    .status(UserStatus.ACTIVE)
                    .page(0)
                    .size(10)
                    .build();

            // When
            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            // Then
            assertThat(result.getContent()).allMatch(u -> u.getStatus() == UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty page when no results")
        @Transactional
        void shouldReturnEmptyPageWhenNoResults() {
            // Given
            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query("nonexistentuserquery" + System.currentTimeMillis())
                    .page(0)
                    .size(10)
                    .build();

            // When
            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should get users by IDs")
        @Transactional
        void shouldGetUsersByIds() {
            // Given
            UserResponse user1 = createTestUser();
            UserResponse user2 = createTestUser();
            List<UUID> ids = List.of(user1.getId(), user2.getId());

            // When
            List<UserSummaryResponse> result = userService.getUsersByIds(ids);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserSummaryResponse::getId)
                    .containsExactlyInAnyOrder(user1.getId(), user2.getId());
        }

        @Test
        @DisplayName("Should return empty list when no matching IDs")
        @Transactional
        void shouldReturnEmptyListWhenNoMatchingIds() {
            // Given
            List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

            // When
            List<UserSummaryResponse> result = userService.getUsersByIds(ids);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Profile Operations")
    class ProfileOperations {

        @Test
        @DisplayName("Should get user profile")
        @Transactional
        void shouldGetUserProfile() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException

            // When
            ProfileResponse profile = userService.getProfile(user.getId());

            // Then
            assertThat(profile).isNotNull();
            assertThat(profile.getUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Should update user profile")
        @Transactional
        void shouldUpdateUserProfile() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .title("Software Engineer")
                    .department("Engineering")
                    .location("New York")
                    .bio("Test bio")
                    .pronouns("they/them")
                    .linkedinUrl("https://linkedin.com/in/testuser")
                    .build();

            // When
            ProfileResponse updated = userService.updateProfile(user.getId(), request);

            // Then
            assertThat(updated.getTitle()).isEqualTo("Software Engineer");
            assertThat(updated.getDepartment()).isEqualTo("Engineering");
            assertThat(updated.getLocation()).isEqualTo("New York");
            assertThat(updated.getBio()).isEqualTo("Test bio");
            assertThat(updated.getPronouns()).isEqualTo("they/them");
            assertThat(updated.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/testuser");
        }

        @Test
        @DisplayName("Should update profile with custom status")
        @Transactional
        void shouldUpdateProfileWithCustomStatus() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .customStatus("In a meeting")
                    .statusEmoji("\uD83D\uDCDE")
                    .build();

            // When
            ProfileResponse updated = userService.updateProfile(user.getId(), request);

            // Then
            assertThat(updated.getCustomStatus()).isEqualTo("In a meeting");
            assertThat(updated.getStatusEmoji()).isEqualTo("\uD83D\uDCDE");
        }

        @Test
        @DisplayName("Should throw when getting profile for non-existent user")
        @Transactional
        void shouldThrowWhenGettingProfileForNonExistentUser() {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> userService.getProfile(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Preferences Operations")
    class PreferencesOperations {

        @Test
        @DisplayName("Should get user preferences")
        @Transactional
        void shouldGetUserPreferences() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException

            // When
            PreferencesResponse preferences = userService.getPreferences(user.getId());

            // Then
            assertThat(preferences).isNotNull();
            assertThat(preferences.getUserId()).isEqualTo(user.getId());
            // Check defaults
            assertThat(preferences.isPushEnabled()).isTrue();
            assertThat(preferences.isEmailEnabled()).isTrue();
            assertThat(preferences.getTheme()).isEqualTo("system");
        }

        @Test
        @DisplayName("Should update user preferences")
        @Transactional
        void shouldUpdateUserPreferences() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .pushEnabled(false)
                    .emailEnabled(false)
                    .smsEnabled(true)
                    .theme("dark")
                    .language("fr")
                    .showOnlineStatus(false)
                    .fontSize(16)
                    .build();

            // When
            PreferencesResponse updated = userService.updatePreferences(user.getId(), request);

            // Then
            assertThat(updated.isPushEnabled()).isFalse();
            assertThat(updated.isEmailEnabled()).isFalse();
            assertThat(updated.isSmsEnabled()).isTrue();
            assertThat(updated.getTheme()).isEqualTo("dark");
            assertThat(updated.getLanguage()).isEqualTo("fr");
            assertThat(updated.isShowOnlineStatus()).isFalse();
            assertThat(updated.getFontSize()).isEqualTo(16);
        }

        @Test
        @DisplayName("Should partially update preferences")
        @Transactional
        void shouldPartiallyUpdatePreferences() {
            // Given
            UserResponse user = createTestUser();
            flushAndClear(); // Clear persistence context to avoid DuplicateKeyException
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .theme("light")
                    .build();

            // When
            PreferencesResponse updated = userService.updatePreferences(user.getId(), request);

            // Then
            assertThat(updated.getTheme()).isEqualTo("light");
            // Other fields should retain defaults
            assertThat(updated.isPushEnabled()).isTrue();
            assertThat(updated.isEmailEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw when getting preferences for non-existent user")
        @Transactional
        void shouldThrowWhenGettingPreferencesForNonExistentUser() {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> userService.getPreferences(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // Helper methods
    private UserResponse createTestUser() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test_" + timestamp + "@test.com")
                .username("testuser" + timestamp)
                .displayName("Test User " + timestamp)
                .build();
        return userService.createUser(request);
    }

    private UserResponse createTestUserWithUsername(String username, String email) {
        CreateUserRequest request = CreateUserRequest.builder()
                .email(email)
                .username(username)
                .displayName("Display " + username)
                .build();
        return userService.createUser(request);
    }
}
