package com.yusuf.jwt_demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Controller'a ulaşan isteklerde oluşan 400'leri ortak formata çevirir.
// (401/403 controller'a gelmeden Security filtrelerinde üretilir → SecurityErrorResponses)
@RestControllerAdvice
public class ApiExceptionHandler {

    // @Valid başarısız: hangi alan neden hatalı, tek tek döner
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationFailed(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), 400, "Bad Request",
                "İstek doğrulanamadı", request.getRequestURI(), errors));
    }

    // JSON bozuk ya da tip uyuşmuyor (ör. tarih "20.11.2026", tutar "kırk")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableBody(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request",
                "İstek gövdesi okunamadı: JSON formatını ve alan tiplerini kontrol edin", request.getRequestURI()));
    }
}
