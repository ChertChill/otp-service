-- Инициализация дефолтной конфигурации OTP
INSERT INTO otp_config (length, ttl_seconds)
VALUES (6, 300)
ON CONFLICT DO NOTHING;  -- если уже есть, не дублировать

-- (Опционально) Добавление тестового администратора
-- Не используйте в продакшене реальный пароль, здесь только для разработки!
INSERT INTO users (username, password_hash, role)
VALUES (
    'admin',
    'YOUR_HASHED_PASSWORD_HERE',
    'ADMIN'
) ON CONFLICT DO NOTHING;
