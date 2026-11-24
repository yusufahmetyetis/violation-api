package com.yusuf.jwt_demo.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

// Tüm hata cevapları (400/401/403) bu tek formatta döner; Swagger'daki şema da bu.
@Schema(description = "400/401/403 ve login hatalarında dönen ortak gövde")
@JsonInclude(JsonInclude.Include.NON_NULL)   // errors yalnızca doğrulama hatasında görünsün
public record ErrorResponse(
        @Schema(example = "2026-11-23T09:00:00Z") Instant timestamp,
        @Schema(example = "403") int status,
        @Schema(example = "Forbidden") String error,
        @Schema(example = "Bu işlem için yetkiniz yok") String message,
        @Schema(example = "/api/violations") String path,
        @Schema(description = "Sadece 400'de: alan → hata mesajı", example = "{\"plate\": \"plaka boş olamaz\"}",
                nullable = true)
        Map<String, String> errors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }
}
