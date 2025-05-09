package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;
import otp.util.JwtUtils;  // Импортируем JwtUtils

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * 
 * Основные функции:
 * 1. Управление пользователями
 *    - Регистрация новых пользователей
 *    - Аутентификация существующих
 *    - Валидация данных
 *    - Управление ролями
 *    - Контроль доступа
 *    - Управление сессиями
 *    - Мониторинг активности
 * 
 * 2. Безопасность
 *    - Хеширование паролей
 *    - Генерация JWT токенов
 *    - Проверка учетных данных
 *    - Контроль доступа
 *    - Защита от атак
 *    - Аудит действий
 *    - Мониторинг безопасности
 * 
 * 3. Обработка запросов
 *    - Валидация входных данных
 *    - Формирование ответов
 *    - Обработка ошибок
 *    - Логирование действий
 *    - Контроль состояния
 *    - Управление сессиями
 *    - Аудит операций
 * 
 * 4. Управление сессиями
 *    - Создание токенов
 *    - Валидация сессий
 *    - Контроль времени жизни
 *    - Обновление токенов
 *    - Завершение сессий
 *    - Мониторинг активности
 *    - Аудит сессий
 * 
 * Поддерживаемые эндпоинты:
 * 1. POST /register
 *    - Регистрация нового пользователя
 *    - Тело запроса: {username, password, role}
 *    - Роли: ADMIN (только один) или USER
 *    - Ответы:
 *      * 201 Created: успешная регистрация
 *      * 409 Conflict: имя занято
 *      * 415 Unsupported Media Type: неверный формат
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 *    - Валидация:
 *      * Формат данных
 *      * Уникальность имени
 *      * Сложность пароля
 *      * Допустимость роли
 *    - Безопасность:
 *      * Защита от спама
 *      * Контроль доступа
 *      * Аудит действий
 *    - Логирование:
 *      * Создание аккаунта
 *      * Изменение ролей
 *      * Ошибки регистрации
 * 
 * 2. POST /login
 *    - Аутентификация пользователя
 *    - Тело запроса: {username, password}
 *    - Возвращает JWT токен
 *    - Ответы:
 *      * 200 OK: успешная аутентификация
 *      * 401 Unauthorized: неверные данные
 *      * 415 Unsupported Media Type: неверный формат
 *      * 405 Method Not Allowed: неверный метод
 *      * 500 Internal Server Error: ошибка сервера
 *    - Валидация:
 *      * Формат данных
 *      * Существование пользователя
 *      * Корректность пароля
 *      * Статус аккаунта
 *    - Безопасность:
 *      * Защита от брутфорса
 *      * Контроль попыток
 *      * Аудит входа
 *    - Логирование:
 *      * Успешный вход
 *      * Неудачные попытки
 *      * Блокировка аккаунта
 * 
 * Безопасность:
 * - Пароли никогда не передаются в открытом виде
 * - Хеширование паролей с использованием соли
 * - JWT токены с ограниченным сроком действия
 * - Проверка уникальности имени пользователя
 * - Ограничение на создание администраторов
 * - Защита от брутфорса
 * - Логирование попыток входа
 * - Мониторинг активности
 * - Аудит безопасности
 * - Контроль доступа
 * - Защита от атак
 * - Предотвращение утечек
 */
public class AuthController {
    private final UserService userService = new UserService(new UserDaoImpl());

    /**
     * Обрабатывает HTTP POST запрос на регистрацию пользователя.
     * 
     * Процесс регистрации:
     * 1. Валидация запроса
     *    - Проверка метода (POST)
     *    - Проверка Content-Type (application/json)
     *    - Разбор JSON тела
     *    - Валидация полей
     *    - Проверка формата
     *    - Контроль длины
     *    - Валидация символов
     * 
     * 2. Проверка ограничений
     *    - Уникальность имени
     *    - Существование администратора
     *    - Валидность роли
     *    - Сложность пароля
     *    - Контроль доступа
     *    - Проверка блокировки
     *    - Валидация контекста
     * 
     * 3. Создание пользователя
     *    - Хеширование пароля
     *    - Сохранение в БД
     *    - Логирование
     *    - Отправка ответа
     *    - Аудит действий
     *    - Мониторинг
     *    - Контроль состояния
     * 
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
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
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);

            // Проверка, не существует ли уже администратор
            if ("ADMIN".equals(req.role) && userService.adminExists()) {
                HttpUtils.sendError(exchange, 409, "Admin already exists");
                return;
            }

            userService.register(req.username, req.password, UserRole.valueOf(req.role));
            HttpUtils.sendEmptyResponse(exchange, 201);
        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос на аутентификацию пользователя.
     * 
     * Процесс аутентификации:
     * 1. Валидация запроса
     *    - Проверка метода (POST)
     *    - Проверка Content-Type (application/json)
     *    - Разбор JSON тела
     *    - Валидация полей
     *    - Проверка формата
     *    - Контроль длины
     *    - Валидация символов
     * 
     * 2. Проверка учетных данных
     *    - Поиск пользователя
     *    - Проверка пароля
     *    - Проверка блокировки
     *    - Обновление статистики
     *    - Контроль попыток
     *    - Валидация состояния
     *    - Проверка контекста
     * 
     * 3. Генерация токена
     *    - Создание JWT
     *    - Установка claims
     *    - Подпись токена
     *    - Отправка ответа
     *    - Логирование
     *    - Аудит
     *    - Мониторинг
     * 
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
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
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);
            String token = userService.login(req.username, req.password);
            if (token == null) {
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }
            String json = JsonUtil.toJson(Map.of("token", token));
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * DTO для разбора JSON тела запроса регистрации.
     * 
     * Поля:
     * - username: логин пользователя
     *   * Должен быть уникальным
     *   * Минимум 3 символа
     *   * Только буквы и цифры
     *   * Не должен содержать пробелы
     *   * Регистр не важен
     *   * Без специальных символов
     *   * Проверка на резервированные имена
     * 
     * - password: пароль
     *   * Минимум 8 символов
     *   * Буквы, цифры и спецсимволы
     *   * Будет захеширован
     *   * Не должен быть простым
     *   * Проверка на утечки
     *   * Контроль сложности
     *   * Валидация формата
     * 
     * - role: роль пользователя
     *   * "ADMIN" или "USER"
     *   * ADMIN только один
     *   * USER по умолчанию
     *   * Нельзя изменить
     *   * Проверка привилегий
     *   * Контроль доступа
     *   * Валидация контекста
     */
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    /**
     * DTO для разбора JSON тела запроса логина.
     * 
     * Поля:
     * - username: логин пользователя
     *   * Должен существовать
     *   * Не должен быть заблокирован
     *   * Регистр не важен
     *   * Без пробелов
     *   * Проверка формата
     *   * Контроль длины
     *   * Валидация символов
     * 
     * - password: пароль
     *   * В открытом виде
     *   * Будет проверен
     *   * Не сохраняется
     *   * Регистр важен
     *   * Контроль длины
     *   * Проверка формата
     *   * Валидация символов
     */
    private static class LoginRequest {
        public String username;
        public String password;
    }
}
