package com.tsb.banking.controller;

import com.tsb.banking.domain.entity.Account;
import com.tsb.banking.domain.entity.Txn;
import com.tsb.banking.dto.TransferRequest;
import com.tsb.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/customers/{customerId}/accounts")
    public List<Account> accounts(@PathVariable Long customerId) {
        return accountService.listAccounts(customerId);
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public List<Txn> txns(@PathVariable String accountNumber) {
        System.out.println(accountNumber);
        return accountService.listTxns(accountNumber);
    }

    @PostMapping("/accounts/transfer")
    public void transfer(@RequestBody @Valid TransferRequest req, Authentication auth) {
        accountService.transfer(req, auth.getName());
    }
}
