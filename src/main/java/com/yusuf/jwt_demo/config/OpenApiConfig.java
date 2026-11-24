package com.yusuf.jwt_demo.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "JWT Demo – Violation API", version = "v1",
                description = """
                        Trafik ihlali kayıtları için JWT korumalı örnek API.

                        **Nasıl denenir?**
                        1. **Auth → POST /auth/login** ile giriş yapın (kullanıcılar endpoint açıklamasında).
                        2. Dönen `accessToken` değerini sağ üstteki **Authorize** butonuna yapıştırın.
                        3. İstediğiniz endpoint'te **Try it out → Execute**. Cevabın üstünde hazır **curl** komutu görünür.

                        **Roller:** ADMIN her şeyi yapar · OPERATOR okur · USER yalnızca kendi plakasının kayıtlarını görür.

                        **Sözleşme dosyası:** `/v3/api-docs` (JSON) veya `/v3/api-docs.yaml`. Postman'e import edilebilir,
                        openapi-generator ile istemci kodu üretilebilir.

                        Tüm hatalar `ErrorResponse` formatında döner: 400 eksik/hatalı alan, 401 token yok/geçersiz/süresi dolmuş,
                        403 rol yetersiz veya kayıt size ait değil.
                        """),
        security = @SecurityRequirement(name = "bearerAuth")   // varsayılan: her endpoint token ister
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "POST /auth/login ile alınan token. Başına 'Bearer' yazmayın, Swagger ekler."
)
public class OpenApiConfig {

    private static final List<String> TAG_ORDER = List.of("Auth", "Violations", "Admin", "Public");

    // Token isteyen her endpoint'e 401 ve 403'ü tek yerden ekler (her metoda tek tek yazmamak için).
    // @SecurityRequirements() ile public işaretlenenler (login, health) atlanır.
    @Bean
    OpenApiCustomizer securityErrorsCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().read(ErrorResponse.class)
                    .forEach(openApi.getComponents()::addSchemas);

            // Swagger UI'daki grup sırası: önce token al (Auth), sonra kullan
            if (openApi.getTags() != null) {
                Map<String, io.swagger.v3.oas.models.tags.Tag> unique = new LinkedHashMap<>();
                openApi.getTags().forEach(t -> unique.putIfAbsent(t.getName(), t));
                List<io.swagger.v3.oas.models.tags.Tag> sorted = new ArrayList<>(unique.values());
                sorted.sort(Comparator.comparingInt(t -> {
                    int i = TAG_ORDER.indexOf(t.getName());
                    return i < 0 ? Integer.MAX_VALUE : i;
                }));
                openApi.setTags(sorted);
            }

            openApi.getPaths().values().forEach(path ->
                    path.readOperations().forEach(op -> {
                        boolean isPublic = op.getSecurity() != null && op.getSecurity().isEmpty();
                        if (isPublic) return;
                        op.getResponses().addApiResponse("401", error("Token yok / geçersiz / süresi dolmuş"));
                        op.getResponses().addApiResponse("403", error("Rol yetersiz veya kayıt size ait değil"));
                    }));
        };
    }

    private static ApiResponse error(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
    }
}
