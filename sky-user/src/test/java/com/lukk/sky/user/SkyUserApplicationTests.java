package com.lukk.sky.user;

import com.lukk.sky.common.H2TestProfileJPAConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyUserApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class SkyUserApplicationTests {

    @Test
    public void contextLoads() {
    }

}
