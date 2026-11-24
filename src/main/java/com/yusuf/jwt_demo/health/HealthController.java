package com.yusuf.jwt_demo.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Public", description = "Token gerektirmeyen endpoint'ler")
public class HealthController {

    @GetMapping(value = "/api/public/health", produces = MediaType.TEXT_PLAIN_VALUE)
    @SecurityRequirements()   // Swagger'da kilitsiz görünsün (global bearerAuth şartını kaldırır)
    @Operation(summary = "Sağlık kontrolü", description = "Uygulama ayaktaysa `ok` döner. Token gerekmez.")
    public String health() {
        return "ok";
    }
}
