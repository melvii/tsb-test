// src/main/java/com/tsb/banking/dto/TxnLineRow.java
package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class TxnLineRow {
  private Long id;
  private String kind;              // "DEBIT" or "CREDIT"
  private BigDecimal amount;
  private String accountNumber;
  private String accountName;
}
