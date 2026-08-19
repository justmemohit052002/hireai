package com.vionsys.hireai.ai.dto.chatbot;

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
public class AiExtractedFields {
    private String currentCtc;
    private String expectedCtc;
    private String noticePeriod;
    private String availability;
}
