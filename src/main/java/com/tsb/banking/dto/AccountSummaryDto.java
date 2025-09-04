package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data @AllArgsConstructor
public class AccountSummaryDto {
  private Long id;
  private String accountNumber;  // e.g. *************3210
  private String type;                 // CHECKING / SAVINGS
  private BigDecimal balance;
  private Instant createdAt;
}
