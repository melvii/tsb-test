package com.tsb.banking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="txns")
public class Txn {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  private Account account;

  @Column(nullable=false)
  private String kind; // CREDIT, DEBIT, TRANSFER

  @Column(nullable=false, precision=18, scale=2)
  private BigDecimal amount;

  private String description;

  @Column(nullable=false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  // com.tsb.banking.domain.entity.Txn
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transfer_id")
  private Transfer transfer;

}
