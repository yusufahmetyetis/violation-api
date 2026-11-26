package com.yusuf.jwt_demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Testler için geçici PostgreSQL. docker-compose'daki DB'ye (5433) dokunmaz:
// her test çalıştırmasında boş bir container açılır, Flyway V1, V2... uygular, test bitince silinir.
// @ServiceConnection: spring.datasource.url/username/password'ü bu container'a göre kendisi ayarlar.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");   // compose ile aynı sürüm
    }
}
