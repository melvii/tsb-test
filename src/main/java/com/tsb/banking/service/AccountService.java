package com.tsb.banking.service;

import com.tsb.banking.audit.AuditEvent;
import com.tsb.banking.crypto.HmacService;
import com.tsb.banking.domain.entity.Account;
import com.tsb.banking.domain.entity.Customer;
import com.tsb.banking.domain.entity.Transfer;
import com.tsb.banking.domain.entity.Txn;
import com.tsb.banking.domain.repo.AccountRepository;
import com.tsb.banking.domain.repo.AuditEventRepository;
import com.tsb.banking.domain.repo.CustomerRepository;
import com.tsb.banking.domain.repo.TransferRepository;
import com.tsb.banking.domain.repo.TxnRepository;
import com.tsb.banking.dto.AccountSummaryDto;
import com.tsb.banking.dto.CustomerAccountsResponse;
import com.tsb.banking.dto.TransferRequest;
import com.tsb.banking.exception.BadRequestException;
import com.tsb.banking.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final TxnRepository txns;
    private final AuditEventRepository auditRepo;
    private final HmacService hmac;
    private final CustomerRepository customers;
    private final TransferRepository transfers;

 @Transactional(readOnly = true)
  public CustomerAccountsResponse listAccountsFormatted(Long customerId) {
    var customer = customers.findById(customerId)
        .orElseThrow(() -> new NotFoundException("Customer not found"));

    var items = accounts.findByCustomerId(customerId).stream()
        .sorted(Comparator.comparing(a -> a.getCreatedAt())) // or .reversed() if you prefer latest first
        .map(a -> new AccountSummaryDto(
            a.getId(),
            a.getAccountNumber(),
            a.getType(),
            a.getBalance(),
            a.getCreatedAt()
        ))
        .toList();

    return new CustomerAccountsResponse(
        customer.getId(),
        customer.getFullName(),
        customer.getEmail(),
        items
    );
  }

/**
 * Tranfer moany between 2 account 
 * @param req
 * @param actorEmail
 */
    
  @Transactional
  public void transfer(TransferRequest req, String actorEmail) {
    // Lookup by blind index (HMAC), not by decrypting
    String fromHash = hmac.hmacHex(req.getFromAccount());
    String toHash   = hmac.hmacHex(req.getToAccount());

    Account from = accounts.findByAccountNumberHash(fromHash)
        .orElseThrow(() -> new NotFoundException("From account not found"));
    Account to = accounts.findByAccountNumberHash(toHash)
        .orElseThrow(() -> new NotFoundException("To account not found"));

    if (from.getId().equals(to.getId())) throw new BadRequestException("Cannot transfer to same account");

    BigDecimal amount = req.getAmount();
    if (amount == null || amount.signum() <= 0) throw new BadRequestException("Invalid amount");
    if (from.getBalance().compareTo(amount) < 0) throw new BadRequestException("Insufficient funds");

    // 1) Update balances
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));
    accounts.save(from);
    accounts.save(to);

    // 2) Parent transfer
    Transfer tr = Transfer.builder()
        .fromAccount(from)
        .toAccount(to)
        .amount(amount)
        .description(req.getDescription())
        .actor(actorEmail)
        .build();
    transfers.save(tr);

    // 3) Two ledger lines (DEBIT from, CREDIT to)
    txns.save(Txn.builder()
        .account(from).transfer(tr).kind("DEBIT").amount(amount)
        .description(req.getDescription()).build());

    txns.save(Txn.builder()
        .account(to).transfer(tr).kind("CREDIT").amount(amount)
        .description(req.getDescription()).build());

    // 4) Audit
    auditRepo.save(AuditEvent.builder()
        .actor(actorEmail)
        .action("TRANSFER")
        .target(from.getAccountNumber() + "->" + to.getAccountNumber())
        .details("{\"amount\":\"" + amount + "\"}")
        .build());
  }



  @Transactional
  public void createForCurrentUser(Account req, String actorEmail) {
    Customer owner = customers.findByEmail(actorEmail)
        .orElseThrow(() -> new NotFoundException("Owner not found"));

    String number = (req.getAccountNumber() == null || req.getAccountNumber().isBlank())
        ? generateAccountNumber(owner.getId())
        : req.getAccountNumber().trim();

    // Prevent duplicates
    String hash = hmac.hmacHex(number);
    if (accounts.findByAccountNumberHash(hash).isPresent()) {
      throw new BadRequestException("Account number already exists");
    }

    BigDecimal opening = req.getBalance() == null ? BigDecimal.ZERO : req.getBalance();

    Account a = Account.builder()
        .customer(owner)
        .accountNumber(number)           // JPA converter will encrypt
        .accountNumberHash(hash)         // deterministic blind index
        .type(req.getType())
        .balance(opening)
        .createdAt(Instant.now())
        .build();

    accounts.save(a);

  }

  private String generateAccountNumber(Long ownerId) {
    // Example: NZ12-<ownerId mod 10000>-<random 7 digits>-00
    String rnd = String.valueOf((long)(Math.random() * 9_000_000L) + 1_000_000L);
    String bank = String.format("%04d", (ownerId % 10_000));
    return "NZ12-" + bank + "-" + rnd + "-00";
  }
}
