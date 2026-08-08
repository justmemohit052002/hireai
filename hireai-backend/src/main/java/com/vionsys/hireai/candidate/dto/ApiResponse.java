package com.vionsys.hireai.candidate.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
 
	 private boolean success;

	    private String message;

	    private T data;

	    @Builder.Default
	    private LocalDateTime timestamp = LocalDateTime.now();
}
