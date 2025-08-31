package com.tsb.banking.controller;

import com.tsb.banking.dto.*;
import com.tsb.banking.exception.BadRequestException;
import com.tsb.banking.service.AuthService;
import com.tsb.banking.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetServiceService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().body("User Registerd sunncefully");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }


    @PostMapping("/request-reset")
    public Map<String, Object> request(@RequestBody RequestReset req) {
        if (req.getEmailOrPhone() == null) throw new BadRequestException("emailOrPhone required");
        passwordResetServiceService.requestReset(req.getEmailOrPhone());
        return Map.of("status", "OK"); // always OK (anti-enumeration)
    }

    // Step 2: verify OTP -> get reset token
    @PostMapping("/verify-reset")
    public Map<String, Object> verify(@RequestBody VerifyReset req) {
        String resetToken = passwordResetServiceService.verifyOtpAndIssueResetToken(req.getEmail(), req.getCode());
        return Map.of("resetToken", resetToken, "expiresInSeconds", 600);
    }

    // Step 3: reset password
    @PostMapping("/reset-password")
    public Map<String, Object> reset(@RequestBody ResetPassword req) {
        passwordResetServiceService.resetPassword(req.getResetToken(), req.getNewPassword());
        return Map.of("status", "PASSWORD_UPDATED");
    }
}
