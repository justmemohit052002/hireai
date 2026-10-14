package com.vionsys.hireai.ai.dto.match;

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
public class AiMatchDetailDto {
    private String requiredSkill;
    private String status;
    private String matchedWith;
    private String matchType;
    private Double confidence;
    private String evidence;
}
