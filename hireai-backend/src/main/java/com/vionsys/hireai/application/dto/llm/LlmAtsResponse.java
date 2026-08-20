package com.vionsys.hireai.application.dto.llm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmAtsResponse {

    private int atsScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String aiSummary;
    private boolean recommended;
    private String recommendation;
}
