package tz.go.tirdo.teltp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Request/response records for authentication and user management. */
public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            String firstName,
            String lastName,
            String phoneNumber,
            String profession,
            boolean dataProcessingConsent) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn) {}

    public record UserResponse(
            String uuid,
            String username,
            String email,
            String fullName,
            String profession,
            String organizationUuid,
            boolean active,
            Set<String> roles) {}

    public record AssignRolesRequest(@NotBlank String userUuid, Set<String> roles) {}
}
