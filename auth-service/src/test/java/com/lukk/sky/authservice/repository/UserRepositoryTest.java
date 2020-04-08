package com.lukk.sky.authservice.repository;

import com.lukk.sky.authservice.Assemblers.UserAssembler;
import com.lukk.sky.authservice.entity.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void whenFindByEmail_thenReturnUser() {
        //Given
        User user = UserAssembler.createTestUser(UserAssembler.TEST_USER_EMAIL);
        entityManager.persist(user);
        entityManager.flush();

        //When
        User found = userRepository.findByEmail(UserAssembler.TEST_USER_EMAIL);

        //Then
        Assert.assertEquals(user.getEmail(), found.getEmail());
    }
}
