package com.lukk.sky.authservice.dto;

import com.lukk.sky.authservice.Assemblers.RoleAssembler;
import com.lukk.sky.authservice.Assemblers.UserAssembler;
import com.lukk.sky.authservice.AuthServiceApplication;
import com.lukk.sky.authservice.H2TestProfileJPAConfig;
import com.lukk.sky.authservice.entity.Role;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.repository.RoleRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;

import static com.lukk.sky.authservice.Assemblers.RoleAssembler.USER_ROLE_NAME;
import static com.lukk.sky.authservice.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        AuthServiceApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class EntityDTOConverterTest {

    @Autowired
    EntityDTOConverter entityDTOConverter;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @MockBean
    RoleRepository roleRepository;

    @Test
    public void whenConvertUserEntity_toDTO_thenUserParametersStaysSameExceptDeletedPassword() {
        //Given
        User testUser = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        UserDTO expected = UserAssembler.createTestUserDTO_withoutPassword(TEST_USER_EMAIL);

        //When
        UserDTO actual = entityDTOConverter.convertUserEntity_toDTO(testUser);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenConvertUserDTO_toEntity_thenUserParametersStaysSame() {
        //Given
        Role userRole = RoleAssembler.getUserRole();

        User expected = UserAssembler.createSimpleTestUser(TEST_USER_EMAIL);
        expected.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        UserDTO testUserDTO = UserAssembler.createTestUserDTO_withPassword(TEST_USER_EMAIL);

        when(roleRepository.findByName(USER_ROLE_NAME)).thenReturn(userRole);

        //When
        User actual = entityDTOConverter.convertUserDTO_toEntity(testUserDTO);

        //Then
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getRoles(), actual.getRoles());
        assertEquals(expected.getId(), actual.getId());
        assertTrue(passwordEncoder.matches(expected.getPassword(), actual.getPassword()));
    }
}
