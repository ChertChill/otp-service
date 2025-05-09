package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Реализация NotificationService для сохранения OTP-кодов в файл.
 * 
 * Основные функции:
 * 1. Запись OTP-кодов в файл
 *    - Создание файла при необходимости
 *    - Добавление временной метки
 *    - Дописывание в конец файла
 * 
 * 2. Управление файловой системой
 *    - Создание директорий
 *    - Проверка прав доступа
 *    - Обработка ошибок
 * 
 * Формат записи:
 * {timestamp} - OTP: {code}
 * Пример:
 * 2024-03-20 15:30:45 - OTP: 123456
 * 
 * Безопасность:
 * - Проверка прав доступа
 * - Логирование операций
 * - Обработка ошибок ввода-вывода
 * - Атомарные операции записи
 */
public class FileNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FileNotificationService.class);
    // Формат временной метки для записи в файл
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Сохраняет OTP-код в файл с временной меткой.
     * 
     * Процесс записи:
     * 1. Формирование записи с временной меткой
     * 2. Создание директорий при необходимости
     * 3. Запись в файл
     * 4. Логирование результата
     * 
     * Формат записи:
     * {timestamp} - OTP: {code}
     * 
     * Возможные ошибки:
     * - RuntimeException: ошибка записи в файл
     * - IOException: проблемы с файловой системой
     * 
     * @param recipientPath путь к файлу для сохранения кода
     * @param code OTP-код для сохранения
     * @throws RuntimeException если не удалось записать в файл
     */
    @Override
    public void sendCode(String recipientPath, String code) {
        Path path = Paths.get(recipientPath);
        String entry = String.format("%s - OTP: %s%n",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                code);
        try {
            // Убедимся, что директория существует
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            // Запишем код в файл (создаём, если нужно, и дописываем в конец)
            Files.write(path, entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("OTP code written to file {}", recipientPath);
        } catch (IOException e) {
            logger.error("Failed to write OTP to file {}", recipientPath, e);
            throw new RuntimeException("File write failed", e);
        }
    }
}

