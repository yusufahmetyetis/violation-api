package com.yusuf.jwt_demo.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Configuration
public class SecurityErrorResponses {

    private final JsonMapper json;   // Boot 4 = Jackson 3 (paket: tools.jackson)

    public SecurityErrorResponses(JsonMapper json) {
        this.json = json;
    }

    // 401: kimlik yok / bozuk / süresi dolmuş
    @Bean
    AuthenticationEntryPoint jsonAuthEntryPoint() {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, ex) -> {
            bearer.commence(request, response, ex);   // WWW-Authenticate header'ı korunsun
            String message = (ex instanceof InvalidBearerTokenException)
                    ? "Token geçersiz: " + reason(ex)   // DEMO: gerçek projede ayrıntı verilmez
                    : "Kimlik doğrulama gerekli";
            write(response, 401, "Unauthorized", message, request.getRequestURI());
        };
    }

    // Spring Security 7 dıştaki exception'a genel "Invalid token" yazar; validator'ların kendi açıklaması
    // ("Token iptal edilmiş", süre/issuer hatası) zincirin içindeki JwtValidationException'da durur.
    private static String reason(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtValidationException invalid) {
                for (OAuth2Error error : invalid.getErrors()) {
                    if (error.getDescription() != null) {
                        return error.getDescription();
                    }
                }
            }
        }
        return ex.getMessage();
    }

    // 403: kimlik var, yetki yok (URL kuralı veya @PreAuthorize)
    @Bean
    AccessDeniedHandler jsonAccessDeniedHandler() {
        BearerTokenAccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
        return (request, response, ex) -> {
            bearer.handle(request, response, ex);
            write(response, 403, "Forbidden", "Bu işlem için yetkiniz yok", request.getRequestURI());
        };
    }

    // 401 ve 403'ün ortak JSON yazıcısı (gövde formatı: ErrorResponse)
    private void write(HttpServletResponse response, int status, String error,
                       String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json.writeValueAsString(ErrorResponse.of(status, error, message, path)));
    }
}