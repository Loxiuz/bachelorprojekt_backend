package com.bachelorprojekt_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    @NotBlank(message = "Username is required")
    @Size(max = 64, message = "Username must be at most 64 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
}
