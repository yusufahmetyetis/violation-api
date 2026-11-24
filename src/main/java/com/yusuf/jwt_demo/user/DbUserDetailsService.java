package com.yusuf.jwt_demo.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final AppUserRepository repo;

    public DbUserDetailsService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı yok: " + username));

        String[] roles = repo.findRoleNames(u.getId()).toArray(String[]::new);

        return User.withUsername(u.getUsername())
                .password(u.getPassword())     // hash olduğu gibi; kontrolü Spring yapar
                .disabled(!u.isEnabled())      // pasif → Spring girişi reddeder
                .roles(roles)                // "ADMIN" → "ROLE_ADMIN" (prefix'i roles() ekler)
                .build();
    }
}