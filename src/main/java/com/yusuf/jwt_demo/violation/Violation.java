package com.yusuf.jwt_demo.violation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "violations")          // schema.sql'deki tablo adı
@Schema(description = "Trafik ihlali kaydı")
@JsonPropertyOrder({"id", "plate", "date", "amount"})   // JSON alan sırası sabit
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id'yi DB'nin sayacı üretir
    @Schema(description = "Kayıt kimliği (sunucu üretir)", example = "1", accessMode = Schema.AccessMode.READ_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Araç plakası", example = "34A0261", requiredMode = Schema.RequiredMode.REQUIRED)
    private String plate;

    @Column(name = "violation_date", nullable = false)   // "date" SQL'de ayrılmış kelime
    @Schema(description = "İhlal tarihi (ISO-8601, yyyy-MM-dd)", example = "2026-11-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Column(nullable = false)
    @Schema(description = "Ceza tutarı (TL, 2 ondalık)", example = "12.36", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    protected Violation() { }          // Hibernate için zorunlu, dışarıdan kullanılmasın diye protected

    public Violation(String plate, LocalDate date, BigDecimal amount) {   // POST'ta kullanılıyor
        this.plate = normalizePlate(plate);
        this.date = date;
        this.amount = amount;
    }

    // "34 a 0261 " → "34A0261". Sahiplik kontrolü plakayı birebir karşılaştırdığı için kayıt hep tek biçimde tutulur.
    // Locale.ROOT: Türkçe locale'de "i".toUpperCase() → "İ" olur, plaka karşılaştırması bozulur.
    public static String normalizePlate(String plate) {
        return plate == null ? null : plate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    // Jackson JSON'a çevirirken getter'ları kullanır
    public Long getId() { return id; }
    public String getPlate() { return plate; }
    public LocalDate getDate() { return date; }
    public BigDecimal getAmount() { return amount; }
}
