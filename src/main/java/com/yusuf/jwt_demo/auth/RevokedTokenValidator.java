package com.yusuf.jwt_demo.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// JwtDecoder her istekte imza, süre ve issuer'dan SONRA bunu da çalıştırır (JwtConfig).
// Logout edilmiş token'ın imzası ve süresi hâlâ geçerlidir; onu ancak bu kontrol yakalar.
@Component
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenRevocationService revocations;

    public RevokedTokenValidator(TokenRevocationService revocations) {
        this.revocations = revocations;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();
        if (jti == null) {
            // Bizim bastığımız her token'da jti var; yoksa iptal edilip edilmediğini bilemeyiz
            return failure("Token kimliği (jti) yok");
        }
        return revocations.isRevoked(jti) ? failure("Token iptal edilmiş") : OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
