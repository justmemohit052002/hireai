package com.vionsys.hireai.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for OAuth Single Sign-On (Google or LinkedIn)")
public class OAuthRequest {

    @NotBlank(message = "OAuth token or credential is required")
    @Schema(description = "ID token or access token from Google/LinkedIn, or simulated token", example = "eyJhbGciOiJSUzI1NiIs...")
    private String token;

    @Schema(description = "Role for account registration (CANDIDATE or RECRUITER)", example = "CANDIDATE")
    private String role;

    @Schema(description = "Company name (required only when registering as RECRUITER)", example = "Vionsys Technologies")
    private String companyName;

    @Schema(description = "User email provided by provider", example = "user@example.com")
    private String email;

    @Schema(description = "First name of user", example = "John")
    private String firstName;

    @Schema(description = "Last name of user", example = "Doe")
    private String lastName;

    @Schema(description = "Profile avatar URL", example = "https://lh3.googleusercontent.com/...")
    private String avatarUrl;
}
