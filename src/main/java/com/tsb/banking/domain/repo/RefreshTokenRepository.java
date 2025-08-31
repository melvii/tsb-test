package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.RefreshToken;
import com.tsb.banking.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);
  void deleteByCustomer(Customer customer);
}
