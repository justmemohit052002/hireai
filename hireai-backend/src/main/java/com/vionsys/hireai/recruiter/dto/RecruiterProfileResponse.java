package com.vionsys.hireai.recruiter.dto;

import java.util.UUID;

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
public class RecruiterProfileResponse {

    private UUID id;

    private UUID userId;

    private String companyName;

    private String designation;

    private String companyWebsite;

    private String companyEmail;

    private String companyPhone;

    private String companyLogoUrl;

    private String companyDescription;

    private String industry;

    private Integer companySize;

    private String country;

    private String state;

    private String city;

    private String address;

    private Boolean verified;

}