package com.waypoint.household.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePersonRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotBlank(message = "role must not be blank")
        String role
) {
}
