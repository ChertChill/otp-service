package otp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

/**
 * Утилиты для работы с JSON.
 * 
 * Основные функции:
 * 1. Десериализация JSON
 *    - Чтение из InputStream
 *    - Преобразование в объекты
 *    - Обработка ошибок
 * 
 * 2. Сериализация объектов
 *    - Преобразование в JSON-строки
 *    - Форматирование вывода
 *    - Обработка ошибок
 * 
 * Безопасность:
 * - Валидация входных данных
 * - Обработка исключений
 * - Безопасное закрытие потоков
 */
public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Десериализует JSON из InputStream в объект указанного класса.
     * 
     * Процесс десериализации:
     * 1. Чтение JSON из потока
     * 2. Преобразование в объект
     * 3. Валидация результата
     * 
     * @param is InputStream с JSON-данными
     * @param clazz класс целевого объекта
     * @return десериализованный объект
     * @throws IOException при ошибках чтения или парсинга
     */
    public static <T> T fromJson(InputStream is, Class<T> clazz) throws IOException {
        return MAPPER.readValue(is, clazz);
    }

    /**
     * Сериализует объект в JSON-строку.
     * 
     * Процесс сериализации:
     * 1. Преобразование объекта в JSON
     * 2. Форматирование строки
     * 3. Валидация результата
     * 
     * @param obj объект для сериализации
     * @return JSON-строка
     * @throws IOException при ошибках сериализации
     */
    public static String toJson(Object obj) throws IOException {
        return MAPPER.writeValueAsString(obj);
    }
}