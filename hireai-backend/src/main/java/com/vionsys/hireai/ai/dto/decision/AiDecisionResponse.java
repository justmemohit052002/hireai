package com.vionsys.hireai.ai.dto.decision;

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
public class AiDecisionResponse {
    private int finalScore;
    private String classification; // "shortlist" | "hold" | "reject"
    private Object breakdown;
    private String explanation;
}
