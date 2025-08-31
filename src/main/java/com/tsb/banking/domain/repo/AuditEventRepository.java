package com.tsb.banking.domain.repo;

import com.tsb.banking.audit.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {}
