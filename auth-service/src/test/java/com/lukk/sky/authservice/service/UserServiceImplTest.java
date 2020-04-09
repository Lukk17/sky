package com.lukk.sky.authservice.service;

import com.lukk.sky.authservice.Assemblers.RoleAssembler;
import com.lukk.sky.authservice.Assemblers.UserAssembler;
import com.lukk.sky.authservice.AuthServiceApplication;
import com.lukk.sky.authservice.H2TestProfileJPAConfig;
import com.lukk.sky.authservice.dto.EntityDTOConverter;
import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.entity.Role;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.repository.RoleRepository;
import com.lukk.sky.authservice.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.authservice.Assemblers.RoleAssembler.USER_ROLE_NAME;
import static com.lukk.sky.authservice.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        AuthServiceApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private EntityDTOConverter entityDTOConverter;


    @Test
    public void whenFindByUserEmail_thenReturnUser() throws UsernameNotFoundException {
        //Given
        User expectedUser = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        when(userRepository.findByEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(expectedUser));

        //When
        User found = userService.findByUserEmail(TEST_USER_EMAIL);

        //Then
        Assert.assertEquals(expectedUser.getEmail(), found.getEmail());

    }

    @Test
    public void whenFindById_thenReturnUser() throws UsernameNotFoundException {
        //Given
        User expectedUser = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

        //When
        User found = userService.findById(1L);

        //Then
        Assert.assertEquals(expectedUser.getEmail(), found.getEmail());
    }

    @Test(expected = UsernameNotFoundException.class)
    public void whenFindById_thenThrowException() throws UsernameNotFoundException {
        //Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //When
        userService.findById(1L);

        //Then -exception should be thrown
    }

    @Test
    public void whenFindAll_thenReturnAllUsers() {
        //Given
        List<User> expectedList = UserAssembler.createUserList();

        when(userRepository.findAll()).thenReturn(expectedList);

        //When
        List<User> found = userService.findAll();

        //Then
        assertEquals(expectedList.get(0).getEmail(), found.get(0).getEmail());
        assertEquals(expectedList.get(1).getEmail(), found.get(1).getEmail());
    }

    @Test
    public void findAllAndConvertToDTO() {
        //Given
        List<User> users = UserAssembler.createUserList();
        List<UserDTO> expected = UserAssembler.createUserDTOList();

        when(userRepository.findAll()).thenReturn(users);
        for (int i = 0; i < users.size(); i++) {
            when(entityDTOConverter.convertUserEntity_toDTO(users.get(i))).thenReturn(expected.get(i));
        }

        //When
        List<UserDTO> actual = userService.findAllAndConvertToDTO();

        //Then
        assertEquals(expected, actual);

    }

    @Test
    public void whenSaveUser_thenReturnUserWithPassAndRoles() {
        //Given
        Role expectedRole = RoleAssembler.getUserRole();

        User expected = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        expected.setRoles(new HashSet<>(Collections.singletonList(expectedRole)));

        UserDTO processedDTO = UserAssembler.createTestUserDTO_withPassword(TEST_USER_EMAIL);

        when(userRepository.save(any())).thenReturn(expected);
        when(roleRepository.findByName(USER_ROLE_NAME)).thenReturn(expectedRole);
        when(entityDTOConverter.convertUserDTO_toEntity(processedDTO)).thenReturn(expected);

        //When
        User actual = userService.registerUser(processedDTO);

        //Then
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getPassword(), actual.getPassword());
        assertEquals(new HashSet<>(Collections.singletonList(expectedRole)), actual.getRoles());
    }

    @Test
    public void whenDeleteUser_thenDeleteUser() throws UsernameNotFoundException {
        //Given
        User expected = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        expected.setId(99L);

        ArgumentCaptor<User> valueCapture = ArgumentCaptor.forClass(User.class);

        doReturn(Optional.of(expected)).when(userRepository).findById(expected.getId());
        doNothing().when(userRepository).delete(valueCapture.capture());

        //When
        userService.deleteUser(expected.getId());

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test(expected = UsernameNotFoundException.class)
    public void deleteNotExistingUser_thenThrowException() throws UsernameNotFoundException {
        //Given
        User expected = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        expected.setId(99L);

        doReturn(Optional.of(expected)).when(userRepository).findById(expected.getId());
        doThrow(NullPointerException.class).when(userRepository).delete(expected);

        //When
        userService.deleteUser(expected.getId());

        //Then - exception should be thrown
    }

    @Test
    public void whenFindUserDetails_thenReturnUserDTO() throws UsernameNotFoundException {
        //Given
        User expected = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        UserDTO processedDTO = UserAssembler.createTestUserDTO_withoutPassword(TEST_USER_EMAIL);

        when(userRepository.findByEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(expected));
        when(entityDTOConverter.convertUserEntity_toDTO(expected)).thenReturn(processedDTO);

        //When
        UserDTO found = userService.findUserDetails(TEST_USER_EMAIL);

        //Then
        assertEquals(processedDTO, found);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void whenFindNotExistingUserDetails_thenThrowException() throws UsernameNotFoundException {
        //Given
        when(userRepository.findByEmail(TEST_USER_EMAIL)).thenReturn(Optional.empty());

        //When
        userService.findUserDetails(TEST_USER_EMAIL);

        //Then - exception should be thrown
    }


    @Test
    public void whenCheckGoodPassword_thenReturnTrue() {
        //Given
        String password = "test";
        String encoded = bCryptPasswordEncoder.encode(password);

        //When
        boolean actual = userService.checkPassword(password, encoded);

        //Then
        assertTrue(actual);
    }

    @Test
    public void whenCheckWrongPassword_thenReturnFalse() {
        //Given
        String encodedPassword = bCryptPasswordEncoder.encode("test");
        String wrongPassword = "test1";

        //When
        boolean actual = userService.checkPassword(wrongPassword, encodedPassword);

        //Then
        assertFalse(actual);
    }


}
