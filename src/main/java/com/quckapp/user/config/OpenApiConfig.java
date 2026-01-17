package com.quckapp.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for QuckApp User Service.
 *
 * Provides comprehensive API documentation with security schemes,
 * server configurations, and grouped API endpoints.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "QuckApp User Service API",
        version = "1.0.0",
        description = """
            ## User Management Service

            The QuckApp User Service provides user management capabilities for the QuckApp ecosystem.
            This service handles user CRUD operations, profile management, and user preferences.

            ### Features
            - **User Management** - Create, read, update, and delete users
            - **Profile Management** - Extended user profile with bio, social links, etc.
            - **User Preferences** - Notification settings, theme preferences, privacy settings
            - **User Search** - Search users by query with filtering and pagination
            - **Batch Operations** - Retrieve multiple users by IDs

            ### Authentication
            This service is typically called by other microservices. Authentication is handled
            via API Key or internal service mesh authentication.

            ### Data Flow
            - User events are published to Kafka for other services to consume
            - Redis caching for frequently accessed user data
            - MySQL for persistent storage with Flyway migrations
            """,
        contact = @Contact(
            name = "QuckApp Team",
            email = "support@quckapp.com",
            url = "https://quckapp.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/", description = "User Service Base Path"),
        @Server(url = "http://localhost:8082", description = "Local Development"),
        @Server(url = "https://api.quckapp.com/users", description = "Production")
    },
    tags = {
        @Tag(name = "Users", description = "Core user management operations - create, read, update, delete"),
        @Tag(name = "Profile", description = "User profile management - bio, avatar, social links"),
        @Tag(name = "Preferences", description = "User preferences - notifications, theme, privacy settings"),
        @Tag(name = "Search", description = "User search and discovery endpoints"),
        @Tag(name = "Batch", description = "Batch operations for retrieving multiple users"),
        @Tag(name = "Admin", description = "Administrative operations - suspend, deactivate users")
    }
)
@SecuritySchemes({
    @SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        parameterName = "X-API-Key",
        description = """
            API Key authentication for internal services.

            Used for service-to-service communication within the QuckApp ecosystem.
            Include the key in the X-API-Key header:
            `X-API-Key: <your-api-key>`
            """
    ),
    @SecurityScheme(
        name = "serviceAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        parameterName = "X-Service-Name",
        description = """
            Service mesh authentication.

            Internal services identify themselves via X-Service-Name header.
            This is typically injected by the service mesh (e.g., Istio, Consul Connect).
            """
    )
})
public class OpenApiConfig {

    /**
     * User CRUD operations
     */
    @Bean
    public GroupedOpenApi usersApi() {
        return GroupedOpenApi.builder()
            .group("1. Users")
            .displayName("User Management")
            .pathsToMatch("/api/users", "/api/users/{id}")
            .pathsToExclude("/api/users/{id}/profile", "/api/users/{id}/preferences", "/api/users/search", "/api/users/batch")
            .build();
    }

    /**
     * Profile management endpoints
     */
    @Bean
    public GroupedOpenApi profileApi() {
        return GroupedOpenApi.builder()
            .group("2. Profile")
            .displayName("Profile Management")
            .pathsToMatch("/api/users/{id}/profile")
            .build();
    }

    /**
     * Preferences management endpoints
     */
    @Bean
    public GroupedOpenApi preferencesApi() {
        return GroupedOpenApi.builder()
            .group("3. Preferences")
            .displayName("User Preferences")
            .pathsToMatch("/api/users/{id}/preferences")
            .build();
    }

    /**
     * Search and discovery endpoints
     */
    @Bean
    public GroupedOpenApi searchApi() {
        return GroupedOpenApi.builder()
            .group("4. Search")
            .displayName("Search & Discovery")
            .pathsToMatch("/api/users/search", "/api/users/email/**", "/api/users/username/**")
            .build();
    }

    /**
     * Batch operations
     */
    @Bean
    public GroupedOpenApi batchApi() {
        return GroupedOpenApi.builder()
            .group("5. Batch")
            .displayName("Batch Operations")
            .pathsToMatch("/api/users/batch")
            .build();
    }

    /**
     * Admin operations
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("6. Admin")
            .displayName("Admin Operations")
            .pathsToMatch("/api/users/{id}/suspend", "/api/users/{id}/activate")
            .build();
    }

    /**
     * All endpoints in one view
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("0. All Endpoints")
            .displayName("All Endpoints")
            .pathsToMatch("/api/**")
            .build();
    }
}
