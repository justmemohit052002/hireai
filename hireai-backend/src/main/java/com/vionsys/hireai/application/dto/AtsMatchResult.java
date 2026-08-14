package com.vionsys.hireai.application.dto;

import java.util.List;

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
public class AtsMatchResult {

    private int matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private double skillMatchPercentage;
    private double experienceMatchPercentage;
}
