package com.tsb.banking.domain.repo;

import com.tsb.banking.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByEmail(String email);
  boolean existsByEmail(String email);
  boolean existsByPhone(String phone);
  Optional<Customer> findByEmailOrPhone(String emailOrPhone, String emailOrPhone2);
}
