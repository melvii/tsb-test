package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.Txn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TxnRepository extends JpaRepository<Txn, Long> {
  List<Txn> findByAccountIdOrderByCreatedAtDesc(Long accountId);

  List<Txn> findByAccount_AccountNumberHashOrderByCreatedAtDesc(String accountNumberHash);

}
