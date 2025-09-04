package com.tsb.banking.service;

import com.tsb.banking.audit.AuditEvent;
import com.tsb.banking.config.security.jwt.JwtService;
import com.tsb.banking.crypto.HmacService;
import com.tsb.banking.domain.entity.Customer;
import com.tsb.banking.domain.entity.RefreshToken;
import com.tsb.banking.domain.repo.AccountRepository;
import com.tsb.banking.domain.repo.AuditEventRepository;
import com.tsb.banking.domain.repo.CustomerRepository;
import com.tsb.banking.domain.repo.RefreshTokenRepository;
import com.tsb.banking.dto.*;
import com.tsb.banking.exception.BadRequestException;
import com.tsb.banking.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final AuditEventRepository auditRepo;
    private final HmacService hmac;
    private final AccountRepository accounts;



    @Transactional
    public void register(RegisterRequest req) {
        if (customers.existsByEmail(req.getEmail())) throw new BadRequestException("Email already used");
        if (customers.existsByPhone(req.getPhone())) throw new BadRequestException("Phone already used");
        Customer c = Customer.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .passwordHash(encoder.encode(req.getPassword()))
                .role("CUSTOMER")
                .createdAt(Instant.now())
                .build();
        customers.save(c);
        auditRepo.save(AuditEvent.builder()
                .actor(req.getEmail()).action("REGISTER").target(null).details(null).build());
    }

    @Transactional
public TokenResponse login(LoginRequest req) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
    );

    org.springframework.security.core.userdetails.User user =
        (org.springframework.security.core.userdetails.User) auth.getPrincipal();

    String access  = jwt.generateAccessToken(user);
    String refresh = jwt.generateRefreshToken(user);

    String emailFromPrincipal = user.getUsername();
    Customer c = customers.findByEmail(emailFromPrincipal)
        .orElseThrow(() -> new NotFoundException("User not found"));

    refreshTokens.deleteByCustomer(c);
    refreshTokens.save(RefreshToken.builder()
            .customer(c)
            .token(refresh)
            .expiry(Instant.now().plusSeconds(7 * 24 * 3600))
            .build());

    auditRepo.save(AuditEvent.builder()
            .actor(emailFromPrincipal)
            .action("LOGIN_SUCCESS")
            .build());

    return new TokenResponse(access, refresh);
}


    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        RefreshToken token = refreshTokens.findByToken(req.getRefreshToken()).orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (token.getExpiry().isBefore(Instant.now())) throw new BadRequestException("Expired refresh token");
        Customer c = token.getCustomer();
        User user = (User) User.withUsername(c.getEmail()).password(c.getPasswordHash()).roles(c.getRole()).build();
        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user);
        token.setToken(refresh);
        token.setExpiry(Instant.now().plusSeconds(7*24*3600));
        refreshTokens.saveAndFlush(token);
        return new TokenResponse(access, refresh);
    }


  public boolean canAccessAccount(String accountNumber, Authentication auth) {
    var email = auth.getName();
    var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    var hash = hmac.hmacHex(accountNumber.trim());
    return accounts.findByAccountNumberHash(hash)
      .map(a -> isAdmin || a.getCustomer().getEmail().equals(email))
      .orElse(false);
  }

}
