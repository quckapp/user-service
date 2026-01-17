package com.quckapp.user.controller;

import com.quckapp.user.dto.UserDtos.*;
import com.quckapp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs for creating, retrieving, updating, and managing users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = """
            Creates a new user account with the provided information.

            **Business Rules:**
            - Email must be unique across all users
            - Username must be unique and contain only alphanumeric characters, underscores, and hyphens
            - A default profile and preferences are automatically created for the user
            - User events are published to Kafka for downstream services
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation failed",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - email or username already exists",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created", userService.createUser(request)));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves complete user information by their unique identifier. Results are cached for performance."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @GetMapping("/email/{email}")
    @Operation(
        summary = "Get user by email",
        description = "Retrieves user information by their email address. Email lookup is case-insensitive."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @Parameter(description = "User email address", example = "john.doe@example.com")
            @PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email)));
    }

    @GetMapping("/username/{username}")
    @Operation(
        summary = "Get user by username",
        description = "Retrieves user information by their username. Username lookup is case-insensitive."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(
            @Parameter(description = "Username", example = "john_doe")
            @PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUsername(username)));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user",
        description = """
            Updates user information. Only provided fields are updated; null fields are ignored.

            **Note:** Email and username cannot be changed through this endpoint.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deactivate user",
        description = """
            Soft-deletes a user by setting their status to INACTIVE.

            **Effects:**
            - User can no longer log in
            - User data is preserved for potential reactivation
            - User events are published to notify other services
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }

    @PostMapping("/{id}/suspend")
    @Operation(
        summary = "Suspend user",
        description = """
            Suspends a user account, typically due to policy violations or security concerns.

            **Effects:**
            - User status set to SUSPENDED
            - User cannot log in or access services
            - Requires admin action to unsuspend
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User suspended"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        userService.suspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("User suspended", null));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search users",
        description = """
            Searches for users by query string with optional status filtering.

            **Search matches against:**
            - Username
            - Display name
            - Email (partial match)

            Results are paginated and sorted by display name.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> searchUsers(
            @Parameter(description = "Search query (min 2 characters)", example = "john", required = true)
            @RequestParam String query,
            @Parameter(description = "Filter by status", example = "ACTIVE",
                schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED", "PENDING_VERIFICATION"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        SearchUsersRequest request = SearchUsersRequest.builder().query(query)
            .status(status != null ? com.quckapp.user.domain.entity.User.UserStatus.valueOf(status.toUpperCase()) : null)
            .page(page).size(size).build();
        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(request)));
    }

    @PostMapping("/batch")
    @Operation(
        summary = "Get users by IDs (batch)",
        description = """
            Retrieves multiple users by their IDs in a single request.

            **Use cases:**
            - Resolving user information for a list of message senders
            - Loading team member details
            - Populating user mentions in content

            **Note:** Non-existent IDs are silently ignored in the response.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved")
    })
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsersByIds(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "List of user UUIDs to retrieve",
                content = @Content(array = @ArraySchema(schema = @Schema(type = "string", format = "uuid")),
                    examples = @ExampleObject(value = "[\"550e8400-e29b-41d4-a716-446655440000\", \"6ba7b810-9dad-11d1-80b4-00c04fd430c8\"]"))
            )
            @RequestBody List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByIds(ids)));
    }

    @GetMapping("/{id}/profile")
    @Operation(
        summary = "Get user profile",
        description = "Retrieves extended profile information for a user including bio, job title, social links, and custom status."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(id)));
    }

    @PatchMapping("/{id}/profile")
    @Operation(
        summary = "Update user profile",
        description = """
            Updates user profile information. Only provided fields are updated.

            **Updatable fields:**
            - Job title, department, location
            - Bio/about me
            - Custom status with emoji and expiry
            - Social links (LinkedIn, Twitter, GitHub)
            - Personal info (pronouns, birthday)
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(id, request)));
    }

    @GetMapping("/{id}/preferences")
    @Operation(
        summary = "Get user preferences",
        description = """
            Retrieves user preferences including:
            - Notification settings (push, email, SMS, desktop)
            - UI preferences (theme, compact mode, font size)
            - Privacy settings (online status, typing indicators, read receipts)
            - Accessibility options (reduced motion, high contrast)
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferences retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<PreferencesResponse>> getPreferences(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getPreferences(id)));
    }

    @PatchMapping("/{id}/preferences")
    @Operation(
        summary = "Update user preferences",
        description = """
            Updates user preferences. Only provided fields are updated; null fields retain their current values.

            **Categories:**
            - **Notifications:** push, email, SMS, desktop, sounds, quiet hours
            - **Display:** theme, language, compact mode, font size
            - **Privacy:** online status visibility, typing indicators, read receipts
            - **Accessibility:** reduced motion, high contrast
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferences updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<PreferencesResponse>> updatePreferences(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", userService.updatePreferences(id, request)));
    }
}
