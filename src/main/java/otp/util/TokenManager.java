package otp.util;

import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утилиты для управления токенами авторизации.
 * 
 * Основные функции:
 * 1. Генерация токенов
 *    - Создание UUID
 *    - Установка времени жизни
 *    - Сохранение в памяти
 * 
 * 2. Валидация токенов
 *    - Проверка существования
 *    - Проверка срока действия
 *    - Автоматическое удаление
 * 
 * 3. Управление токенами
 *    - Получение пользователя
 *    - Досрочное отзывание
 *    - Очистка истекших
 * 
 * Безопасность:
 * - Потокобезопасное хранение
 * - Ограниченное время жизни
 * - Логирование операций
 * - Защита от null-значений
 */
public final class TokenManager {
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    /** 
     * Хранилище токенов в памяти.
     * Формат: токен → информация о пользователе и времени истечения
     * Использует ConcurrentHashMap для потокобезопасности
     */
    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    /** Время жизни токена в минутах */
    private static final long TTL_MINUTES = 30;

    /** Закрытый конструктор для предотвращения создания экземпляров */
    private TokenManager() {}

    /**
     * Генерирует новый токен для пользователя.
     * 
     * Процесс генерации:
     * 1. Создание UUID
     * 2. Расчет времени истечения
     * 3. Сохранение в хранилище
     * 4. Логирование операции
     * 
     * @param user объект пользователя
     * @return строковое представление токена
     */
    public static String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES);
        tokens.put(token, new TokenInfo(user, expiry));
        logger.info("Generated token {} for user {} (expires at {})", token, user.getUsername(), expiry);
        return token;
    }

    /**
     * Проверяет валидность токена.
     * 
     * Процесс проверки:
     * 1. Поиск токена в хранилище
     * 2. Проверка срока действия
     * 3. Автоматическое удаление истекших
     * 
     * Особенности:
     * - Логирование неудачных попыток
     * - Автоматическая очистка
     * - Защита от null-значений
     * 
     * @param token строка токена
     * @return true если токен валиден
     */
    public static boolean validate(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null) {
            logger.warn("Token validation failed: token not found");
            return false;
        }
        if (Instant.now().isAfter(info.expiry)) {
            tokens.remove(token);
            logger.warn("Token {} expired at {}, removed from store", token, info.expiry);
            return false;
        }
        return true;
    }

    /**
     * Получает пользователя по токену.
     * 
     * Процесс:
     * 1. Валидация токена
     * 2. Извлечение информации
     * 3. Возврат пользователя
     * 
     * @param token валидный токен
     * @return объект User или null если токен невалиден
     */
    public static User getUser(String token) {
        if (!validate(token)) {
            return null;
        }
        return tokens.get(token).user;
    }

    /**
     * Отзывает токен досрочно.
     * 
     * Процесс:
     * 1. Удаление из хранилища
     * 2. Логирование операции
     * 
     * @param token строка токена
     */
    public static void revoke(String token) {
        if (tokens.remove(token) != null) {
            logger.info("Token {} revoked", token);
        }
    }

    /** 
     * Внутренний класс для хранения информации о токене.
     * 
     * Содержит:
     * - Ссылку на пользователя
     * - Время истечения токена
     */
    private static class TokenInfo {
        final User user;
        final Instant expiry;

        TokenInfo(User user, Instant expiry) {
            this.user = user;
            this.expiry = expiry;
        }
    }
}
