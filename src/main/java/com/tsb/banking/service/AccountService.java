package com.tsb.banking.service;

import com.tsb.banking.audit.AuditEvent;
import com.tsb.banking.crypto.HmacService;
import com.tsb.banking.domain.entity.Account;
import com.tsb.banking.domain.entity.Txn;
import com.tsb.banking.domain.repo.AccountRepository;
import com.tsb.banking.domain.repo.AuditEventRepository;
import com.tsb.banking.domain.repo.TxnRepository;
import com.tsb.banking.dto.TransferRequest;
import com.tsb.banking.exception.BadRequestException;
import com.tsb.banking.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final TxnRepository txns;
    private final AuditEventRepository auditRepo;
    private final HmacService hmac;

    public List<Account> listAccounts(Long customerId) {
        return accounts.findByCustomerId(customerId);
    }

    public List<Txn> listTxns(String accountNumber) {
        String hash = hmac.hmacHex(accountNumber);
        System.out.println(accountNumber);
        return txns.findByAccount_AccountNumberHashOrderByCreatedAtDesc(hash);
    }
/**
 * Tranfer 
 * @param req
 * @param actorEmail
 */
    @Transactional
    public void transfer(TransferRequest req, String actorEmail) {
        String fromHash = hmac.hmacHex(req.getFromAccount());
        String toHash   = hmac.hmacHex(req.getToAccount());

        // lock rows to avoid race conditions on concurrent transfers
        Account from = accounts.findForUpdateByHash(fromHash)
            .orElseThrow(() -> new NotFoundException("From account not found"));
        Account to = accounts.findForUpdateByHash(toHash)
            .orElseThrow(() -> new NotFoundException("To account not found"));
        if (from.getId().equals(to.getId())) throw new BadRequestException("Cannot transfer to same account");
        if (!from.getCustomer().equals(to.getCustomer())) throw new BadRequestException("Cannot transfer to a different  account");

        if (from.getBalance().compareTo(req.getAmount()) < 0) throw new BadRequestException("Insufficient funds");

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));
        accounts.save(from);
        accounts.save(to);

        txns.save(Txn.builder().account(from).kind("DEBIT").amount(req.getAmount())
                .description(req.getDescription()).build());
        txns.save(Txn.builder().account(to).kind("CREDIT").amount(req.getAmount())
                .description(req.getDescription()).build());

        auditRepo.save(AuditEvent.builder()
                .actor(actorEmail).action("TRANSFER")
                .target(from.getAccountNumber() + "->" + to.getAccountNumber())
                .details("{\"amount\":\"" + req.getAmount() + "\"}")
                .build());
    }
}
