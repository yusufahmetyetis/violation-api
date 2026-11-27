package com.yusuf.jwt_demo.auth;

import com.yusuf.jwt_demo.config.ErrorResponse;
import com.yusuf.jwt_demo.config.JwtConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Token alma ve iptal etme. Diğer endpoint'leri denemeden önce buradan token alın.")
public class AuthController {

    @Schema(description = "Giriş bilgileri")
    public record LoginRequest(
            @Schema(example = "admin") @NotBlank(message = "kullanıcı adı boş olamaz") String username,
            @Schema(example = "admin123") @NotBlank(message = "parola boş olamaz") String password) { }

    @Schema(description = "Başarılı girişte dönen token")
    public record TokenResponse(
            @Schema(description = "JWT. Swagger'da sağ üstteki Authorize'a yapıştırın (Bearer yazmadan).",
                    example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
            @Schema(description = "Authorization header'ında kullanılacak şema", example = "Bearer") String tokenType,
            @Schema(description = "Geçerlilik süresi (saniye)", example = "900") long expiresIn) { }

    private final AuthenticationManager authManager;
    private final JwtEncoder jwtEncoder;
    private final TokenRevocationService revocations;
    private final long expSeconds;

    public AuthController(AuthenticationManager authManager, JwtEncoder jwtEncoder,
                          TokenRevocationService revocations,
                          @Value("${jwt.expiration-seconds}") long expSeconds) {
        this.authManager = authManager;
        this.jwtEncoder = jwtEncoder;
        this.revocations = revocations;
        this.expSeconds = expSeconds;
    }

    @PostMapping("/logout")
    @Operation(summary = "Çıkış yap, token'ı iptal et",
            description = """
                    Gönderilen token kara listeye alınır. Süresi dolmamış olsa bile o token'la yapılan sonraki
                    her istek **401 (Token iptal edilmiş)** alır. Kullanıcının diğer token'larını etkilemez;
                    tekrar login olunca yeni token sorunsuz çalışır.""")
    @ApiResponse(responseCode = "204", description = "Token iptal edildi (gövde yok)")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        // Buraya gelen token imza/süre/issuer/kara liste kontrollerinden geçmiş, yani geçerli bir token.
        revocations.revoke(jwt.getId(), jwt.getExpiresAt());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @SecurityRequirements()   // public: Swagger'da kilitsiz görünsün
    @Operation(summary = "Giriş yap, JWT al",
            description = """
                    Demo kullanıcıları:
                    | kullanıcı | parola | rol |
                    |---|---|---|
                    | admin | admin123 | ADMIN |
                    | operator | operator123 | OPERATOR |
                    | user | user123 | USER (plaka 34A0261) |
                    | pasif | pasif123 | USER, hesap pasif → 401 |
                    """)
    @ApiResponse(responseCode = "200", description = "Giriş başarılı")
    @ApiResponse(responseCode = "400", description = "Kullanıcı adı veya parola boş",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Kullanıcı adı/parola hatalı veya hesap pasif (aynı mesaj)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        // Yanlış parola veya pasif kullanıcıda burada exception fırlar → aşağıdaki handler → 401
        Authentication auth = authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password()));

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))     // FACTOR_PASSWORD gibi şeyleri at
                .map(a -> a.substring("ROLE_".length())) // token'a "ADMIN" yaz; prefix'i converter geri ekler
                .toList();

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)                // iss: kim bastı (decoder bunu şart koşar)
                .subject(auth.getName())                 // sub: kim
                .issuedAt(now)                           // iat: ne zaman
                .expiresAt(now.plusSeconds(expSeconds))  // exp: ne zamana kadar geçerli
                .id(UUID.randomUUID().toString())        // jti: token'ın kimliği (kara liste için)
                .claim("roles", roles)
                .build();

        // Encoder varsayılan olarak RS256 bekler. HS256 olduğunu header'da açıkça söylüyoruz.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", expSeconds);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> loginFailed(AuthenticationException e, HttpServletRequest request) {
        // yanlış parola ve pasif kullanıcı için AYNI mesaj: dışarıya ipucu yok
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                401, "Unauthorized", "Kullanıcı adı veya parola hatalı", request.getRequestURI()));
    }
}
