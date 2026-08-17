package com.tekwatt.user.dto;
import jakarta.validation.constraints.*;
public record UpdateUserRequest(@NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) String lastName, @Size(max=200) String fullName, @NotBlank @Email @Size(max=254) String email, @Size(max=32) String phone, @Size(max=100) String city, @Size(max=20) String zipcode, String status) { }
