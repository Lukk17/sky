package com.lukk.service;

import com.lukk.dto.UserDTO;
import com.lukk.entity.Role;
import com.lukk.entity.User;
import com.lukk.repository.RoleRepository;
import com.lukk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
//    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public User findByUserEmail(String email) {
        log.info("finding user by email");
        return userRepository.findByEmail(email);
    }

    @Override
    public User findById(Long id) {
        log.info("Finding user by id");
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<User> findAll() {
        log.info("Finding all users");
        return userRepository.findAll();
    }

    @Override
    public List<UserDTO> findAllAndConvertToDTO() {
        List<User> users = findAll();

        return users.stream()
                .map(this::convertUserEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public User saveUser(UserDTO userDTO) {
        User user = convertUserDTO_toEntity(userDTO);

        log.info("Saving user: " + user.getEmail() + " " + user.getId() + " " + user.getRoles());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        try {
            log.info("Removing user: " + Objects.requireNonNull(user).getEmail() + " " + user.getId() + " " + user.getRoles());
            userRepository.delete(user);

        } catch (NullPointerException e) {
            log.error("User not found - cannot be removed");
        }
    }

//    @Override
//    public boolean checkPassword(String newPassword, String password) {
//        log.info("checking password");
//        return passwordEncoder.matches(newPassword, password);
//    }

    @Override
    public UserDTO findUserDetails(String email) {
        return convertUserEntity_toDTO(findByUserEmail(email));
    }


    private UserDTO convertUserEntity_toDTO(User user) {

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();

    }

    private User convertUserDTO_toEntity(UserDTO userDTO) {
        Role userRole = roleRepository.findByName("USER");

        return User.builder()
                .email(userDTO.getEmail())
//                .password(passwordEncoder.encode(userDTO.getPassword()))
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

    }

}
