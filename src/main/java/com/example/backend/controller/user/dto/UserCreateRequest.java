package com.example.backend.controller.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest (
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(min = 2, max = 50) String name
) {}
