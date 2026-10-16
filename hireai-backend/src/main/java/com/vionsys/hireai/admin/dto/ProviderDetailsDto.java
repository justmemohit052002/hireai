package com.vionsys.hireai.admin.dto;

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
public class ProviderDetailsDto {
    private String name;
    private Boolean isConfigured;
    private String keyMasked;
    private Boolean reachable;
    private String model;
}
