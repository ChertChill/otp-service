package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Контроллер административных операций для управления системой OTP.
 * 
 * Основные функции:
 * 1. Управление конфигурацией
 *    - Настройка параметров OTP
 *    - Контроль безопасности
 *    - Валидация настроек
 *    - Аудит изменений
 * 
 * 2. Управление пользователями
 *    - Просмотр списка
 *    - Удаление аккаунтов
 *    - Контроль доступа
 *    - Мониторинг активности
 * 
 * 3. Безопасность
 *    - Проверка ролей
 *    - Валидация данных
 *    - Защита от атак
 *    - Логирование действий
 * 
 * Поддерживаемые эндпоинты:
 * 1. PATCH /admin/config
 *    - Изменение параметров OTP-кодов
 *    - Тело запроса: {length, ttlSeconds}
 *    - Ограничения: length >= 4, ttlSeconds >= 60
 *    - Ответы:
 *      * 204 No Content: успешное обновление
 *      * 400 Bad Request: неверные параметры
 *      * 415 Unsupported Media Type: неверный формат
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 * 
 * 2. GET /admin/users
 *    - Получение списка обычных пользователей
 *    - Исключает администраторов
 *    - Возвращает массив пользователей в JSON
 *    - Ответы:
 *      * 200 OK: список пользователей
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 * 
 * 3. DELETE /admin/users/{id}
 *    - Удаление пользователя по ID
 *    - Каскадное удаление связанных OTP-кодов
 *    - ID передается в пути запроса
 *    - Ответы:
 *      * 204 No Content: успешное удаление
 *      * 400 Bad Request: неверный ID
 *      * 404 Not Found: пользователь не найден
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 * 
 * Безопасность:
 * - Доступ только для администраторов
 * - Валидация входных параметров
 * - Защита от удаления администраторов
 * - Проверка существования пользователей
 * - Аудит всех операций
 * - Защита от инъекций
 */
public class AdminController {
    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );

    /**
     * Обрабатывает HTTP PATCH запрос на изменение конфигурации OTP.
     * 
     * Процесс обновления:
     * 1. Валидация запроса
     *    - Проверка метода (PATCH)
     *    - Проверка Content-Type (application/json)
     *    - Разбор JSON тела
     *    - Валидация полей
     * 
     * 2. Проверка параметров
     *    - Длина кода >= 4
     *    - Время жизни >= 60 сек
     *    - Проверка ограничений
     *    - Валидация значений
     * 
     * 3. Обновление конфигурации
     *    - Сохранение в БД
     *    - Применение изменений
     *    - Логирование
     *    - Обработка ошибок
     * 
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void updateOtpConfig(HttpExchange exchange) throws IOException {
        if (!"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("application/json")) {
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ConfigRequest.class);
            adminService.updateOtpConfig(req.length, req.ttlSeconds);
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP GET запрос для получения списка пользователей.
     * 
     * Процесс получения:
     * 1. Валидация запроса
     *    - Проверка метода (GET)
     *    - Проверка прав доступа
     *    - Валидация параметров
     *    - Подготовка запроса
     * 
     * 2. Получение данных
     *    - Запрос к БД
     *    - Фильтрация администраторов
     *    - Форматирование данных
     *    - Подготовка ответа
     * 
     * 3. Отправка ответа
     *    - Сериализация в JSON
     *    - Установка заголовков
     *    - Отправка данных
     *    - Обработка ошибок
     * 
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void listUsers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            List<User> users = adminService.getAllUsersWithoutAdmins();
            String json = JsonUtil.toJson(users);
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP DELETE запрос на удаление пользователя.
     * 
     * Процесс удаления:
     * 1. Валидация запроса
     *    - Проверка метода (DELETE)
     *    - Извлечение ID из пути
     *    - Проверка формата
     *    - Валидация прав
     * 
     * 2. Проверка пользователя
     *    - Поиск в БД
     *    - Проверка роли
     *    - Проверка активности
     *    - Подготовка к удалению
     * 
     * 3. Удаление данных
     *    - Удаление OTP-кодов
     *    - Удаление пользователя
     *    - Очистка зависимостей
     *    - Логирование
     * 
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void deleteUser(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            URI uri = exchange.getRequestURI();
            String[] segments = uri.getPath().split("/");
            Long id = Long.valueOf(segments[segments.length - 1]);
            adminService.deleteUserAndCodes(id);
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (NumberFormatException e) {
            HttpUtils.sendError(exchange, 400, "Invalid user ID");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела запроса обновления конфигурации OTP.
     * 
     * Поля:
     * - length: длина генерируемых OTP-кодов
     *   * Минимум 4 символа
     *   * Максимум 10 символов
     *   * Только цифры
     *   * Рекомендуется 6
     * 
     * - ttlSeconds: время жизни кода
     *   * Минимум 60 секунд
     *   * Максимум 3600 секунд
     *   * В секундах
     *   * Рекомендуется 300
     */
    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}
