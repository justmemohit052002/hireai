package com.vionsys.hireai.chat.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
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
public class CreateConversationRequest {

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    private UUID jobId;

    private UUID jobApplicationId;

    private String initialMessage;
}
