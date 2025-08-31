package com.tsb.banking.domain.entity;

import com.tsb.banking.crypto.AccountNumberConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="accounts")
public class Account {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  private Customer customer;

  @Convert(converter = AccountNumberConverter.class)
  @Column(name="account_number_enc", nullable=false, unique=true, length=2048)
  private String accountNumber;

  @Column(name = "account_number_hash", length = 64, unique = true)
  private String accountNumberHash;

  @Column(nullable=false)
  private String type; // SAVINGS, CHECKING

  @Column(nullable=false, precision=18, scale=2)
  @Builder.Default
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable=false)
  @Builder.Default
  private Instant createdAt=Instant.now();
}
