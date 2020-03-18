package com.lukk.service;

import com.lukk.H2TestProfileJPAConfig;
import com.lukk.SkyApplication;
import com.lukk.entity.Role;
import com.lukk.entity.User;
import com.lukk.repository.RoleRepository;
import com.lukk.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static com.lukk.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.Assemblers.UserAssembler.createTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;


    @Test
    void whenFindByUserEmail_thenReturnUser() {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userRepository.findByEmail(TEST_USER_EMAIL)).thenReturn(expected);

        //When
        User found = userService.findByUserEmail(TEST_USER_EMAIL);

        //Then
        assertEquals(expected.getEmail(), found.getEmail());

    }

    @Test
    void whenFindById_thenReturnUser() {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(expected));

        //When
        User found = userService.findById(1L);

        //Then
        assertEquals(expected.getEmail(), found.getEmail());
    }

    @Test
    void whenFindAll_thenReturnAllUsers() {
        //Given
        List<User> expectedList = Arrays.asList(
                createTestUser(TEST_USER_EMAIL),
                createTestUser("User2@mail")
        );
        Mockito.when(userRepository.findAll()).thenReturn(expectedList);

        //When
        List<User> found = userService.findAll();

        //Then
        assertEquals(expectedList.get(0).getEmail(), found.get(0).getEmail());
        assertEquals(expectedList.get(1).getEmail(), found.get(1).getEmail());

    }

    @Test
    void whenSaveUser_thenReturnUserWithEncodedPassAndRoles() {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        // another instance required to not make changes in expected user when saving
        User processed = createTestUser(TEST_USER_EMAIL);
        Role expectedRole = new Role(1L, "USER", new ArrayList<User>());

        Mockito.when(userRepository.save(processed)).thenReturn(processed);
        Mockito.when(roleRepository.findByName("USER")).thenReturn(expectedRole);

        //When
        User found = userService.saveUser(processed);

        //Then
        assertEquals(expected.getEmail(), found.getEmail());
        assertEquals(new HashSet<>(Collections.singletonList(expectedRole)), found.getRoles());
        Assertions.assertTrue(bCryptPasswordEncoder.matches(expected.getPassword(), found.getPassword()));


    }

    @Test
    void deleteUserTest() {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        expected.setId(99L);

        ArgumentCaptor<User> valueCapture = ArgumentCaptor.forClass(User.class);
        doNothing().when(userRepository).delete(valueCapture.capture());

        Mockito.when(userRepository.findById(expected.getId())).thenReturn(java.util.Optional.of(expected));

        //When
        userService.deleteUser(expected.getId());

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    void checkPasswordTest() {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);

        //When
        String encoded = bCryptPasswordEncoder.encode(expected.getPassword());

        //Then
        bCryptPasswordEncoder.matches(expected.getPassword(), encoded);
    }
}
