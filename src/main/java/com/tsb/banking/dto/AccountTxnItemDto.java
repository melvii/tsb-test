// src/main/java/com/tsb/banking/dto/AccountTxnItemDto.java
package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data @AllArgsConstructor
public class AccountTxnItemDto {
  private Long transferId;
  private Instant createdAt;
  private String side;                // "DEBIT" | "CREDIT" (relative to requested account)
  private BigDecimal amount;
  private String description;
  private String counterpartyName;
  private String counterpartyMasked;
}
