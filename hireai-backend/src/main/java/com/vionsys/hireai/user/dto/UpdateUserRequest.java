package com.vionsys.hireai.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    private String firstName;

    private String lastName;

    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^\\+?[0-9\\s\\-\\(\\)]{7,25}$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

}