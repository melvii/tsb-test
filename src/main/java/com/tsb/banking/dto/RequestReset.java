package com.tsb.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data public class RequestReset { @NotBlank private String emailOrPhone; }