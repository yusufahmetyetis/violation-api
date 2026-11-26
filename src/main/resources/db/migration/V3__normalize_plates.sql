-- Flyway migration V3: plakaları uygulamanın Violation.normalizePlate() kuralına getirir.
-- Uygulama artık plakayı kaydederken boşlukları siliyor ve büyük harfe çeviriyor; sahiplik kontrolü
-- birebir karşılaştırma yaptığı için ESKİ satırların da aynı biçimde olması gerekir. Yoksa önceden
-- "34a0261" ya da "34 A 0261" diye girilmiş bir kaydı sahibi (USER) göremez.
--
-- WHERE koşulu: sadece değişmesi gereken satırlara dokunur. Temiz bir veritabanında 0 satır günceller.
-- Not: Demo plakaları ASCII. PostgreSQL'in UPPER'ı veritabanının locale'ine göre çalışır, Türkçe
-- karakterli plakalar olsaydı Java'daki Locale.ROOT ile birebir aynı sonucu vermeyebilirdi.

UPDATE violations
SET plate = UPPER(REGEXP_REPLACE(plate, '\s+', '', 'g'))
WHERE plate <> UPPER(REGEXP_REPLACE(plate, '\s+', '', 'g'));

UPDATE users
SET plate = UPPER(REGEXP_REPLACE(plate, '\s+', '', 'g'))
WHERE plate IS NOT NULL
  AND plate <> UPPER(REGEXP_REPLACE(plate, '\s+', '', 'g'));
