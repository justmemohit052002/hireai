package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidateRequest {
                       
	  @Size(max = 50)
	    private String firstName;

	    @Size(max = 50)
	    private String lastName;

	    @Email
	    private String email;

	    @Pattern(regexp = "^[6-9]\\d{9}$")
	    private String phone;

	    private String linkedinUrl;

	    private String githubUrl;

	    private String portfolioUrl;

	    private String currentCompany;

	    private String currentDesignation;

	    @PositiveOrZero
	    private BigDecimal experience;

	    @PositiveOrZero
	    private BigDecimal currentCtc;

	    @PositiveOrZero
	    private BigDecimal expectedCtc;

	    @PositiveOrZero
	    private Integer noticePeriod;

	    private String location;
}
