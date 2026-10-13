package com.vionsys.hireai.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminAuditLogResponse {
    private UUID id;
    private UUID actorUserId;
    private String actorEmail;
    private String action;
    private UUID targetUserId;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
