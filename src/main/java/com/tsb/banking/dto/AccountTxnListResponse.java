// src/main/java/com/tsb/banking/dto/AccountTxnListResponse.java
package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data @AllArgsConstructor
public class AccountTxnListResponse {
  private String accountMasked;       // requested account (masked once)
  private String accountOwnerName;    // holder of the requested account
  private List<AccountTxnItemDto> items;
}
