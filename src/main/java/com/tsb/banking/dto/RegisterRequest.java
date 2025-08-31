package com.tsb.banking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String fullName;
    @Email @NotBlank private String email;
    @NotBlank private String phone;
    @Size(min=8) private String password;
}
