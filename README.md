# JWT Demo – Violation API

Spring Boot 4 + Spring Security 7 ile JWT korumalı, rol tabanlı örnek API (H2, bellek içi veritabanı).

## Çalıştırma

IntelliJ'den `JwtDemoApplication`'ı çalıştırın ya da:

```bash
./mvnw spring-boot:run
```

> Java 25 gerekir (`JAVA_HOME` bir JDK 25'i göstermeli).

## API'yi başka bir geliştirici nasıl kullanır?

| Ne | Adres |
|---|---|
| Swagger UI (oku, dene, curl al) | http://localhost:8080/swagger-ui.html |
| OpenAPI sözleşmesi (JSON) | http://localhost:8080/v3/api-docs |
| OpenAPI sözleşmesi (YAML) | http://localhost:8080/v3/api-docs.yaml |

1. Swagger UI'da **Auth → POST /auth/login** → *Try it out* → *Execute*. Dönen `accessToken`'ı kopyalayın.
2. Sağ üstteki **Authorize**'a yapıştırın (başına `Bearer` yazmadan).
3. Herhangi bir endpoint'te *Try it out* → *Execute*. Cevabın üstünde hazır **curl** komutu çıkar.

Sözleşme dosyasıyla yapılabilecekler:
- **Postman:** *Import* → `http://localhost:8080/v3/api-docs` → tüm istekler koleksiyon olarak gelir.
- **İstemci kodu üretme:** [openapi-generator](https://openapi-generator.tech) ile TypeScript, Kotlin, Java… istemcisi üretilebilir.

Swagger dokümanı koddaki anotasyonlardan üretilir; kod değişince doküman da kendiliğinden güncellenir.

## Demo kullanıcıları

| kullanıcı | parola | rol | not |
|---|---|---|---|
| admin | admin123 | ADMIN | her şey |
| operator | operator123 | OPERATOR | sadece okur |
| user | user123 | USER | sadece kendi plakası (34A0261) |
| pasif | pasif123 | USER | hesap pasif → giriş 401 |

## Endpoint'ler

| Metot | Yol | Rol |
|---|---|---|
| POST | `/auth/login` | herkes |
| GET | `/api/public/health` | herkes |
| GET | `/api/violations` | OPERATOR, ADMIN |
| POST | `/api/violations` | ADMIN |
| DELETE | `/api/violations/{id}` | ADMIN |
| GET | `/api/violations/{id}` | OPERATOR, ADMIN; USER yalnızca kendi kaydı |
| GET | `/api/violations/mine` | USER |
| GET | `/api/admin/users` | ADMIN |

Hatalar tek formatta döner (`ErrorResponse`): **400** eksik/hatalı alan (`errors` alanında ayrıntı), **401** token yok/geçersiz/süresi dolmuş, **403** rol yetersiz ya da kayıt size ait değil.

## Elle test

- `demo.http`: ~3 dakikalık sunum akışı (istekleri tek tek çalıştırın).
- `istekler.http`: tam test seti, IntelliJ'de *Run All* ile ✔/✘ sonuçları görünür.
- H2 konsolu: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:jwtdemo`, kullanıcı `sa`, parola boş).

## Yalnızca demo için olanlar

Gerçek bir projede şunlar farklı olmalı: JWT secret `application.yaml` yerine ortam değişkeninden/Vault'tan okunur; H2 konsolu kapatılır; Swagger UI ve `/v3/api-docs` ya kapatılır ya da yetkiye bağlanır; 401 cevabında token hatasının ayrıntısı verilmez.
