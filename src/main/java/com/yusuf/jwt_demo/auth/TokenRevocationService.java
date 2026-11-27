package com.yusuf.jwt_demo.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Token kara listesinin tek giriş noktası: logout'ta yazar, her istekte okunur.
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repo;

    public TokenRevocationService(RevokedTokenRepository repo) {
        this.repo = repo;
    }

    // Logout: token'ı iptal et. Aynı işlemde süresi dolmuş eski kayıtları da temizle;
    // böylece tablo hiç büyümez ve ayrı bir zamanlayıcıya (@Scheduled) gerek kalmaz.
    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        repo.deleteExpired(Instant.now());
        repo.save(new RevokedToken(jti, expiresAt));
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return repo.existsById(jti);
    }
}
