package com.vionsys.hireai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleHealthStateDto {
    private String id;
    private String name;
    private String status; // UP / DEGRADED / DOWN
    private String provider;
    private String model;
    private String lastSuccessAt;
    private String lastFailureAt;
    private long durationInCurrentStateSeconds;
    private String error;
}
