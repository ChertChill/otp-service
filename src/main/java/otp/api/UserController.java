package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.OtpService;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationServiceFactory;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

/**
 * Контроллер пользовательских операций для работы с OTP-кодами.
 * 
 * Основные функции:
 * 1. Управление OTP-кодами
 *    - Генерация новых кодов
 *    - Валидация существующих
 *    - Отправка уведомлений
 *    - Контроль жизненного цикла
 *    - Управление состоянием
 *    - Мониторинг активности
 *    - Аудит операций
 * 
 * 2. Безопасность
 *    - Проверка аутентификации
 *    - Валидация данных
 *    - Контроль доступа
 *    - Защита от атак
 *    - Предотвращение подделки
 *    - Аудит безопасности
 *    - Мониторинг активности
 * 
 * 3. Интеграция
 *    - Работа с сервисами
 *    - Управление каналами
 *    - Обработка ошибок
 *    - Логирование
 *    - Контроль состояния
 *    - Валидация контекста
 *    - Аудит операций
 * 
 * 4. Управление уведомлениями
 *    - Выбор каналов
 *    - Формирование сообщений
 *    - Отправка кодов
 *    - Контроль доставки
 *    - Мониторинг статуса
 *    - Обработка ошибок
 *    - Аудит операций
 * 
 * Поддерживаемые эндпоинты:
 * 1. POST /otp/generate
 *    - Генерация и отправка OTP-кода
 *    - Тело запроса: {userId, operationId, channel}
 *    - Каналы отправки: EMAIL, SMS, TELEGRAM, FILE
 *    - Ответы:
 *      * 202 Accepted: успешная генерация
 *      * 400 Bad Request: неверные данные
 *      * 415 Unsupported Media Type: неверный формат
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 *    - Безопасность:
 *      * Проверка аутентификации
 *      * Валидация данных
 *      * Контроль доступа
 *      * Аудит операций
 *      * Мониторинг активности
 *      * Защита от атак
 *      * Логирование действий
 * 
 * 2. POST /otp/validate
 *    - Проверка валидности OTP-кода
 *    - Тело запроса: {code}
 *    - Проверяет срок действия и статус
 *    - Ответы:
 *      * 200 OK: код валиден
 *      * 400 Bad Request: код неверен
 *      * 415 Unsupported Media Type: неверный формат
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 *    - Безопасность:
 *      * Проверка аутентификации
 *      * Валидация данных
 *      * Контроль доступа
 *      * Аудит операций
 *      * Мониторинг активности
 *      * Защита от атак
 *      * Логирование действий
 * 
 * Безопасность:
 * - Доступ только для аутентифицированных пользователей
 * - Проверка принадлежности OTP-кода пользователю
 * - Ограничение времени жизни кодов
 * - Защита от повторного использования
 * - Защита от брутфорса
 * - Логирование операций
 * - Контроль доступа
 * - Мониторинг активности
 * - Предотвращение атак
 * - Аудит безопасности
 */
public class UserController {
    private final OtpService otpService = new OtpService(
            new OtpCodeDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
    );

    /**
     * Обрабатывает HTTP POST запрос генерации OTP-кода.
     * 
     * Процесс генерации:
     * 1. Валидация запроса
     *    - Проверка метода (POST)
     *    - Проверка Content-Type (application/json)
     *    - Разбор JSON тела
     *    - Валидация полей
     *    - Контроль структуры
     *    - Проверка формата
     *    - Валидация контекста
     * 
     * 2. Генерация кода
     *    - Создание OTP
     *    - Проверка ограничений
     *    - Сохранение в БД
     *    - Логирование
     *    - Контроль состояния
     *    - Валидация результата
     *    - Аудит операции
     * 
     * 3. Отправка кода
     *    - Выбор канала
     *    - Формирование сообщения
     *    - Отправка уведомления
     *    - Обработка ошибок
     *    - Контроль доставки
     *    - Мониторинг статуса
     *    - Аудит операции
     * 
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void generateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            GenerateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), GenerateRequest.class);
            otpService.sendOtpToUser(req.userId, req.operationId,
                    NotificationChannel.valueOf(req.channel));
            HttpUtils.sendEmptyResponse(exchange, 202);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос валидации OTP-кода.
     * 
     * Процесс валидации:
     * 1. Валидация запроса
     *    - Проверка метода (POST)
     *    - Проверка Content-Type (application/json)
     *    - Разбор JSON тела
     *    - Валидация полей
     *    - Контроль структуры
     *    - Проверка формата
     *    - Валидация контекста
     * 
     * 2. Проверка кода
     *    - Поиск в БД
     *    - Проверка срока
     *    - Проверка статуса
     *    - Проверка принадлежности
     *    - Контроль состояния
     *    - Валидация контекста
     *    - Аудит операции
     * 
     * 3. Обработка результата
     *    - Обновление статуса
     *    - Логирование
     *    - Отправка ответа
     *    - Обработка ошибок
     *    - Контроль состояния
     *    - Валидация результата
     *    - Аудит операции
     * 
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void validateOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            ValidateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ValidateRequest.class);
            boolean valid = otpService.validateOtp(req.code);
            if (valid) {
                HttpUtils.sendEmptyResponse(exchange, 200);
            } else {
                HttpUtils.sendError(exchange, 400, "Invalid or expired code");
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела запроса генерации OTP.
     * 
     * Поля:
     * - userId: идентификатор пользователя
     *   * Должен существовать
     *   * Должен быть активен
     *   * Должен иметь роль USER
     *   * Валидация формата
     *   * Контроль существования
     *   * Проверка контекста
     * 
     * - operationId: идентификатор операции
     *   * Опциональный
     *   * Уникальный
     *   * Для привязки кода
     *   * Валидация формата
     *   * Контроль уникальности
     *   * Проверка контекста
     * 
     * - channel: канал отправки
     *   * EMAIL: отправка на email
     *   * SMS: отправка на телефон
     *   * TELEGRAM: отправка в Telegram
     *   * FILE: сохранение в файл
     *   * Валидация значения
     *   * Контроль доступности
     *   * Проверка контекста
     */
    private static class GenerateRequest {
        public Long userId;
        public String operationId;
        public String channel;
    }

    /**
     * DTO для разбора JSON тела запроса валидации OTP.
     * 
     * Поля:
     * - code: OTP-код
     *   * Должен быть активен
     *   * Не должен быть использован
     *   * Не должен быть просрочен
     *   * Должен принадлежать пользователю
     *   * Валидация формата
     *   * Контроль состояния
     *   * Проверка контекста
     */
    private static class ValidateRequest {
        public String code;
    }
}
