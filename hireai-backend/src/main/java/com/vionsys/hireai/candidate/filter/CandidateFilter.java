package com.vionsys.hireai.candidate.filter;

import java.math.BigDecimal;

import com.vionsys.hireai.candidate.enums.CandidateStatus;

import lombok.Data;

@Data
public class CandidateFilter {

	  private String candidateId;

	    private String firstName;

	    private String lastName;

	    private String email;

	    private String phone;

	    private String location;

	    private CandidateStatus candidateStatus;

	    private BigDecimal experience;

	    private String skill;
}
