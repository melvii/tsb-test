package com.tsb.banking.config.security;

import com.tsb.banking.domain.entity.Customer;
import com.tsb.banking.domain.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer c = repo.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(c.getEmail(), c.getPasswordHash(),  AuthorityUtils.createAuthorityList("ROLE_" + c.getRole()));
    }
}
