package com.tsb.banking.controller;

import com.tsb.banking.domain.entity.Account;
import com.tsb.banking.dto.AccountTxnListResponse;
import com.tsb.banking.dto.CustomerAccountsResponse;
import com.tsb.banking.dto.TransferRequest;
import com.tsb.banking.service.AccountService;
import com.tsb.banking.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final TransactionService transactionService;

    private final AccountService accountService;

    @PreAuthorize("@authz.ownsCustomerId(#customerId, authentication)")
    @GetMapping("/customers/{customerId}/accounts")
    public CustomerAccountsResponse accounts(@PathVariable Long customerId) {
        return accountService.listAccountsFormatted(customerId);
    }

    @PreAuthorize("@authz.ownsAccount(#accountNumber, authentication)")
    @GetMapping("/accounts/{accountNumber}/transactions")
    public AccountTxnListResponse txns(@PathVariable String accountNumber,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size) {
        return transactionService.listTxns(accountNumber,page,size);
    }

    @PostMapping("/accounts/transfer")
    public void transfer(@RequestBody @Valid TransferRequest req, Authentication auth) {
        accountService.transfer(req, auth.getName());
    }

    @PostMapping("/account")
    public void createAccount(@RequestBody Account req ,Authentication auth) {
        accountService.createForCurrentUser(req, auth.getName());        
        
    }
    
}
