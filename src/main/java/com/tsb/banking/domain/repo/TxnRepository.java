// src/main/java/com/tsb/banking/domain/repo/TxnRepository.java
package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.Txn;
import com.tsb.banking.dto.AccountTxnRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TxnRepository extends JpaRepository<Txn, Long> {

  /**
   * Returns one row per ledger line for the given account (by blind index),
   * including computed counterparty name/number via CASE on the transfer sides.
   */
  @Query("""
    select new com.tsb.banking.dto.AccountTxnRow(
      t.transfer.id,
      t.createdAt,
      t.kind,
      t.amount,
      t.description,
      case when t.kind = 'DEBIT'  then tc.fullName   else fc.fullName end,
      case when t.kind = 'DEBIT'  then ta.accountNumber else fa.accountNumber end
    )
    from Txn t
      join t.transfer tr
      join tr.fromAccount fa
      join fa.customer fc
      join tr.toAccount ta
      join ta.customer tc
    where t.account.accountNumberHash = :hash
    order by t.createdAt desc
  """)
  List<AccountTxnRow> findRowsForAccountHash(@Param("hash") String hash, Pageable pageable);
}
