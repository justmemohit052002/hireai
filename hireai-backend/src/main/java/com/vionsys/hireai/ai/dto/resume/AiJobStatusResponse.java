package com.vionsys.hireai.ai.dto.resume;

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
public class AiJobStatusResponse {
    private String jobId;
    private String status; // "processing" | "complete" | "failed"
    private AiResumeParsedResult result;
}
