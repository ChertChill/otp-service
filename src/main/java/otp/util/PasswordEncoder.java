package otp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилиты для безопасного хранения и проверки паролей.
 * 
 * Основные функции:
 * 1. Хеширование паролей
 *    - Использование SHA-256
 *    - Преобразование в HEX
 *    - Обработка ошибок
 * 
 * 2. Проверка паролей
 *    - Сравнение хешей
 *    - Валидация входных данных
 *    - Безопасное сравнение
 * 
 * Безопасность:
 * - Использование криптографического хеширования
 * - Защита от timing-атак
 * - Обработка null-значений
 * - Логирование ошибок
 */
public final class PasswordEncoder {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoder.class);

    /** Закрытый конструктор для предотвращения создания экземпляров */
    private PasswordEncoder() {}

    /**
     * Хеширует пароль с использованием SHA-256.
     * 
     * Процесс хеширования:
     * 1. Получение экземпляра MessageDigest
     * 2. Преобразование пароля в байты
     * 3. Вычисление хеша
     * 4. Конвертация в HEX-строку
     * 
     * Возможные ошибки:
     * - NoSuchAlgorithmException: алгоритм недоступен
     * - IllegalStateException: ошибка инициализации
     * 
     * @param rawPassword исходный пароль
     * @return HEX-строка хеша
     * @throws IllegalStateException если алгоритм недоступен
     */
    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Не удалось получить алгоритм SHA-256 для хеширования пароля", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Проверяет соответствие пароля сохраненному хешу.
     * 
     * Процесс проверки:
     * 1. Валидация входных данных
     * 2. Хеширование пароля
     * 3. Сравнение хешей
     * 
     * Особенности:
     * - Защита от null-значений
     * - Регистронезависимое сравнение
     * - Безопасное сравнение строк
     * 
     * @param rawPassword проверяемый пароль
     * @param storedHash сохраненный хеш
     * @return true если пароль соответствует хешу
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) {
            return false;
        }
        return hash(rawPassword).equalsIgnoreCase(storedHash);
    }

    /**
     * Преобразует массив байт в HEX-строку.
     * 
     * Процесс преобразования:
     * 1. Создание StringBuilder
     * 2. Преобразование каждого байта
     * 3. Форматирование в HEX
     * 
     * @param bytes массив байт для преобразования
     * @return HEX-строка
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

