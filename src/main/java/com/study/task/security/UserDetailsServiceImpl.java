package com.study.task.security;

import com.study.task.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;

    public UserDetailsServiceImpl(UserRepository users) {
        this.users = users;
    }

    public UserDetails loadUserByUsername(String email) {
        return users
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
