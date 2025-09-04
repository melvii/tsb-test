// src/main/java/com/tsb/banking/dto/AccountTxnRow.java
package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @AllArgsConstructor
public class AccountTxnRow {
  private Long transferId;
  private Instant createdAt;
  private String side;                // "DEBIT" | "CREDIT"
  private BigDecimal amount;
  private String description;
  private String counterpartyName;
  private String counterpartyNumber;  // plaintext from JPA converter; will be masked in service
}
