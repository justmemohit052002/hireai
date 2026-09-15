package com.vionsys.hireai.recruiter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Recruiter Profile Creation and Update Request Payload")
public class RecruiterProfileRequest {

    @Schema(description = "Registered legal name of the hiring organization", example = "Cyberdyne Systems Inc.")
    @NotBlank(message = "Company name is required")
    @Size(max = 150)
    private String companyName;

    @Schema(description = "Recruiter's internal designation/title", example = "Lead Talent Acquisition Specialist")
    @Size(max = 100)
    private String designation;

    @Schema(description = "Official company website URL", example = "https://www.cyberdyne.com")
    @Size(max = 500)
    private String companyWebsite;

    @Schema(description = "Company contact / support email", example = "careers@cyberdyne.com")
    @Email(message = "Invalid company email")
    @Size(max = 150)
    private String companyEmail;

    @Schema(description = "Company phone number", example = "+1-555-0199")
    @Size(max = 20)
    private String companyPhone;

    @Schema(description = "Company logo URL", example = "https://www.cyberdyne.com/assets/logo.png")
    @Size(max = 500)
    private String companyLogoUrl;

    @Schema(description = "About the company / mission statement", example = "Cyberdyne Systems is a leading artificial intelligence and automation technology research and product engineering firm.")
    @Size(max = 1000)
    private String companyDescription;

    @Schema(description = "Industry vertical", example = "Information Technology & AI")
    @Size(max = 100)
    private String industry;

    @Schema(description = "Approximate total employee count", example = "250")
    private Integer companySize;

    @Schema(description = "Headquarters Country", example = "India")
    @Size(max = 100)
    private String country;

    @Schema(description = "State / Province", example = "Maharashtra")
    @Size(max = 100)
    private String state;

    @Schema(description = "City", example = "Pune")
    @Size(max = 100)
    private String city;

    @Schema(description = "Headquarters street address", example = "Cyber Park, Tower 4, Viman Nagar")
    @Size(max = 500)
    private String address;

}