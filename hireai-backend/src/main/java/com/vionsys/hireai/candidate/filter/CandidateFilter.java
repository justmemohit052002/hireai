package com.vionsys.hireai.candidate.filter;

import java.math.BigDecimal;

import com.vionsys.hireai.candidate.enums.CandidateStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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