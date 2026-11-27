package com.yusuf.jwt_demo;

import com.yusuf.jwt_demo.auth.RevokedToken;
import com.yusuf.jwt_demo.auth.RevokedTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /auth/logout: token jti'si PostgreSQL'deki revoked_tokens kara listesine yazılır,
 * sonraki her istekte RevokedTokenValidator bu listeye bakar.
 */
class LogoutIntegrationTests extends IntegrationTestSupport {

    @Autowired RevokedTokenRepository revokedTokens;

    private void logout(String token) throws Exception {
        mvc.perform(post("/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Logout → 204; aynı token'la sonraki istek → 401 'Token iptal edilmiş'")
    void revokedTokenIsRejected() throws Exception {
        String token = admin();
        mvc.perform(get("/api/violations").header("Authorization", bearer(token))).andExpect(status().isOk());

        logout(token);

        mvc.perform(get("/api/violations").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Token iptal edilmiş")))
                .andExpect(jsonPath("$.path", is("/api/violations")));
    }

    @Test
    @DisplayName("Logout sonrası tekrar login olunur, yeni token çalışır (sadece eski token iptal)")
    void reLoginGivesWorkingToken() throws Exception {
        String old = user();
        logout(old);

        String fresh = user();
        mvc.perform(get("/api/violations/mine").header("Authorization", bearer(fresh))).andExpect(status().isOk());
        mvc.perform(get("/api/violations/mine").header("Authorization", bearer(old))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bir kullanıcının logout'u diğer kullanıcıların token'larını etkilemez")
    void otherUsersUnaffected() throws Exception {
        String adminToken = admin();
        String operatorToken = operator();

        logout(adminToken);

        mvc.perform(get("/api/violations").header("Authorization", bearer(operatorToken))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Aynı kullanıcının başka bir token'ı (ör. başka cihaz) etkilenmez")
    void sameUserOtherTokenUnaffected() throws Exception {
        String laptop = admin();
        String phone = admin();

        logout(laptop);

        mvc.perform(get("/api/violations").header("Authorization", bearer(phone))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token'sız logout → 401")
    void logoutWithoutToken() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/auth/logout")));
    }

    @Test
    @DisplayName("Aynı token'la ikinci logout → 401 (token zaten iptal)")
    void secondLogoutRejected() throws Exception {
        String token = operator();
        logout(token);

        mvc.perform(post("/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/auth/login hâlâ public: iptal edilmiş token header'da olsa da giriş çalışır")
    void loginStillPublicWithRevokedToken() throws Exception {
        String token = admin();
        logout(token);

        mvc.perform(post("/auth/login").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout süresi dolmuş eski kayıtları temizler, geçerli olanlara dokunmaz")
    void logoutCleansUpExpiredEntries() throws Exception {
        String expiredJti = UUID.randomUUID().toString();
        String stillValidJti = UUID.randomUUID().toString();
        revokedTokens.save(new RevokedToken(expiredJti, Instant.now().minusSeconds(60)));
        revokedTokens.save(new RevokedToken(stillValidJti, Instant.now().plusSeconds(600)));

        logout(admin());

        assertThat(revokedTokens.existsById(expiredJti)).as("süresi dolmuş kayıt silinmeli").isFalse();
        assertThat(revokedTokens.existsById(stillValidJti)).as("geçerli kayıt kalmalı").isTrue();
    }
}
