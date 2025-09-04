package com.tsb.banking.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.tsb.banking.crypto.HmacService;
import com.tsb.banking.domain.repo.AccountRepository;
import com.tsb.banking.domain.repo.CustomerRepository;
import com.tsb.banking.dto.TransferRequest;

import lombok.RequiredArgsConstructor;

@Component("authz")
@RequiredArgsConstructor
public class Authz {
  private final HmacService hmac;
  private final AccountRepository accounts;
  private final CustomerRepository customers;

  public boolean isAdmin(Authentication auth) {
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  public boolean ownsCustomerId(Long customerId, Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) return false;
    return isAdmin(auth) || customers.existsByIdAndEmail(customerId, auth.getName());
  }

  public boolean ownsAccount(String accountNumber, Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) return false;
    if (isAdmin(auth)) return true;
    String hash = hmac.hmacHex(accountNumber == null ? "" : accountNumber.trim());
    return accounts.existsByCustomer_EmailAndAccountNumberHash(auth.getName(), hash);
  }

  public boolean canTransfer(TransferRequest req, Authentication auth) {
    return ownsAccount(req.getFromAccount(), auth);
  }

  public boolean canCreateAccount(Authentication auth) {
    return auth != null && auth.isAuthenticated();
  }
}
