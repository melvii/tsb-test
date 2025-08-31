package com.tsb.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data public class VerifyReset  { @NotBlank private String email; @NotBlank private String code; }
