package com.tsb.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data public class ResetPassword { @NotBlank private String resetToken; @NotBlank private String newPassword;} 