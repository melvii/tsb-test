package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.Transfer;


import org.springframework.data.jpa.repository.JpaRepository;


public interface TransferRepository extends JpaRepository<Transfer, Long> {


}
