package com.tsb.banking.service;

import com.tsb.banking.crypto.HmacService;
import com.tsb.banking.domain.repo.AccountRepository;
import com.tsb.banking.domain.repo.TxnRepository;
import com.tsb.banking.dto.*;
import com.tsb.banking.exception.NotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TxnRepository txns;
    private final HmacService hmac;
    private final AccountRepository accounts;

    @Transactional(readOnly = true)
    public AccountTxnListResponse listTxns(String plaintextAccountNumber, int page, int size) {
      String hash = hmac.hmacHex(normalize(plaintextAccountNumber));
  
      // Top summary (once)
      String ownerName = accounts.findHolderNameByHash(hash)
          .orElseThrow(() -> new NotFoundException("Account not found"));
      String plainNumber = accounts.findPlainNumberByHash(hash).orElse(null);
  
      var rows = txns.findRowsForAccountHash(hash, PageRequest.of(page, size));
  
      List<AccountTxnItemDto> items = rows.stream()
          .map(r -> new AccountTxnItemDto(
              r.getTransferId(),
              r.getCreatedAt(),
              r.getSide(),
              r.getAmount(),
              r.getDescription(),
              r.getCounterpartyName(),
              r.getCounterpartyNumber()
          ))
          .toList();
  
      return new AccountTxnListResponse(plainNumber, ownerName, items);
    }
  
 // private String mask(String acc) {
 //   return acc == null ? null : acc.replaceAll(".(?=.{4})", "*");
 // }


  private String normalize(String acc) {
    return acc == null ? "" : acc.trim();
  }
}
