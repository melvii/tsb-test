package com.tsb.banking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="customers")
public class Customer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private String fullName;

  @Column(nullable=false, unique=true)
  private String email;

  @Column(nullable=false, unique=true)
  private String phone;

  @Column(nullable=false)
  private String passwordHash;

  @Column(nullable=false)
  private String role; // CUSTOMER or ADMIN

  @Column(nullable=false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
