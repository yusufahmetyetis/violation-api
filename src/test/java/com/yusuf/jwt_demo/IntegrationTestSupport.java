package com.yusuf.jwt_demo;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entegrasyon testlerinin ortak tabanı: gerçek uygulama + MockMvc + Testcontainers PostgreSQL.
 * Bütün alt sınıflar (ve JwtDemoApplicationTests) aynı anotasyonları taşıdığı için Spring test
 * context'ini paylaşır; tüm test çalıştırması için tek bir PostgreSQL container'ı açılır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTestSupport {

    @Autowired protected MockMvc mvc;

    /** Gerçek /auth/login'den token alır. */
    protected String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected String admin() throws Exception { return login("admin", "admin123"); }
    protected String operator() throws Exception { return login("operator", "operator123"); }
    protected String user() throws Exception { return login("user", "user123"); }

    protected static String bearer(String token) { return "Bearer " + token; }
}
