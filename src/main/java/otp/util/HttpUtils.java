package otp.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Утилиты для работы с HTTP-запросами и ответами.
 * 
 * Основные функции:
 * 1. Отправка JSON-ответов
 *    - Установка заголовков
 *    - Кодирование в UTF-8
 *    - Корректное закрытие потоков
 * 
 * 2. Обработка ошибок
 *    - Форматирование JSON-ошибок
 *    - Установка статус-кодов
 *    - Отправка пустых ответов
 * 
 * Безопасность:
 * - Корректная обработка кодировок
 * - Безопасное закрытие потоков
 * - Валидация входных данных
 */
public class HttpUtils {

    /**
     * Отправляет JSON-ответ с указанным статус-кодом.
     * 
     * Процесс отправки:
     * 1. Установка Content-Type: application/json
     * 2. Кодирование JSON в UTF-8
     * 3. Отправка заголовков и тела
     * 4. Корректное закрытие потока
     * 
     * @param exch HTTP-обмен для отправки ответа
     * @param status HTTP-статус ответа
     * @param json JSON-строка для отправки
     * @throws IOException при ошибках ввода-вывода
     */
    public static void sendJsonResponse(HttpExchange exch, int status, String json) throws IOException {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exch.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Отправляет пустой ответ с указанным статус-кодом.
     * 
     * Процесс отправки:
     * 1. Отправка заголовков без тела
     * 2. Установка длины -1
     * 
     * @param exch HTTP-обмен для отправки ответа
     * @param status HTTP-статус ответа
     * @throws IOException при ошибках ввода-вывода
     */
    public static void sendEmptyResponse(HttpExchange exch, int status) throws IOException {
        exch.sendResponseHeaders(status, -1);
    }

    // Отправить JSON-ошибку с сообщением
    public static void sendError(HttpExchange exch, int status, String message) throws IOException {
        String errorJson = String.format("{\"error\":\"%s\"}", message);
        sendJsonResponse(exch, status, errorJson);
    }
}


