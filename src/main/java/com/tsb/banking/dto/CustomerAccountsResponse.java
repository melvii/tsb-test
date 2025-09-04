package com.tsb.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data @AllArgsConstructor
public class CustomerAccountsResponse {
  private Long customerId;
  private String customerName;
  private String customerEmail;
  private List<AccountSummaryDto> accounts;
}
