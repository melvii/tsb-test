package com.tsb.banking.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Transfer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Builder.Default
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_account_id", nullable = false)
  private Account fromAccount;

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_account_id", nullable = false)
  private Account toAccount;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  private String description;
  private String actor;
}
