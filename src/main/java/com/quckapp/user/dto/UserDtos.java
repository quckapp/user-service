package com.quckapp.user.dto;

import com.quckapp.user.domain.entity.User.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Request payload for creating a new user")
    public static class CreateUserRequest {
        @NotBlank @Email
        @Schema(description = "User's email address (must be unique)", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
        @Schema(description = "Unique username (3-50 chars, alphanumeric with _ and -)", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @Size(max = 100)
        @Schema(description = "Display name shown in UI", example = "John Doe")
        private String displayName;

        @Size(max = 500)
        @Schema(description = "URL to user's avatar image", example = "https://cdn.quckapp.com/avatars/john.png")
        private String avatarUrl;

        @Schema(description = "Phone number with country code", example = "+1234567890")
        private String phone;

        @Schema(description = "IANA timezone identifier", example = "America/New_York", defaultValue = "UTC")
        private String timezone;

        @Schema(description = "Locale/language preference", example = "en-US", defaultValue = "en")
        private String locale;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Request payload for updating user details")
    public static class UpdateUserRequest {
        @Size(max = 100)
        @Schema(description = "Updated display name", example = "Johnny Doe")
        private String displayName;

        @Size(max = 500)
        @Schema(description = "Updated avatar URL", example = "https://cdn.quckapp.com/avatars/johnny.png")
        private String avatarUrl;

        @Schema(description = "Updated phone number", example = "+1987654321")
        private String phone;

        @Schema(description = "Updated timezone", example = "Europe/London")
        private String timezone;

        @Schema(description = "Updated locale", example = "en-GB")
        private String locale;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Complete user information response")
    public static class UserResponse {
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID id;

        @Schema(description = "User's email address", example = "john.doe@example.com")
        private String email;

        @Schema(description = "Unique username", example = "john_doe")
        private String username;

        @Schema(description = "Display name shown in UI", example = "John Doe")
        private String displayName;

        @Schema(description = "Avatar image URL", example = "https://cdn.quckapp.com/avatars/john.png")
        private String avatarUrl;

        @Schema(description = "Phone number", example = "+1234567890")
        private String phone;

        @Schema(description = "User's timezone", example = "America/New_York")
        private String timezone;

        @Schema(description = "User's locale preference", example = "en-US")
        private String locale;

        @Schema(description = "Current account status", example = "ACTIVE")
        private UserStatus status;

        @Schema(description = "Whether email has been verified", example = "true")
        private boolean emailVerified;

        @Schema(description = "Whether phone has been verified", example = "false")
        private boolean phoneVerified;

        @Schema(description = "Timestamp of last login")
        private Instant lastLoginAt;

        @Schema(description = "Account creation timestamp")
        private Instant createdAt;

        @Schema(description = "Last update timestamp")
        private Instant updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Abbreviated user information for listings and search results")
    public static class UserSummaryResponse {
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID id;

        @Schema(description = "Unique username", example = "john_doe")
        private String username;

        @Schema(description = "Display name shown in UI", example = "John Doe")
        private String displayName;

        @Schema(description = "Avatar image URL", example = "https://cdn.quckapp.com/avatars/john.png")
        private String avatarUrl;

        @Schema(description = "Current account status", example = "ACTIVE")
        private UserStatus status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Request payload for updating user profile")
    public static class UpdateProfileRequest {
        @Size(max = 100)
        @Schema(description = "Job title", example = "Software Engineer")
        private String title;

        @Size(max = 100)
        @Schema(description = "Department or team", example = "Engineering")
        private String department;

        @Size(max = 100)
        @Schema(description = "Location/office", example = "New York, NY")
        private String location;

        @Size(max = 1000)
        @Schema(description = "User biography/about me", example = "Full-stack developer passionate about clean code")
        private String bio;

        @Size(max = 200)
        @Schema(description = "Custom status message", example = "In a meeting")
        private String customStatus;

        @Size(max = 10)
        @Schema(description = "Status emoji", example = "📞")
        private String statusEmoji;

        @Schema(description = "When the custom status expires")
        private Instant statusExpiry;

        @Schema(description = "User's pronouns", example = "they/them")
        private String pronouns;

        @Schema(description = "User's birthday")
        private Instant birthday;

        @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/johndoe")
        private String linkedinUrl;

        @Schema(description = "Twitter/X profile URL", example = "https://twitter.com/johndoe")
        private String twitterUrl;

        @Schema(description = "GitHub profile URL", example = "https://github.com/johndoe")
        private String githubUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "User profile information response")
    public static class ProfileResponse {
        @Schema(description = "User ID this profile belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID userId;

        @Schema(description = "Job title", example = "Software Engineer")
        private String title;

        @Schema(description = "Department or team", example = "Engineering")
        private String department;

        @Schema(description = "Location/office", example = "New York, NY")
        private String location;

        @Schema(description = "User biography", example = "Full-stack developer passionate about clean code")
        private String bio;

        @Schema(description = "Custom status message", example = "In a meeting")
        private String customStatus;

        @Schema(description = "Status emoji", example = "📞")
        private String statusEmoji;

        @Schema(description = "When the custom status expires")
        private Instant statusExpiry;

        @Schema(description = "User's pronouns", example = "they/them")
        private String pronouns;

        @Schema(description = "User's birthday")
        private Instant birthday;

        @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/johndoe")
        private String linkedinUrl;

        @Schema(description = "Twitter/X profile URL", example = "https://twitter.com/johndoe")
        private String twitterUrl;

        @Schema(description = "GitHub profile URL", example = "https://github.com/johndoe")
        private String githubUrl;

        @Schema(description = "Last update timestamp")
        private Instant updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Request payload for updating user preferences")
    public static class UpdatePreferencesRequest {
        @Schema(description = "Enable push notifications", example = "true")
        private Boolean pushEnabled;

        @Schema(description = "Enable email notifications", example = "true")
        private Boolean emailEnabled;

        @Schema(description = "Enable SMS notifications", example = "false")
        private Boolean smsEnabled;

        @Schema(description = "Enable desktop notifications", example = "true")
        private Boolean desktopNotifications;

        @Schema(description = "Enable notification sounds", example = "true")
        private Boolean soundEnabled;

        @Schema(description = "Quiet hours start time", example = "22:00")
        private LocalTime quietHoursStart;

        @Schema(description = "Quiet hours end time", example = "08:00")
        private LocalTime quietHoursEnd;

        @Schema(description = "Enable quiet hours", example = "false")
        private Boolean quietHoursEnabled;

        @Schema(description = "UI theme preference", example = "dark", allowableValues = {"light", "dark", "system"})
        private String theme;

        @Schema(description = "Language preference", example = "en")
        private String language;

        @Schema(description = "Enable compact UI mode", example = "false")
        private Boolean compactMode;

        @Schema(description = "Collapse sidebar by default", example = "false")
        private Boolean sidebarCollapsed;

        @Schema(description = "Show only unread messages", example = "false")
        private Boolean showUnreadOnly;

        @Schema(description = "Show message previews in notifications", example = "true")
        private Boolean messagePreview;

        @Schema(description = "Use Enter key to send messages", example = "true")
        private Boolean enterToSend;

        @Schema(description = "Enable markdown rendering", example = "true")
        private Boolean markdownEnabled;

        @Schema(description = "Enable emoji suggestions while typing", example = "true")
        private Boolean emojiSuggestionsEnabled;

        @Schema(description = "Show online/offline status to others", example = "true")
        private Boolean showOnlineStatus;

        @Schema(description = "Show typing indicator to others", example = "true")
        private Boolean showTypingIndicator;

        @Schema(description = "Show read receipts to others", example = "true")
        private Boolean showReadReceipts;

        @Schema(description = "Reduce motion/animations for accessibility", example = "false")
        private Boolean reducedMotion;

        @Schema(description = "Enable high contrast mode for accessibility", example = "false")
        private Boolean highContrast;

        @Min(10) @Max(24)
        @Schema(description = "Font size in pixels (10-24)", example = "14", minimum = "10", maximum = "24")
        private Integer fontSize;

        @Schema(description = "Custom settings key-value pairs")
        private Map<String, Object> customSettings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "User preferences response")
    public static class PreferencesResponse {
        @Schema(description = "User ID these preferences belong to", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID userId;

        @Schema(description = "Push notifications enabled", example = "true")
        private boolean pushEnabled;

        @Schema(description = "Email notifications enabled", example = "true")
        private boolean emailEnabled;

        @Schema(description = "SMS notifications enabled", example = "false")
        private boolean smsEnabled;

        @Schema(description = "Desktop notifications enabled", example = "true")
        private boolean desktopNotifications;

        @Schema(description = "Notification sounds enabled", example = "true")
        private boolean soundEnabled;

        @Schema(description = "Quiet hours start time", example = "22:00")
        private LocalTime quietHoursStart;

        @Schema(description = "Quiet hours end time", example = "08:00")
        private LocalTime quietHoursEnd;

        @Schema(description = "Quiet hours enabled", example = "false")
        private boolean quietHoursEnabled;

        @Schema(description = "UI theme", example = "dark")
        private String theme;

        @Schema(description = "Language preference", example = "en")
        private String language;

        @Schema(description = "Compact UI mode enabled", example = "false")
        private boolean compactMode;

        @Schema(description = "Sidebar collapsed", example = "false")
        private boolean sidebarCollapsed;

        @Schema(description = "Show only unread messages", example = "false")
        private boolean showUnreadOnly;

        @Schema(description = "Show message previews", example = "true")
        private boolean messagePreview;

        @Schema(description = "Enter to send enabled", example = "true")
        private boolean enterToSend;

        @Schema(description = "Markdown enabled", example = "true")
        private boolean markdownEnabled;

        @Schema(description = "Emoji suggestions enabled", example = "true")
        private boolean emojiSuggestionsEnabled;

        @Schema(description = "Show online status", example = "true")
        private boolean showOnlineStatus;

        @Schema(description = "Show typing indicator", example = "true")
        private boolean showTypingIndicator;

        @Schema(description = "Show read receipts", example = "true")
        private boolean showReadReceipts;

        @Schema(description = "Reduced motion enabled", example = "false")
        private boolean reducedMotion;

        @Schema(description = "High contrast mode enabled", example = "false")
        private boolean highContrast;

        @Schema(description = "Font size in pixels", example = "14")
        private int fontSize;

        @Schema(description = "Custom settings")
        private Map<String, Object> customSettings;

        @Schema(description = "Last update timestamp")
        private Instant updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "User search request parameters")
    public static class SearchUsersRequest {
        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "Search query (matches username, display name, email)", example = "john", requiredMode = Schema.RequiredMode.REQUIRED)
        private String query;

        @Schema(description = "Filter by user status", example = "ACTIVE")
        private UserStatus status;

        @Min(0)
        @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
        private Integer page = 0;

        @Min(1) @Max(100)
        @Schema(description = "Page size (1-100)", example = "20", defaultValue = "20")
        private Integer size = 20;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Paginated response wrapper")
    public static class PageResponse<T> {
        @Schema(description = "List of items in the current page")
        private List<T> content;

        @Schema(description = "Current page number (0-indexed)", example = "0")
        private int page;

        @Schema(description = "Page size", example = "20")
        private int size;

        @Schema(description = "Total number of items across all pages", example = "150")
        private long totalElements;

        @Schema(description = "Total number of pages", example = "8")
        private int totalPages;

        @Schema(description = "Whether this is the first page", example = "true")
        private boolean first;

        @Schema(description = "Whether this is the last page", example = "false")
        private boolean last;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Standard API response wrapper")
    public static class ApiResponse<T> {
        @Schema(description = "Whether the request was successful", example = "true")
        private boolean success;

        @Schema(description = "Human-readable message", example = "User created successfully")
        private String message;

        @Schema(description = "Response payload")
        private T data;

        @Schema(description = "Response timestamp")
        private Instant timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().success(true).data(data).timestamp(Instant.now()).build();
        }
        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).timestamp(Instant.now()).build();
        }
        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).timestamp(Instant.now()).build();
        }
    }
}
