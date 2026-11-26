package com.yusuf.jwt_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

// Anotasyonlar SecurityIntegrationTests ile birebir aynı: Spring test context'ini paylaşırlar,
// böylece tüm test çalıştırması için tek bir PostgreSQL container'ı açılır.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class JwtDemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
