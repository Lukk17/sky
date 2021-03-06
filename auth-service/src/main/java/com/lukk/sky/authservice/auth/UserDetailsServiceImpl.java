package com.lukk.sky.authservice.auth;

import com.lukk.sky.authservice.entity.Role;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) {
        try {
            User user = userService.findByUserEmail(email);
            Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

            for (Role role : user.getRoles()) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            }

            log.info("Logged user: " + user.getEmail());

            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthorities);
        } catch (UsernameNotFoundException e) {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Username: " + email + " not found");
        }
    }
}
