package com.yusuf.jwt_demo.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    // Token'ı kimin bastığı. AuthController bunu "iss" claim'ine yazar, decoder da bunu şart koşar.
    public static final String ISSUER = "jwt-demo";

    private final SecretKey key;

    public JwtConfig(@Value("${jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 en az 256 bit (32 byte) anahtar ister. Kontrol burada olmazsa kısa secret
        // uygulama açılırken değil, ilk login'de token basılırken patlar.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret en az 32 byte olmalı (HS256), şu an " + bytes.length + " byte");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Token BASAN: login'de kullanacağız
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    // Token OKUYAN: her istekte imzayı, süreyi (exp/nbf) ve issuer'ı (iss) kontrol eder.
    // Varsayılan decoder sadece imza ve süreye bakar; aynı secret'ı paylaşan başka bir sistemin
    // bastığı token'ı da kabul ederdi. createDefaultWithIssuer = süre kontrolü + "iss mutlaka jwt-demo".
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    // Token'daki "roles": ["ADMIN"] → Spring'in anladığı ROLE_ADMIN
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");  // varsayılan "scope"; biz "roles" kullanıyoruz
        roles.setAuthorityPrefix("ROLE_");       // varsayılan "SCOPE_"

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}