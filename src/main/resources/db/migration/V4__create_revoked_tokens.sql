-- Flyway migration V4: logout ile iptal edilen token'ların kara listesi.
-- JWT stateless'tır: sunucu verdiği token'ı hatırlamaz, süresi dolana kadar kabul eder.
-- Logout'ta token'ın jti'si (benzersiz kimliği) buraya yazılır; her istekte "bu jti iptal mi?" diye bakılır.

CREATE TABLE revoked_tokens (
    jti        VARCHAR(36) PRIMARY KEY,   -- token'ın "jti" claim'i (UUID)
    expires_at TIMESTAMPTZ NOT NULL       -- token'ın kendi "exp"i. Bu andan sonra token zaten geçersiz,
                                          -- satır güvenle silinebilir (tablo büyümesin diye)
);
