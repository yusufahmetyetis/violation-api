# JWT Demo – Violation API

Spring Boot 4 + Spring Security 7 ile JWT korumalı, rol tabanlı örnek API. Veritabanı Docker'da çalışan PostgreSQL 17; şema Flyway ile yönetilir.

## Çalıştırma

> Gerekenler: Java 25 (`JAVA_HOME` bir JDK 25'i göstermeli) ve Docker Desktop.

**1. Veritabanını başlat** (proje klasöründe):

```bash
docker compose up -d
```

**2. Uygulamayı çalıştır:** IntelliJ'den `JwtDemoApplication` ya da:

```bash
./mvnw spring-boot:run
```

İlk açılışta Flyway tabloları ve demo verileri kurar. Sonraki açılışlarda logda `Schema "public" is up to date` görünür, hiçbir şey tekrar çalışmaz.

## Veritabanı (PostgreSQL + Docker volume)

| Ne | Değer |
|---|---|
| Host / port | `localhost:5433` (container içinde 5432; makinede kurulu bir PostgreSQL 5432'yi kullanıyorsa çakışmasın diye) |
| DB / kullanıcı / parola | `jwtdemo` / `jwtdemo` / `jwtdemo` (demo değerleri) |
| Veri nerede? | `pgdata` adlı named Docker volume (`docker volume ls` → `jwt-demo_pgdata`) |

Bağlantı bilgileri ortam değişkeniyle değiştirilebilir: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

| Komut | Ne olur? |
|---|---|
| `docker compose up -d` | DB'yi başlatır |
| `docker compose down` | Container'ı siler, **veri kalır** (volume duruyor) |
| `docker compose down -v` | Container'ı **ve volume'u** siler; sonraki açılışta Flyway her şeyi baştan kurar |
| `docker exec -it violation-db psql -U jwtdemo` | DB'ye SQL konsoluyla bağlanır |

### Şema değişiklikleri (Flyway)

Tablolar `src/main/resources/db/migration` altındaki dosyalarla kurulur:

- `V1__create_tables.sql`: tablolar
- `V2__insert_demo_data.sql`: demo kullanıcılar ve ihlaller

Flyway hangi dosyaların çalıştığını DB'deki `flyway_schema_history` tablosunda tutar ve her açılışta sadece **yeni** dosyaları çalıştırır.

**Kural:** Çalışmış bir migration dosyası değiştirilmez (Flyway checksum'ı tutar, değişirse uygulama açılmaz). Değişiklik için yeni dosya yazılır, ör. `V3__add_email_to_users.sql`. Hibernate `ddl-auto: validate` ile çalışır: entity'ler tablolarla uyuşmazsa uygulama açılışta hata verir.

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
- `istekler.http`: tam test seti, IntelliJ'de *Run All* ile ✔/✘ sonuçları görünür. Eklediği kaydı kendisi sildiği için kalıcı veritabanında tekrar tekrar çalıştırılabilir.
- Veriyi görmek: `docker exec -it violation-db psql -U jwtdemo -c "SELECT * FROM violations;"`

## Yalnızca demo için olanlar

Gerçek bir projede şunlar farklı olmalı: JWT secret ve DB parolası `application.yaml` / `docker-compose.yml` yerine ortam değişkeninden/Vault'tan okunur; Swagger UI ve `/v3/api-docs` ya kapatılır ya da yetkiye bağlanır; 401 cevabında token hatasının ayrıntısı verilmez.
