package com.quckapp.user.service;

import com.quckapp.user.domain.entity.User;
import com.quckapp.user.domain.entity.User.UserStatus;
import com.quckapp.user.domain.entity.UserPreferences;
import com.quckapp.user.domain.entity.UserProfile;
import com.quckapp.user.domain.repository.UserPreferencesRepository;
import com.quckapp.user.domain.repository.UserProfileRepository;
import com.quckapp.user.domain.repository.UserRepository;
import com.quckapp.user.dto.UserDtos.*;
import com.quckapp.user.exception.DuplicateResourceException;
import com.quckapp.user.exception.ResourceNotFoundException;
import com.quckapp.user.kafka.UserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    private UserService userService;

    private UUID testUserId;
    private User testUser;
    private UserProfile testProfile;
    private UserPreferences testPreferences;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, profileRepository, preferencesRepository, eventPublisher);

        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .phone("+1234567890")
                .timezone("UTC")
                .locale("en")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .title("Engineer")
                .department("Engineering")
                .location("New York")
                .bio("Test bio")
                .build();

        testPreferences = UserPreferences.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .pushEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .theme("dark")
                .language("en")
                .showOnlineStatus(true)
                .fontSize(14)
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .username("newuser")
                    .displayName("New User")
                    .avatarUrl("https://example.com/new-avatar.jpg")
                    .phone("+9876543210")
                    .timezone("America/New_York")
                    .locale("en-US")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                user.setCreatedAt(Instant.now());
                user.setUpdatedAt(Instant.now());
                return user;
            });
            when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            UserResponse result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getDisplayName()).isEqualTo("New User");

            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any(UserProfile.class));
            verify(preferencesRepository).save(any(UserPreferences.class));
            verify(eventPublisher).publishUserCreated(any(User.class));
        }

        @Test
        @DisplayName("should convert email and username to lowercase")
        void shouldConvertToLowercase() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("TEST@EXAMPLE.COM")
                    .username("TESTUSER")
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            userService.createUser(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
            assertThat(userCaptor.getValue().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should use default timezone and locale when not provided")
        void shouldUseDefaultTimezoneAndLocale() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("default@example.com")
                    .username("defaultuser")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            userService.createUser(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getTimezone()).isEqualTo("UTC");
            assertThat(userCaptor.getValue().getLocale()).isEqualTo("en");
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowWhenEmailExists() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("existing@example.com")
                    .username("newuser")
                    .build();

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishUserCreated(any());
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowWhenUsernameExists() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .username("existinguser")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("should get user by ID")
        void shouldGetUserById() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserById(testUserId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should throw exception when user not found by ID")
        void shouldThrowWhenUserNotFoundById() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should get user by email")
        void shouldGetUserByEmail() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserByEmail("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should get user by email case insensitive")
        void shouldGetUserByEmailCaseInsensitive() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            userService.getUserByEmail("TEST@EXAMPLE.COM");

            verify(userRepository).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("should throw exception when user not found by email")
        void shouldThrowWhenUserNotFoundByEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get user by username")
        void shouldGetUserByUsername() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserByUsername("testuser");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should get user by username case insensitive")
        void shouldGetUserByUsernameCaseInsensitive() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            userService.getUserByUsername("TESTUSER");

            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("should throw exception when user not found by username")
        void shouldThrowWhenUserNotFoundByUsername() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Updated Name")
                    .avatarUrl("https://example.com/new-avatar.jpg")
                    .phone("+1111111111")
                    .timezone("Europe/London")
                    .locale("en-GB")
                    .build();

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse result = userService.updateUser(testUserId, request);

            assertThat(result).isNotNull();
            assertThat(testUser.getDisplayName()).isEqualTo("Updated Name");
            assertThat(testUser.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
            assertThat(testUser.getPhone()).isEqualTo("+1111111111");
            assertThat(testUser.getTimezone()).isEqualTo("Europe/London");
            assertThat(testUser.getLocale()).isEqualTo("en-GB");

            verify(userRepository).save(testUser);
            verify(eventPublisher).publishUserUpdated(testUser);
        }

        @Test
        @DisplayName("should only update provided fields")
        void shouldOnlyUpdateProvidedFields() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Only Name Updated")
                    .build();

            String originalTimezone = testUser.getTimezone();
            String originalLocale = testUser.getLocale();

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(testUserId, request);

            assertThat(testUser.getDisplayName()).isEqualTo("Only Name Updated");
            assertThat(testUser.getTimezone()).isEqualTo(originalTimezone);
            assertThat(testUser.getLocale()).isEqualTo(originalLocale);
        }

        @Test
        @DisplayName("should throw exception when user not found for update")
        void shouldThrowWhenUserNotFoundForUpdate() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Updated")
                    .build();

            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(testUserId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishUserUpdated(any());
        }
    }

    @Nested
    @DisplayName("Deactivate User Tests")
    class DeactivateUserTests {

        @Test
        @DisplayName("should deactivate user successfully")
        void shouldDeactivateUserSuccessfully() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            userService.deactivateUser(testUserId);

            assertThat(testUser.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verify(userRepository).save(testUser);
            verify(eventPublisher).publishUserDeactivated(testUser);
        }

        @Test
        @DisplayName("should throw exception when user not found for deactivation")
        void shouldThrowWhenUserNotFoundForDeactivation() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(eventPublisher, never()).publishUserDeactivated(any());
        }
    }

    @Nested
    @DisplayName("Suspend User Tests")
    class SuspendUserTests {

        @Test
        @DisplayName("should suspend user successfully")
        void shouldSuspendUserSuccessfully() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            userService.suspendUser(testUserId);

            assertThat(testUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            verify(userRepository).save(testUser);
            verify(eventPublisher).publishUserSuspended(testUser);
        }

        @Test
        @DisplayName("should throw exception when user not found for suspension")
        void shouldThrowWhenUserNotFoundForSuspension() {
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.suspendUser(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(eventPublisher, never()).publishUserSuspended(any());
        }
    }

    @Nested
    @DisplayName("Search Users Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("should search users with status filter")
        void shouldSearchUsersWithStatusFilter() {
            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query("test")
                    .status(UserStatus.ACTIVE)
                    .page(0)
                    .size(20)
                    .build();

            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.searchUsers(eq("test"), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(userPage);

            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(userRepository).searchUsers(eq("test"), eq(UserStatus.ACTIVE), any(Pageable.class));
        }

        @Test
        @DisplayName("should search users without status filter")
        void shouldSearchUsersWithoutStatusFilter() {
            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query("test")
                    .page(0)
                    .size(20)
                    .build();

            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.searchAllUsers(eq("test"), any(Pageable.class))).thenReturn(userPage);

            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).searchAllUsers(eq("test"), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no results")
        void shouldReturnEmptyPageWhenNoResults() {
            SearchUsersRequest request = SearchUsersRequest.builder()
                    .query("nonexistent")
                    .page(0)
                    .size(20)
                    .build();

            Page<User> emptyPage = new PageImpl<>(List.of());
            when(userRepository.searchAllUsers(anyString(), any(Pageable.class))).thenReturn(emptyPage);

            PageResponse<UserSummaryResponse> result = userService.searchUsers(request);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Get Users By IDs Tests")
    class GetUsersByIdsTests {

        @Test
        @DisplayName("should get users by IDs")
        void shouldGetUsersByIds() {
            UUID userId2 = UUID.randomUUID();
            User user2 = User.builder()
                    .id(userId2)
                    .email("user2@example.com")
                    .username("user2")
                    .displayName("User Two")
                    .status(UserStatus.ACTIVE)
                    .build();

            List<UUID> ids = Arrays.asList(testUserId, userId2);
            when(userRepository.findByIdIn(ids)).thenReturn(Arrays.asList(testUser, user2));

            List<UserSummaryResponse> result = userService.getUsersByIds(ids);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no users found")
        void shouldReturnEmptyListWhenNoUsersFound() {
            List<UUID> ids = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
            when(userRepository.findByIdIn(ids)).thenReturn(List.of());

            List<UserSummaryResponse> result = userService.getUsersByIds(ids);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Profile Tests")
    class ProfileTests {

        @Test
        @DisplayName("should get user profile")
        void shouldGetProfile() {
            testUser.setProfile(testProfile);
            when(userRepository.findByIdWithProfile(testUserId)).thenReturn(Optional.of(testUser));

            ProfileResponse result = userService.getProfile(testUserId);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Engineer");
            assertThat(result.getDepartment()).isEqualTo("Engineering");
            assertThat(result.getLocation()).isEqualTo("New York");
        }

        @Test
        @DisplayName("should create profile if not exists")
        void shouldCreateProfileIfNotExists() {
            testUser.setProfile(null);
            when(userRepository.findByIdWithProfile(testUserId)).thenReturn(Optional.of(testUser));
            when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

            ProfileResponse result = userService.getProfile(testUserId);

            assertThat(result).isNotNull();
            verify(profileRepository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should throw exception when user not found for profile")
        void shouldThrowWhenUserNotFoundForProfile() {
            when(userRepository.findByIdWithProfile(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should update profile successfully")
        void shouldUpdateProfileSuccessfully() {
            testUser.setProfile(testProfile);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .title("Senior Engineer")
                    .department("Platform")
                    .location("San Francisco")
                    .bio("Updated bio")
                    .customStatus("Working on something cool")
                    .statusEmoji("rocket")
                    .pronouns("they/them")
                    .linkedinUrl("https://linkedin.com/in/test")
                    .twitterUrl("https://twitter.com/test")
                    .githubUrl("https://github.com/test")
                    .build();

            when(userRepository.findByIdWithProfile(testUserId)).thenReturn(Optional.of(testUser));
            when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

            ProfileResponse result = userService.updateProfile(testUserId, request);

            assertThat(result).isNotNull();
            assertThat(testProfile.getTitle()).isEqualTo("Senior Engineer");
            assertThat(testProfile.getDepartment()).isEqualTo("Platform");
            assertThat(testProfile.getLocation()).isEqualTo("San Francisco");

            verify(profileRepository).save(testProfile);
            verify(eventPublisher).publishProfileUpdated(eq(testUserId), eq(testProfile));
        }

        @Test
        @DisplayName("should create profile on update if not exists")
        void shouldCreateProfileOnUpdateIfNotExists() {
            testUser.setProfile(null);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .title("New Title")
                    .build();

            when(userRepository.findByIdWithProfile(testUserId)).thenReturn(Optional.of(testUser));
            when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.updateProfile(testUserId, request);

            ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(profileRepository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getTitle()).isEqualTo("New Title");
        }
    }

    @Nested
    @DisplayName("Preferences Tests")
    class PreferencesTests {

        @Test
        @DisplayName("should get user preferences")
        void shouldGetPreferences() {
            testUser.setPreferences(testPreferences);
            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.of(testUser));

            PreferencesResponse result = userService.getPreferences(testUserId);

            assertThat(result).isNotNull();
            assertThat(result.isPushEnabled()).isTrue();
            assertThat(result.isEmailEnabled()).isTrue();
            assertThat(result.isSmsEnabled()).isFalse();
            assertThat(result.getTheme()).isEqualTo("dark");
        }

        @Test
        @DisplayName("should create preferences if not exists")
        void shouldCreatePreferencesIfNotExists() {
            testUser.setPreferences(null);
            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.of(testUser));
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            PreferencesResponse result = userService.getPreferences(testUserId);

            assertThat(result).isNotNull();
            verify(preferencesRepository).save(any(UserPreferences.class));
        }

        @Test
        @DisplayName("should throw exception when user not found for preferences")
        void shouldThrowWhenUserNotFoundForPreferences() {
            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getPreferences(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should update preferences successfully")
        void shouldUpdatePreferencesSuccessfully() {
            testUser.setPreferences(testPreferences);
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .pushEnabled(false)
                    .emailEnabled(false)
                    .smsEnabled(true)
                    .theme("light")
                    .language("fr")
                    .showOnlineStatus(false)
                    .fontSize(16)
                    .customSettings(Map.of("key", "value"))
                    .build();

            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.of(testUser));
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            PreferencesResponse result = userService.getPreferences(testUserId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should only update provided preference fields")
        void shouldOnlyUpdateProvidedPreferenceFields() {
            testUser.setPreferences(testPreferences);
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .theme("light")
                    .build();

            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.of(testUser));
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);

            userService.updatePreferences(testUserId, request);

            assertThat(testPreferences.getTheme()).isEqualTo("light");
            // Original values should be preserved for non-updated fields
            verify(preferencesRepository).save(testPreferences);
            verify(eventPublisher).publishPreferencesUpdated(eq(testUserId), eq(testPreferences));
        }

        @Test
        @DisplayName("should create preferences on update if not exists")
        void shouldCreatePreferencesOnUpdateIfNotExists() {
            testUser.setPreferences(null);
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .theme("light")
                    .build();

            when(userRepository.findByIdWithPreferences(testUserId)).thenReturn(Optional.of(testUser));
            when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.updatePreferences(testUserId, request);

            ArgumentCaptor<UserPreferences> prefsCaptor = ArgumentCaptor.forClass(UserPreferences.class);
            verify(preferencesRepository).save(prefsCaptor.capture());
            assertThat(prefsCaptor.getValue().getTheme()).isEqualTo("light");
        }
    }
}
