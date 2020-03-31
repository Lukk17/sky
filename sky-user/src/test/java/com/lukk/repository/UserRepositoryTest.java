package com.lukk.repository;

import com.lukk.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static com.lukk.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.Assemblers.UserAssembler.createTestUser;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void whenFindByEmail_thenReturnUser() {
        //Given
        User user = createTestUser(TEST_USER_EMAIL);
        entityManager.persist(user);
        entityManager.flush();

        //When
        User found = userRepository.findByEmail(TEST_USER_EMAIL);

        //Then
        assertEquals(user.getEmail(), found.getEmail());
    }
}
