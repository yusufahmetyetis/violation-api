package com.yusuf.jwt_demo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uygulamanın güvenlik davranışını uçtan uca test eder: gerçek login → gerçek JWT → gerçek
 * filtre zinciri → Testcontainers'taki gerçek PostgreSQL (Flyway migration'larıyla kurulmuş).
 * Demo verisi V2__insert_demo_data.sql'den gelir: user'ın plakası 34A0261 (ihlal 1 ve 4),
 * ihlal 2 başkasına (34B0102) ait.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SecurityIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder jwtEncoder;

    // ---------------------------------------------------------------- yardımcılar

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String admin() throws Exception { return login("admin", "admin123"); }
    private String operator() throws Exception { return login("operator", "operator123"); }
    private String user() throws Exception { return login("user", "user123"); }

    private static String bearer(String token) { return "Bearer " + token; }

    /** Uygulamanın kendi anahtarıyla, istediğimiz claim'lerle token üretir (süresi dolmuş, yanlış issuer...). */
    private String customToken(String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("admin")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Tüm hata cevaplarının ortak ErrorResponse formatında olduğunu doğrular. */
    private static void assertErrorFormat(ResultActions result, int status, String path) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status", is(status)))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path", is(path)))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("Doğru parola → 200 ve Bearer token")
    void loginSucceeds() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("Yanlış parola ve pasif hesap → aynı 401 mesajı (ipucu sızmaz)")
    void wrongPasswordAndDisabledAccountLookTheSame() throws Exception {
        String wrong = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"yanlis\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String disabled = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pasif\",\"password\":\"pasif123\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongMsg = JsonPath.read(wrong, "$.message");
        String disabledMsg = JsonPath.read(disabled, "$.message");
        org.assertj.core.api.Assertions.assertThat(disabledMsg).isEqualTo(wrongMsg);
    }

    // ---------------------------------------------------------------- token doğrulama (401)

    @Test
    @DisplayName("Token yok → 401")
    void noToken() throws Exception {
        assertErrorFormat(mvc.perform(get("/api/violations")), 401, "/api/violations");
    }

    @Test
    @DisplayName("Bozuk token → 401")
    void garbageToken() throws Exception {
        assertErrorFormat(mvc.perform(get("/api/violations").header("Authorization", bearer("bu.bir.tokendegil"))),
                401, "/api/violations");
    }

    @Test
    @DisplayName("Süresi dolmuş token → 401")
    void expiredToken() throws Exception {
        // Nimbus 60 sn saat kayması toleransı tanır, o yüzden 5 dk önce dolmuş bir token
        String expired = customToken("jwt-demo", Instant.now().minusSeconds(300));
        assertErrorFormat(mvc.perform(get("/api/violations").header("Authorization", bearer(expired))),
                401, "/api/violations");
    }

    @Test
    @DisplayName("İmzası doğru ama issuer'ı farklı token → 401")
    void wrongIssuer() throws Exception {
        String foreign = customToken("baska-bir-sistem", Instant.now().plusSeconds(900));
        assertErrorFormat(mvc.perform(get("/api/violations").header("Authorization", bearer(foreign))),
                401, "/api/violations");
    }

    @Test
    @DisplayName("Login'e süresi dolmuş token'la gelmek girişi engellemez (public yolda Bearer yok sayılır)")
    void expiredTokenOnLoginIsIgnored() throws Exception {
        String expired = customToken("jwt-demo", Instant.now().minusSeconds(300));
        mvc.perform(post("/auth/login").header("Authorization", bearer(expired))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- rol matrisi (URL kuralları)

    @Test
    @DisplayName("GET /api/violations: ADMIN ve OPERATOR 200, USER 403")
    void listViolationsByRole() throws Exception {
        mvc.perform(get("/api/violations").header("Authorization", bearer(admin()))).andExpect(status().isOk());
        mvc.perform(get("/api/violations").header("Authorization", bearer(operator()))).andExpect(status().isOk());
        assertErrorFormat(mvc.perform(get("/api/violations").header("Authorization", bearer(user()))),
                403, "/api/violations");
    }

    @Test
    @DisplayName("POST ve DELETE /api/violations: sadece ADMIN")
    void writeOnlyForAdmin() throws Exception {
        String body = "{\"plate\":\"06TEST01\",\"date\":\"2026-11-26\",\"amount\":10.00}";
        for (String token : List.of(operator(), user())) {
            mvc.perform(post("/api/violations").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/violations/1").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("GET /api/admin/users: sadece ADMIN")
    void adminUsersOnlyForAdmin() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin()))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(operator())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(user())))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- sahiplik (@PreAuthorize)

    @Test
    @DisplayName("USER kendi kaydını görür (ihlal 1)")
    void userSeesOwnViolation() throws Exception {
        mvc.perform(get("/api/violations/1").header("Authorization", bearer(user())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate", is("34A0261")));
    }

    @Test
    @DisplayName("USER başkasının kaydına 403 alır, 500 değil (genel handler 403'ü yutmamalı)")
    void userCannotSeeOthersViolation() throws Exception {
        // @PreAuthorize'ın AccessDeniedException'ı ApiExceptionHandler'daki Exception handler'ına düşer.
        // Orada yeniden fırlatılmazsa bu test 500 görür.
        assertErrorFormat(mvc.perform(get("/api/violations/2").header("Authorization", bearer(user()))),
                403, "/api/violations/2");
    }

    @Test
    @DisplayName("Olmayan kayıt: USER 403 (varlık sızmaz), ADMIN 404")
    void missingViolation() throws Exception {
        mvc.perform(get("/api/violations/999999").header("Authorization", bearer(user())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/violations/999999").header("Authorization", bearer(admin())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/mine sadece token sahibinin plakasını döner; OPERATOR'a kapalı")
    void mineReturnsOnlyOwnPlate() throws Exception {
        mvc.perform(get("/api/violations/mine").header("Authorization", bearer(user())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].plate", everyItem(is("34A0261"))))
                .andExpect(jsonPath("$[*].id", hasItem(1)))
                .andExpect(jsonPath("$[*].id", hasItem(4)));
        mvc.perform(get("/api/violations/mine").header("Authorization", bearer(operator())))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- plaka normalizasyonu

    @Test
    @DisplayName("Admin ' 34 a 0261 ' girer → 34A0261 kaydedilir, USER kaydı görür")
    void plateIsNormalizedAndOwnerCanSeeIt() throws Exception {
        String admin = admin();
        String body = mvc.perform(post("/api/violations").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\" 34 a 0261 \",\"date\":\"2026-11-26\",\"amount\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate", is("34A0261")))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        try {
            String user = user();
            mvc.perform(get("/api/violations/" + id).header("Authorization", bearer(user)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/violations/mine").header("Authorization", bearer(user)))
                    .andExpect(jsonPath("$[*].id", hasItem(id)));
        } finally {
            mvc.perform(delete("/api/violations/" + id).header("Authorization", bearer(admin)))
                    .andExpect(status().isNoContent());
        }
    }

    // ---------------------------------------------------------------- hata formatı

    @Test
    @DisplayName("Eksik alan → 400 ve alan bazında errors")
    void validationError() throws Exception {
        mvc.perform(post("/api/violations").header("Authorization", bearer(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plate\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.plate").isString())
                .andExpect(jsonPath("$.errors.date").isString());
    }

    @Test
    @DisplayName("/api/violations/abc → 400, ErrorResponse formatında")
    void typeMismatch() throws Exception {
        ResultActions result = mvc.perform(get("/api/violations/abc").header("Authorization", bearer(admin())));
        assertErrorFormat(result, 400, "/api/violations/abc");
        // kullanıcının gönderdiği değer mesaja geri yansıtılmaz
        result.andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abc"))));
    }

    @Test
    @DisplayName("Olmayan adres → 404, yanlış metot → 405; ikisi de ErrorResponse formatında")
    void notFoundAndMethodNotAllowed() throws Exception {
        String admin = admin();
        assertErrorFormat(mvc.perform(get("/api/yok").header("Authorization", bearer(admin))), 404, "/api/yok");
        assertErrorFormat(mvc.perform(put("/api/violations/1").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")),
                405, "/api/violations/1");
    }
}
