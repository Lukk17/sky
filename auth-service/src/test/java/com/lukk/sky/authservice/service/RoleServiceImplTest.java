package com.lukk.sky.authservice.service;

import com.lukk.sky.authservice.Assemblers.RoleAssembler;
import com.lukk.sky.authservice.AuthServiceApplication;
import com.lukk.sky.authservice.H2TestProfileJPAConfig;
import com.lukk.sky.authservice.entity.Role;
import com.lukk.sky.authservice.repository.RoleRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        AuthServiceApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class RoleServiceImplTest {

    @Autowired
    RoleService roleService;

    @MockBean
    private RoleRepository roleRepository;

    @Test
    public void whenFindRoleByNameUser_thenReturnUserRole() {
        //Given
        Role expectedRole = RoleAssembler.getUserRole();
        when(roleRepository.findByName(RoleAssembler.USER_ROLE_NAME)).thenReturn(expectedRole);

        //When
        Role found = roleService.findByName(RoleAssembler.USER_ROLE_NAME);

        //Then
        assertEquals(expectedRole.getName(), found.getName());
        assertEquals(expectedRole.getId(), found.getId());
    }

    @Test
    public void whenFindRoleByNameAdmin_thenReturnAdminRole() {
        //Given
        Role expectedRole = RoleAssembler.getAdminRole();
        when(roleRepository.findByName(RoleAssembler.ADMIN_ROLE_NAME)).thenReturn(expectedRole);

        //When
        Role found = roleService.findByName(RoleAssembler.ADMIN_ROLE_NAME);

        //Then
        assertEquals(expectedRole.getName(), found.getName());
        assertEquals(expectedRole.getId(), found.getId());
    }

}
