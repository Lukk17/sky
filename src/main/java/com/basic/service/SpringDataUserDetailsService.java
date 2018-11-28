package com.basic.service;

import com.basic.entity.Role;
import com.basic.entity.User;
import com.basic.interfaceService.UserServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;

public class SpringDataUserDetailsService implements UserDetailsService{

    private UserServiceInterface userServiceInterface;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userServiceInterface.findByUserEmail(email);
        if(user == null) {
            throw new UsernameNotFoundException(email);
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Role role: user.getRoles()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));
        }

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthorities);
    }

    @Autowired
    public void setUserServiceInterface(UserServiceInterface userServiceInterface)
    {
        this.userServiceInterface = userServiceInterface;
    }
}
