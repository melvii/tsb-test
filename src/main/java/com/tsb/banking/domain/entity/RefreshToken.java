package com.tsb.banking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="refresh_tokens")
public class RefreshToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  private Customer customer;

  @Column(nullable=false, length=600)
  private String token;

  @Column(nullable=false)
  private Instant expiry;
}
