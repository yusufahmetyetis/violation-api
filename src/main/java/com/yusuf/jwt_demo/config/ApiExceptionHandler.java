package com.yusuf.jwt_demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Controller katmanında oluşan hataları (400, 404, 405, 500...) ortak ErrorResponse formatına çevirir.
// (Token yok/geçersiz 401 ve URL kuralı 403'leri controller'a gelmeden Security filtrelerinde üretilir
//  → SecurityErrorResponses)
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    // Yoldaki/parametredeki değer tipe uymuyor: /api/violations/abc (id sayı olmalı)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request",
                "'" + e.getName() + "' için geçersiz değer: " + e.getValue(), request.getRequestURI()));
    }

    // Olmayan adres → 404, adres var ama metot yanlış (ör. PUT) → 405
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> notFoundOrWrongMethod(Exception e, HttpServletRequest request) {
        boolean notFound = e instanceof NoResourceFoundException;
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.METHOD_NOT_ALLOWED;
        String message = notFound ? "Bu adreste bir kaynak yok" : "Bu adres bu HTTP metodunu desteklemiyor";
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), status.getReasonPhrase(),
                message, request.getRequestURI()));
    }

    // Geri kalan her şey. Önce iki istisna:
    //  - Güvenlik hataları (@PreAuthorize'ın 403'ü vb.) burada yutulursa 500 olur. Aynen geri fırlatıyoruz,
    //    Spring Security bunları SecurityErrorResponses'taki 401/403'e çevirir.
    //  - Spring'in kendi HTTP hataları (415, 406...) kendi durum koduyla, bizim formatımızda döner.
    // Gerçek bir hata ise ayrıntı loga yazılır, istemciye sadece genel mesaj gider (stack trace sızmasın).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) throws Exception {
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode code = springError.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            return ResponseEntity.status(code).body(ErrorResponse.of(code.value(),
                    status != null ? status.getReasonPhrase() : "Error",
                    springError.getBody().getDetail(), request.getRequestURI()));
        }
        log.error("Beklenmeyen hata: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(ErrorResponse.of(500, "Internal Server Error",
                "Beklenmeyen bir hata oluştu", request.getRequestURI()));
    }
}
