package com.xinzhe.projectmentor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "projectmentor.jwt.secret=context-load-test-secret-not-for-production"
})
class ProjectmentorServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
