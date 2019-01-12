package com.basic.service;

import com.basic.entity.Role;
import com.basic.entity.User;
import com.basic.repository.RoleRepository;
import com.basic.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;

@Service
public class UserService implements IUserService
{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByUserEmail(String email)
    {

        return userRepository.findByEmail(email);
    }

    @Override
    public User findById(Long id)
    {

        return userRepository.findById(id).get();
    }

    @Override
    public void saveUser(User user)
    {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName("ROLE_USER");
        user.setRoles(new HashSet<>(Arrays.asList(userRole)));
        userRepository.save(user);
    }

    @Override
    public void delete(Long id)
    {
        userRepository.delete(userRepository.findById(id).get());
    }

    @Override
    public boolean checkPassword(String newPassword, String password)
    {
        return passwordEncoder.matches(newPassword, password);
    }


}
