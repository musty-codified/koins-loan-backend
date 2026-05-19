package com.koins.loanbackend.security;

import com.koins.loanbackend.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.koins.loanbackend.domain.User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));

        return User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_USER")
            .build();
    }
}