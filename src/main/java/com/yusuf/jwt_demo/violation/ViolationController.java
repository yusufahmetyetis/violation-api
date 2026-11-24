package com.yusuf.jwt_demo.violation;

import com.yusuf.jwt_demo.config.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/violations")
@Tag(name = "Violations", description = "Trafik ihlali kayıtları. Her endpoint'in açıklamasında hangi rolün erişebildiği yazar.")
public class ViolationController {

    private final ViolationRepository repo;
    private final OwnershipChecker ownership;

    public ViolationController(ViolationRepository repo, OwnershipChecker ownership) {   // Spring enjekte eder
        this.repo = repo;
        this.ownership = ownership;
    }

    @Schema(description = "Yeni ihlal kaydı")
    public record ViolationRequest(
            @Schema(description = "Araç plakası", example = "35C0001", maxLength = 15)
            @NotBlank(message = "plaka boş olamaz") @Size(max = 15, message = "plaka en fazla 15 karakter olabilir")
            String plate,

            @Schema(description = "İhlal tarihi (ISO-8601, yyyy-MM-dd)", example = "2026-11-20")
            @NotNull(message = "tarih zorunlu")
            LocalDate date,

            @Schema(description = "Ceza tutarı (TL, en fazla 2 ondalık)", example = "40.00")
            @NotNull(message = "tutar zorunlu") @Positive(message = "tutar sıfırdan büyük olmalı")
            @Digits(integer = 8, fraction = 2, message = "tutar en fazla 8 tam, 2 ondalık basamak olabilir")
            BigDecimal amount) { }

    @GetMapping
    @Operation(summary = "Tüm ihlalleri listele", description = "**Rol:** OPERATOR, ADMIN")
    public List<Violation> getAll() {
        return repo.findAll();
    }

    @PostMapping
    @Operation(summary = "Yeni ihlal ekle",
            description = "**Rol:** ADMIN. Başarılıysa 201 döner; yeni kaydın adresi `Location` header'ındadır.")
    @ApiResponse(responseCode = "201", description = "Kayıt oluşturuldu",
            headers = @Header(name = "Location", description = "Yeni kaydın adresi",
                    schema = @Schema(type = "string", example = "/api/violations/5")),
            content = @Content(schema = @Schema(implementation = Violation.class)))
    @ApiResponse(responseCode = "400", description = "Eksik/hatalı alan (ayrıntı `errors` içinde)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Violation> create(@Valid @RequestBody ViolationRequest req) {
        Violation saved = repo.save(new Violation(req.plate(), req.date(), req.amount()));
        // 201 Created + Location header: "yeni kaynak şu adreste"
        return ResponseEntity.created(URI.create("/api/violations/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "İhlal sil", description = "**Rol:** ADMIN")
    @ApiResponse(responseCode = "204", description = "Silindi (gövde yok)")
    @ApiResponse(responseCode = "404", description = "Bu id ile kayıt yok", content = @Content)
    public ResponseEntity<Void> delete(@Parameter(description = "İhlal id", example = "1") @PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();   // 404
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();       // 204: silindi, dönecek gövde yok
    }

    // USER: sadece kendi plakasının ihlalleri
    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Kendi ihlallerim",
            description = "**Rol:** USER. Token sahibinin plakasına kayıtlı ihlaller döner.")
    public List<Violation> mine(Authentication auth) {
        String plate = ownership.plateOf(auth);
        return plate == null ? List.of() : repo.findByPlate(plate);
    }

    // OPERATOR/ADMIN: her kayıt. USER: sadece kendi kaydı, değilse 403
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN') or @ownership.ownsViolation(#id, authentication)")
    @Operation(summary = "Tek ihlal getir",
            description = "**Rol:** OPERATOR, ADMIN her kaydı görür. USER yalnızca kendi plakasına ait kaydı görür; "
                    + "başkasının kaydı ya da var olmayan bir id için 403 alır (kaydın varlığı sızdırılmaz).")
    @ApiResponse(responseCode = "200", description = "Kayıt bulundu")
    @ApiResponse(responseCode = "404", description = "Bu id ile kayıt yok (yalnızca OPERATOR/ADMIN)", content = @Content)
    public ResponseEntity<Violation> getById(@Parameter(description = "İhlal id", example = "1") @PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
