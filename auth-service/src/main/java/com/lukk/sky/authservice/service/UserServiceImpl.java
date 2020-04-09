package com.lukk.sky.authservice.service;

import com.lukk.sky.authservice.dto.EntityDTOConverter;
import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EntityDTOConverter entityDTOConverter;

    @Override
    public User findByUserEmail(String email) throws UsernameNotFoundException {
        log.info("finding user by email");
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public User findById(Long id) throws UsernameNotFoundException {
        log.info("Finding user by id");
        return userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
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
                .map(entityDTOConverter::convertUserEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public User registerUser(UserDTO userDTO) throws IllegalArgumentException {
        User user = entityDTOConverter.convertUserDTO_toEntity(userDTO);

        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User already Exist!");
        }
        ;

        log.info("Saving user: " + user.getEmail() + " " + user.getId() + " " + user.getRoles());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id).orElse(null);
        try {
            log.info("Removing user: " + Objects.requireNonNull(user).getEmail() + " " + user.getId() + " " + user.getRoles());
            userRepository.delete(user);

        } catch (NullPointerException e) {
            throw new UsernameNotFoundException("User not found - cannot be removed");
        }
    }

    @Override
    public boolean checkPassword(String newPassword, String password) {
        log.info("checking password");
        return passwordEncoder.matches(newPassword, password);
    }

    @Override
    public UserDTO findUserDetails(String email) throws UsernameNotFoundException {
        return entityDTOConverter.convertUserEntity_toDTO(findByUserEmail(email));
    }
}
