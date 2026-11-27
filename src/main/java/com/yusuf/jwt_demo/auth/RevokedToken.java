package com.yusuf.jwt_demo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Logout ile iptal edilmiş bir token (V4__create_revoked_tokens.sql)
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    private String jti;            // token'ın benzersiz kimliği

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;     // token'ın kendi süresi; sonrasında satır silinebilir

    protected RevokedToken() { }   // Hibernate için

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
}
