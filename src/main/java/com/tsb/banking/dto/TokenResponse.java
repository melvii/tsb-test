package com.tsb.banking.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;
}
