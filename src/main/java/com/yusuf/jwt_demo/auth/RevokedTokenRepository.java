package com.yusuf.jwt_demo.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    // existsById(jti) hazır geliyor: her istekte primary key üzerinden tek sorgu

    // Süresi dolmuş kayıtları tek bir DELETE ile siler (satır satır yüklemeden)
    @Modifying
    @Query("delete from RevokedToken r where r.expiresAt < :now")
    int deleteExpired(Instant now);
}
