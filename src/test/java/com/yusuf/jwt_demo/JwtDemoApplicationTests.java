package com.yusuf.jwt_demo;

import org.junit.jupiter.api.Test;

// Uygulama context'i (Flyway + Hibernate validate dahil) hatasız ayağa kalkıyor mu?
// IntegrationTestSupport'u extend ettiği için diğer entegrasyon testleriyle aynı container'ı paylaşır.
class JwtDemoApplicationTests extends IntegrationTestSupport {

	@Test
	void contextLoads() {
	}

}
