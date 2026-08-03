package com.vionsys.hireai.recruiter.dto;

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
public class RecruiterProfileRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150)
    private String companyName;

    @Size(max = 100)
    private String designation;

    @Size(max = 500)
    private String companyWebsite;

    @Email(message = "Invalid company email")
    @Size(max = 150)
    private String companyEmail;

    @Size(max = 20)
    private String companyPhone;

    @Size(max = 500)
    private String companyLogoUrl;

    @Size(max = 1000)
    private String companyDescription;

    @Size(max = 100)
    private String industry;

    private Integer companySize;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String city;

    @Size(max = 500)
    private String address;

}