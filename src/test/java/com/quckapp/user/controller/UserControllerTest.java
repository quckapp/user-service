package com.quckapp.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quckapp.user.domain.entity.User.UserStatus;
import com.quckapp.user.dto.UserDtos.*;
import com.quckapp.user.exception.DuplicateResourceException;
import com.quckapp.user.exception.GlobalExceptionHandler;
import com.quckapp.user.exception.ResourceNotFoundException;
import com.quckapp.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UserResponse testUserResponse;
    private UserSummaryResponse testUserSummary;
    private ProfileResponse testProfileResponse;
    private PreferencesResponse testPreferencesResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUserId = UUID.randomUUID();
        testUserResponse = UserResponse.builder()
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

        testUserSummary = UserSummaryResponse.builder()
                .id(testUserId)
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .status(UserStatus.ACTIVE)
                .build();

        testProfileResponse = ProfileResponse.builder()
                .userId(testUserId)
                .title("Engineer")
                .department("Engineering")
                .location("New York")
                .bio("Test bio")
                .updatedAt(Instant.now())
                .build();

        testPreferencesResponse = PreferencesResponse.builder()
                .userId(testUserId)
                .pushEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .theme("dark")
                .language("en")
                .showOnlineStatus(true)
                .fontSize(14)
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() throws Exception {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .username("newuser")
                    .displayName("New User")
                    .build();

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User created"))
                    .andExpect(jsonPath("$.data.id").value(testUserId.toString()));

            verify(userService).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("should return 400 when email already exists")
        void shouldReturn400WhenEmailExists() throws Exception {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("existing@example.com")
                    .username("newuser")
                    .build();

            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new DuplicateResourceException("Email already exists"));

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("should get user by ID")
        void shouldGetUserById() throws Exception {
            when(userService.getUserById(testUserId)).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/users/{id}", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));

            verify(userService).getUserById(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found by ID")
        void shouldReturn404WhenUserNotFoundById() throws Exception {
            when(userService.getUserById(testUserId))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/{id}", testUserId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should get user by email")
        void shouldGetUserByEmail() throws Exception {
            when(userService.getUserByEmail("test@example.com")).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/users/email/{email}", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("test@example.com"));

            verify(userService).getUserByEmail("test@example.com");
        }

        @Test
        @DisplayName("should return 404 when user not found by email")
        void shouldReturn404WhenUserNotFoundByEmail() throws Exception {
            when(userService.getUserByEmail("unknown@example.com"))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/email/{email}", "unknown@example.com"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should get user by username")
        void shouldGetUserByUsername() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/users/username/{username}", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("testuser"));

            verify(userService).getUserByUsername("testuser");
        }

        @Test
        @DisplayName("should return 404 when user not found by username")
        void shouldReturn404WhenUserNotFoundByUsername() throws Exception {
            when(userService.getUserByUsername("unknown"))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/username/{username}", "unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Updated Name")
                    .timezone("America/New_York")
                    .build();

            when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class)))
                    .thenReturn(testUserResponse);

            mockMvc.perform(put("/api/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User updated"));

            verify(userService).updateUser(eq(testUserId), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("should return 404 when user not found for update")
        void shouldReturn404WhenUserNotFoundForUpdate() throws Exception {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .displayName("Updated")
                    .build();

            when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(put("/api/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Deactivate User Tests")
    class DeactivateUserTests {

        @Test
        @DisplayName("should deactivate user successfully")
        void shouldDeactivateUserSuccessfully() throws Exception {
            doNothing().when(userService).deactivateUser(testUserId);

            mockMvc.perform(delete("/api/users/{id}", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User deactivated"));

            verify(userService).deactivateUser(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found for deactivation")
        void shouldReturn404WhenUserNotFoundForDeactivation() throws Exception {
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userService).deactivateUser(testUserId);

            mockMvc.perform(delete("/api/users/{id}", testUserId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Suspend User Tests")
    class SuspendUserTests {

        @Test
        @DisplayName("should suspend user successfully")
        void shouldSuspendUserSuccessfully() throws Exception {
            doNothing().when(userService).suspendUser(testUserId);

            mockMvc.perform(post("/api/users/{id}/suspend", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User suspended"));

            verify(userService).suspendUser(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found for suspension")
        void shouldReturn404WhenUserNotFoundForSuspension() throws Exception {
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userService).suspendUser(testUserId);

            mockMvc.perform(post("/api/users/{id}/suspend", testUserId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Search Users Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("should search users successfully")
        void shouldSearchUsersSuccessfully() throws Exception {
            PageResponse<UserSummaryResponse> pageResponse = PageResponse.<UserSummaryResponse>builder()
                    .content(List.of(testUserSummary))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(userService.searchUsers(any(SearchUsersRequest.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/users/search")
                            .param("query", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].username").value("testuser"));

            verify(userService).searchUsers(any(SearchUsersRequest.class));
        }

        @Test
        @DisplayName("should search users with status filter")
        void shouldSearchUsersWithStatusFilter() throws Exception {
            PageResponse<UserSummaryResponse> pageResponse = PageResponse.<UserSummaryResponse>builder()
                    .content(List.of(testUserSummary))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(userService.searchUsers(any(SearchUsersRequest.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/users/search")
                            .param("query", "test")
                            .param("status", "ACTIVE")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(userService).searchUsers(any(SearchUsersRequest.class));
        }
    }

    @Nested
    @DisplayName("Batch Get Users Tests")
    class BatchGetUsersTests {

        @Test
        @DisplayName("should get users by IDs")
        void shouldGetUsersByIds() throws Exception {
            List<UUID> ids = Arrays.asList(testUserId, UUID.randomUUID());
            when(userService.getUsersByIds(anyList())).thenReturn(List.of(testUserSummary));

            mockMvc.perform(post("/api/users/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());

            verify(userService).getUsersByIds(anyList());
        }

        @Test
        @DisplayName("should return empty list when no users found")
        void shouldReturnEmptyListWhenNoUsersFound() throws Exception {
            List<UUID> ids = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
            when(userService.getUsersByIds(anyList())).thenReturn(List.of());

            mockMvc.perform(post("/api/users/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("Profile Tests")
    class ProfileTests {

        @Test
        @DisplayName("should get user profile")
        void shouldGetProfile() throws Exception {
            when(userService.getProfile(testUserId)).thenReturn(testProfileResponse);

            mockMvc.perform(get("/api/users/{id}/profile", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Engineer"))
                    .andExpect(jsonPath("$.data.department").value("Engineering"))
                    .andExpect(jsonPath("$.data.location").value("New York"));

            verify(userService).getProfile(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found for profile")
        void shouldReturn404WhenUserNotFoundForProfile() throws Exception {
            when(userService.getProfile(testUserId))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/{id}/profile", testUserId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should update profile successfully")
        void shouldUpdateProfileSuccessfully() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .title("Senior Engineer")
                    .department("Platform")
                    .location("San Francisco")
                    .bio("Updated bio")
                    .build();

            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class)))
                    .thenReturn(testProfileResponse);

            mockMvc.perform(patch("/api/users/{id}/profile", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"));

            verify(userService).updateProfile(eq(testUserId), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("should return 404 when user not found for profile update")
        void shouldReturn404WhenUserNotFoundForProfileUpdate() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .title("Updated")
                    .build();

            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(patch("/api/users/{id}/profile", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Preferences Tests")
    class PreferencesTests {

        @Test
        @DisplayName("should get user preferences")
        void shouldGetPreferences() throws Exception {
            when(userService.getPreferences(testUserId)).thenReturn(testPreferencesResponse);

            mockMvc.perform(get("/api/users/{id}/preferences", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pushEnabled").value(true))
                    .andExpect(jsonPath("$.data.theme").value("dark"))
                    .andExpect(jsonPath("$.data.language").value("en"));

            verify(userService).getPreferences(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found for preferences")
        void shouldReturn404WhenUserNotFoundForPreferences() throws Exception {
            when(userService.getPreferences(testUserId))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/{id}/preferences", testUserId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should update preferences successfully")
        void shouldUpdatePreferencesSuccessfully() throws Exception {
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .pushEnabled(false)
                    .theme("light")
                    .language("fr")
                    .build();

            when(userService.updatePreferences(eq(testUserId), any(UpdatePreferencesRequest.class)))
                    .thenReturn(testPreferencesResponse);

            mockMvc.perform(patch("/api/users/{id}/preferences", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Preferences updated"));

            verify(userService).updatePreferences(eq(testUserId), any(UpdatePreferencesRequest.class));
        }

        @Test
        @DisplayName("should return 404 when user not found for preferences update")
        void shouldReturn404WhenUserNotFoundForPreferencesUpdate() throws Exception {
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .theme("light")
                    .build();

            when(userService.updatePreferences(eq(testUserId), any(UpdatePreferencesRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(patch("/api/users/{id}/preferences", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
