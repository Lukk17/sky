package com.lukk.sky.authservice.auth;

import com.lukk.sky.authservice.Assemblers.RoleAssembler;
import com.lukk.sky.authservice.Assemblers.UserAssembler;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static com.lukk.sky.authservice.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.authservice.Assemblers.UserAssembler.TEST_USER_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserDetailsServiceImplTest {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @MockBean
    UserService userService;

    @Test
    public void whenLoadUserByUsername_thenReturnUserDetails() {
        //Given
        User user = UserAssembler.createCompleteTestUser(TEST_USER_EMAIL, 1L);

        UserDetails expected = new org.springframework.security.core.userdetails.User(
                TEST_USER_EMAIL,
                TEST_USER_PASSWORD,
                Collections.singletonList(new SimpleGrantedAuthority(
                        "ROLE_" + RoleAssembler.getUserRole().getName())
                ));
        when(userService.findByUserEmail(any())).thenReturn(user);

        //When
        UserDetails actual = userDetailsService.loadUserByUsername(TEST_USER_EMAIL);

        //Then
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getAuthorities(), actual.getAuthorities());
    }

    @Test(expected = UsernameNotFoundException.class)
    public void whenLoadNotExistingUserByUsername_thenThrowException() {
        //Given
        when(userService.findByUserEmail(any())).thenThrow(new UsernameNotFoundException("User not found"));

        //When
        userDetailsService.loadUserByUsername(TEST_USER_EMAIL);

        //Then - throw exception
    }
}
