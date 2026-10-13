package com.vionsys.hireai.admin.dto;

import java.util.List;
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
public class AdminCallLogsResponse {
    private Boolean available;
    private String message;
    private List<AdminCallLogEntry> items;
    private Long totalElements;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
