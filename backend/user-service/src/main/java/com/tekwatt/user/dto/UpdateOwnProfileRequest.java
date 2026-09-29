package com.tekwatt.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 32) String phone,
        @Size(max = 100) String city,
        @Size(max = 20) String zipcode) { }
