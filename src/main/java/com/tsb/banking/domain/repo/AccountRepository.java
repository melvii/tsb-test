package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.Account;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
  List<Account> findByCustomerId(Long customerId);
  Optional<Account> findByAccountNumber(String accountNumber);

  Optional<Account> findByAccountNumberHash(String accountNumberHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumberHash = :hash")
    Optional<Account> findForUpdateByHash(@Param("hash") String hash);


    @Query("""
    select c.fullName from Account a join a.customer c
    where a.accountNumberHash = :hash
  """)
  Optional<String> findHolderNameByHash(@Param("hash") String hash);

  @Query("""
    select a.accountNumber from Account a
    where a.accountNumberHash = :hash
  """)
  Optional<String> findPlainNumberByHash(@Param("hash") String hash);



// AccountRepository.java
boolean existsByCustomer_EmailAndAccountNumberHash(String email, String accountNumberHash);


}
