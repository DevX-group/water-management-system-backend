package com.backend.water_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "MAIL_USERNAME=testuser",
    "MAIL_PASSWORD=testpass"
})
class WaterManagementSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
