-- Flyway migration V2: demo verileri. Volume dolu olduğu sürece tekrar çalışmaz,
-- yani API'den eklenen/silinen kayıtlar yeniden başlatmada korunur.

-- Roller
INSERT INTO roles (name) VALUES ('ADMIN'), ('OPERATOR'), ('USER');

-- Kullanıcılar. Parolalar: admin123 / operator123 / user123 / pasif123
-- {bcrypt} öneki Spring'e "bu hash'i BCrypt ile kontrol et" der.
INSERT INTO users (username, password, enabled, plate) VALUES
                                                           ('admin',    '{bcrypt}$2a$10$Mdrk18oHHe/fyJv8QzVGBuU81iOIctruCCo4azOCuA1YRrc3iGJ.a', TRUE,  NULL),
                                                           ('operator', '{bcrypt}$2a$10$jCpzluppzjrFjMorDaT3cegzh/VIqGqZSMjIy93MSbCAKDOJCe72W', TRUE,  NULL),
                                                           ('user',     '{bcrypt}$2a$10$e0/l.mnOUYCyMMOTqdL6U.lyT9bjEUijLYXui.Mfj9qNEVFwkZgGu', TRUE,  '34A0261'),
                                                           ('pasif',    '{bcrypt}$2a$10$VbZ9j9f/KHU0cJUhJOPd5.ILcA7T5LSHR2yv10u4OZhsMiJvsi6CO', FALSE, '34B0102');

-- Rol atamaları: id'leri elle yazmak yerine isimle eşliyoruz
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON
    (u.username = 'admin'    AND r.name = 'ADMIN')
        OR (u.username = 'operator' AND r.name = 'OPERATOR')
        OR (u.username = 'user'     AND r.name = 'USER')
        OR (u.username = 'pasif'    AND r.name = 'USER');

-- İhlaller (user'ın plakası 34A0261 → id 1 ve 4)
INSERT INTO violations (plate, violation_date, amount) VALUES
                                                           ('34A0261', DATE '2026-11-11', 12.36),
                                                           ('34B0102', DATE '2026-11-09', 25.12),
                                                           ('06A102',  DATE '2026-10-06', 35.00),
                                                           ('34A0261', DATE '2026-10-28', 18.50);
