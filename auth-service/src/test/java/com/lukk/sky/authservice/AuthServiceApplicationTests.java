package com.lukk.sky.authservice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        AuthServiceApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class AuthServiceApplicationTests {

    @Test
    public void contextLoads() {
        AuthServiceApplication.main(new String[0]);
    }

}
