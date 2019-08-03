package com.lukk.service;

import com.lukk.entity.Role;
import com.lukk.entity.User;
import com.lukk.repository.RoleRepository;
import com.lukk.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Service
@Log4j2
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
        log.debug("finding user by email");
        return userRepository.findByEmail(email);
    }

    @Override
    public User findById(Long id)
    {
        log.debug("finding user by id");
        return userRepository.findById(id).get();
    }

    @Override
    public List<User> findAll()
    {
        log.debug("finding all users");
        return userRepository.findAll();
    }

    @Override
    public void saveUser(User user)
    {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName("USER");
        user.setRoles(new HashSet<>(Arrays.asList(userRole)));
        log.debug("saving user: " + user.getEmail() + " " + user.getId() + " " + user.getRoles());
        userRepository.save(user);

    }

    @Override
    public void delete(Long id)
    {
        User user = userRepository.findById(id).get();
        log.debug("saving user: " + user.getEmail() + " " + user.getId() + " " + user.getRoles());
        userRepository.delete(user);
    }

    @Override
    public boolean checkPassword(String newPassword, String password)
    {
        log.debug("checking password");
        return passwordEncoder.matches(newPassword, password);
    }


}
