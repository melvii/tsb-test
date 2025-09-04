// com.tsb.banking.service.PasswordResetService
package com.tsb.banking.service;

import com.tsb.banking.audit.AuditEvent;
import com.tsb.banking.config.security.jwt.JwtService;
import com.tsb.banking.domain.entity.Customer;
import com.tsb.banking.domain.repo.AuditEventRepository;
import com.tsb.banking.domain.repo.CustomerRepository;
import com.tsb.banking.domain.repo.RefreshTokenRepository;
import com.tsb.banking.exception.BadRequestException;
import com.tsb.banking.exception.NotFoundException;
import com.tsb.banking.otp.SmsOtpGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final CustomerRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final SmsOtpGateway otp;
    private final JwtService jwt;
    private final AuditEventRepository audit;

    @Transactional
    public void requestReset(String emailOrPhone) {
        // Anti-enumeration: always return 200
        customers.findByEmailOrPhone(emailOrPhone, emailOrPhone).ifPresent(c -> {
            // Send OTP only if we actually have a user
            if (c.getPhone() == null || !c.getPhone().startsWith("+")) return;
            otp.sendOtp(c.getPhone().trim());
            audit.save(AuditEvent.builder()
                    .actor(c.getEmail())
                    .action("PWD_RESET_OTP_REQUEST")
                    .target(maskPhone(c.getPhone()))
                    .build());
        });
    }

    @Transactional
    public String verifyOtpAndIssueResetToken(String email, String code) {
        Customer c = customers.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Account not found")); // still returns 404 only here
        boolean ok = otp.verifyOtp(c.getPhone(), code);
        audit.save(AuditEvent.builder()
                .actor(email)
                .action(ok ? "PWD_RESET_OTP_OK" : "PWD_RESET_OTP_FAIL")
                .target(maskPhone(c.getPhone()))
                .build());
        if (!ok) throw new BadRequestException("Invalid OTP");
        return jwt.generateResetToken(email);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = jwt.validateAndExtractResetEmail(resetToken);
        Customer c = customers.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        validatePassword(newPassword);
        c.setPasswordHash(encoder.encode(newPassword));
        customers.save(c);

        // Invalidate all refresh tokens (logs out every device)
        refreshTokens.deleteByCustomer(c);

        audit.save(AuditEvent.builder()
                .actor(email)
                .action("PWD_RESET_COMPLETE")
                .target(null)
                .build());
    }

    private void validatePassword(String p) {
        if (p == null || p.length() < 8) throw new BadRequestException("Password too short");
        if (!p.matches(".*[A-Z].*")) throw new BadRequestException("Must contain an uppercase letter");
        if (!p.matches(".*[a-z].*")) throw new BadRequestException("Must contain a lowercase letter");
        if (!p.matches(".*\\d.*"))   throw new BadRequestException("Must contain a digit");
        if (!p.matches(".*[^A-Za-z0-9].*")) throw new BadRequestException("Must contain a special char");
    }

    private String maskPhone(String ph) {
        if (ph == null) return null;
        return ph.replaceAll(".(?=.{4})", "*");
    }
}
