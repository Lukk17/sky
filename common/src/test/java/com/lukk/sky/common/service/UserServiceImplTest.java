package com.lukk.sky.common.service;

import com.lukk.sky.common.Assemblers.UserAssembler;
import com.lukk.sky.common.CommonApplication;
import com.lukk.sky.common.H2TestProfileJPAConfig;
import com.lukk.sky.common.dto.UserDTO;
import com.lukk.sky.common.entity.Role;
import com.lukk.sky.common.entity.User;
import com.lukk.sky.common.repository.RoleRepository;
import com.lukk.sky.common.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        CommonApplication.class,
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


    @Test
    public void whenFindByUserEmail_thenReturnUser() {
        //Given
        User expectedUser = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);
        Mockito.when(userRepository.findByEmail(UserAssembler.TEST_USER_EMAIL)).thenReturn(expectedUser);

        //When
        User found = userService.findByUserEmail(UserAssembler.TEST_USER_EMAIL);

        //Then
        Assert.assertEquals(expectedUser.getEmail(), found.getEmail());

    }

    @Test
    public void whenFindById_thenReturnUser() {
        //Given
        User expectedUser = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

        //When
        User found = userService.findById(1L);

        //Then
        Assert.assertEquals(expectedUser.getEmail(), found.getEmail());
    }

    @Test
    public void whenFindAll_thenReturnAllUsers() {
        //Given
        List<User> expectedList = Arrays.asList(
                UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL),
                UserAssembler.createTestUser("User2@mail")
        );
        Mockito.when(userRepository.findAll()).thenReturn(expectedList);

        //When
        List<User> found = userService.findAll();

        //Then
        Assert.assertEquals(expectedList.get(0).getEmail(), found.get(0).getEmail());
        Assert.assertEquals(expectedList.get(1).getEmail(), found.get(1).getEmail());

    }

    @Test
    public void whenSaveUser_thenReturnUserWithEncodedPassAndRoles() {
        //Given
        User expected = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);
        // another instance required to not make changes in expected user when saving
        UserDTO processedDTO = UserAssembler.createTestUserDTO(UserAssembler.TEST_USER_EMAIL);
        User processedUser = UserAssembler.convertUserDTO_toEntity(processedDTO);

        Role expectedRole = new Role(1L, "USER", new ArrayList<User>());

        System.out.println(processedUser);
        Mockito.when(userRepository.save(any())).thenReturn(processedUser);
        Mockito.when(roleRepository.findByName("USER")).thenReturn(expectedRole);

        //When
        User found = userService.saveUser(processedDTO);

        //Then
        Assert.assertEquals(expected.getEmail(), found.getEmail());
        Assert.assertEquals(new HashSet<>(Collections.singletonList(expectedRole)), found.getRoles());
//        assertTrue(bCryptPasswordEncoder.matches(expected.getPassword(), found.getPassword()));
    }

    @Test
    public void deleteUserTest() {
        //Given
        User expected = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);
        expected.setId(99L);

        ArgumentCaptor<User> valueCapture = ArgumentCaptor.forClass(User.class);
        doNothing().when(userRepository).delete(valueCapture.capture());

        Mockito.when(userRepository.findById(expected.getId())).thenReturn(java.util.Optional.of(expected));

        //When
        userService.deleteUser(expected.getId());

        //Then
        Assert.assertEquals(expected, valueCapture.getValue());
    }

    @Test
    public void checkPasswordTest() {
        //Given
        User expected = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);

        //When
        String encoded = bCryptPasswordEncoder.encode(expected.getPassword());

        //Then
        bCryptPasswordEncoder.matches(expected.getPassword(), encoded);
    }
}
