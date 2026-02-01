package com.example.backend.controller.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @Email @NotBlank String email,

        // ✅ 인스타 아이디(핸들): 3~20자, 영문소문자/숫자/._ 만 허용
        @NotBlank
        @Size(min = 3, max = 20)
        @Pattern(
                regexp = "^[a-z0-9._]{3,20}$",
                message = "아이디(handle)는 영문 소문자/숫자/._ 만 가능하며 3~20자여야 합니다."
        )
        String handle,

        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(min = 2, max = 50) String name
) {}
