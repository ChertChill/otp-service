package otp.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

/**
 * Утилиты для работы с JWT токенами.
 * 
 * Основные функции:
 * 1. Генерация токенов
 *    - Создание JWT
 *    - Установка срока действия
 *    - Подпись токена
 * 
 * 2. Валидация токенов
 *    - Проверка подписи
 *    - Проверка срока действия
 *    - Обработка ошибок
 * 
 * 3. Извлечение данных
 *    - Получение имени пользователя
 *    - Проверка валидности
 *    - Безопасное извлечение
 * 
 * Безопасность:
 * - Использование HMAC256
 * - Секретный ключ
 * - Ограниченный срок действия
 * - Обработка исключений
 */
public class JwtUtils {

    /** Секретный ключ для подписи токенов */
    private static final String SECRET_KEY = "mySecretKey";

    /**
     * Генерирует JWT токен для указанного пользователя.
     * 
     * Процесс генерации:
     * 1. Создание токена с именем пользователя
     * 2. Установка времени создания
     * 3. Установка срока действия (1 час)
     * 4. Подпись токена
     * 
     * Структура токена:
     * - subject: имя пользователя
     * - issuedAt: время создания
     * - expiresAt: время истечения
     * 
     * @param username имя пользователя
     * @return подписанный JWT токен
     */
    public static String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // Токен действует 1 час
                .sign(Algorithm.HMAC256(SECRET_KEY)); // Подпись токена
    }

    /**
     * Проверяет валидность JWT токена.
     * 
     * Процесс проверки:
     * 1. Верификация подписи
     * 2. Проверка срока действия
     * 3. Обработка ошибок
     * 
     * Возможные ошибки:
     * - Неверная подпись
     * - Истекший срок действия
     * - Некорректный формат
     * 
     * @param token JWT токен для проверки
     * @return true если токен валиден, false в противном случае
     */
    public static boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token); // Проверка подписи и срока действия
            return true;
        } catch (Exception e) {
            return false; // Если токен невалиден
        }
    }

    /**
     * Извлекает имя пользователя из JWT токена.
     * 
     * Процесс извлечения:
     * 1. Верификация токена
     * 2. Извлечение subject
     * 3. Проверка валидности
     * 
     * Возможные ошибки:
     * - Неверная подпись
     * - Истекший срок действия
     * - Некорректный формат
     * 
     * @param token JWT токен
     * @return имя пользователя из токена
     * @throws Exception если токен невалиден
     */
    public static String extractUsername(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token); // Верификация токена
        return decodedJWT.getSubject(); // Возвращает имя пользователя
    }
}

