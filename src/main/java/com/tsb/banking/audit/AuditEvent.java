package com.tsb.banking.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="audit_events")
public class AuditEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  @Builder.Default
  private Instant eventTime =Instant.now();

  @Column(nullable=false)
  private String actor;

  @Column(nullable=false)
  private String action;

  private String target;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String details; // JSON string
}
