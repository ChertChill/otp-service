package otp.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import otp.model.UserRole;

/**
 * Диспетчер HTTP-запросов.
 * 
 * Основные функции:
 * 1. Маршрутизация запросов
 *    - Регистрация контекстов
 *    - Привязка обработчиков
 *    - Настройка фильтров
 *    - Обработка методов
 *    - Управление маршрутами
 *    - Контроль доступа
 *    - Валидация запросов
 * 
 * 2. Управление безопасностью
 *    - Фильтрация запросов
 *    - Проверка ролей
 *    - Валидация токенов
 *    - Защита маршрутов
 *    - Контроль доступа
 *    - Предотвращение атак
 *    - Аудит безопасности
 * 
 * 3. Интеграция компонентов
 *    - Связь с контроллерами
 *    - Настройка фильтров
 *    - Обработка ошибок
 *    - Логирование
 *    - Мониторинг
 *    - Управление состоянием
 *    - Контроль зависимостей
 * 
 * 4. Обработка запросов
 *    - Валидация входных данных
 *    - Маршрутизация
 *    - Обработка ошибок
 *    - Форматирование ответов
 *    - Контроль состояния
 *    - Логирование действий
 *    - Аудит операций
 * 
 * Архитектура маршрутизации:
 * 1. Публичные маршруты (не требуют аутентификации):
 *    - POST /register
 *      * Регистрация нового пользователя
 *      * Тело: {username, password, email}
 *      * Ответ: 201 Created или 400 Bad Request
 *      * Ограничения: уникальный username
 *      * Валидация: формат данных
 *      * Безопасность: защита от спама
 *      * Логирование: создание аккаунта
 * 
 *    - POST /login
 *      * Аутентификация пользователя
 *      * Тело: {username, password}
 *      * Ответ: 200 OK с JWT токеном или 401 Unauthorized
 *      * Ограничения: защита от брутфорса
 *      * Валидация: учетные данные
 *      * Безопасность: ограничение попыток
 *      * Логирование: вход в систему
 * 
 * 2. Маршруты для пользователей (требуют роль USER):
 *    - POST /otp/generate
 *      * Генерация нового OTP-кода
 *      * Тело: {userId, operationId, channel}
 *      * Ответ: 202 Accepted или 400 Bad Request
 *      * Ограничения: лимит генерации
 *      * Валидация: параметры запроса
 *      * Безопасность: контроль частоты
 *      * Логирование: создание кода
 * 
 *    - POST /otp/validate
 *      * Проверка OTP-кода
 *      * Тело: {code}
 *      * Ответ: 200 OK или 400 Bad Request
 *      * Ограничения: срок действия
 *      * Валидация: формат кода
 *      * Безопасность: защита от перебора
 *      * Логирование: проверка кода
 * 
 * 3. Маршруты для администраторов (требуют роль ADMIN):
 *    - PATCH /admin/config
 *      * Обновление конфигурации OTP
 *      * Тело: {length, ttlSeconds}
 *      * Ответ: 204 No Content или 400 Bad Request
 *      * Ограничения: валидные параметры
 *      * Валидация: значения параметров
 *      * Безопасность: контроль доступа
 *      * Логирование: изменение конфигурации
 * 
 *    - GET /admin/users
 *      * Получение списка пользователей
 *      * Ответ: 200 OK с JSON-массивом
 *      * Ограничения: только обычные пользователи
 *      * Валидация: параметры запроса
 *      * Безопасность: контроль доступа
 *      * Логирование: просмотр списка
 * 
 *    - DELETE /admin/users/{id}
 *      * Удаление пользователя
 *      * Ответ: 204 No Content или 404 Not Found
 *      * Ограничения: нельзя удалить админа
 *      * Валидация: существование пользователя
 *      * Безопасность: контроль доступа
 *      * Логирование: удаление пользователя
 * 
 * Безопасность:
 * - Все маршруты (кроме публичных) защищены фильтром аутентификации
 * - Для каждого защищенного маршрута указана минимальная требуемая роль
 * - Фильтры проверяют JWT токен и роль пользователя
 * - Защита от несанкционированного доступа к административным функциям
 * - Валидация входных данных на уровне контроллеров
 * - Защита от CSRF и XSS атак
 * - Ограничение частоты запросов
 * - Логирование всех действий
 * - Аудит безопасности
 * - Мониторинг активности
 * - Предотвращение атак
 * - Контроль доступа
 * - Защита данных
 */
public class Dispatcher {
    private final AuthController authController = new AuthController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();

    /**
     * Регистрирует все маршруты в HTTP-сервере и настраивает фильтры безопасности.
     * 
     * Процесс регистрации:
     * 1. Создание контекстов
     *    - Определение путей
     *    - Привязка обработчиков
     *    - Настройка фильтров
     *    - Обработка методов
     *    - Валидация маршрутов
     *    - Контроль доступа
     *    - Логирование
     * 
     * 2. Настройка безопасности
     *    - Публичные маршруты без фильтров
     *    - USER маршруты с базовой защитой
     *    - ADMIN маршруты с расширенной защитой
     *    - Обработка ошибок
     *    - Контроль доступа
     *    - Аудит безопасности
     *    - Мониторинг
     * 
     * 3. Интеграция компонентов
     *    - Связь с контроллерами
     *    - Настройка фильтров
     *    - Обработка методов
     *    - Логирование
     *    - Управление состоянием
     *    - Контроль зависимостей
     *    - Валидация конфигурации
     * 
     * 4. Обработка ошибок
     *    - Неверные методы
     *    - Неверные пути
     *    - Ошибки доступа
     *    - Внутренние ошибки
     *    - Валидация запросов
     *    - Форматирование ответов
     *    - Логирование ошибок
     * 
     * @param server экземпляр HttpServer для регистрации маршрутов
     * @throws IllegalArgumentException если server равен null
     */
    public void registerRoutes(HttpServer server) {
        // Публичные маршруты
        server.createContext("/register", authController::handleRegister);
        server.createContext("/login",    authController::handleLogin);

        // Маршруты для пользователей (роль USER)
        HttpContext genCtx = server.createContext("/otp/generate", userController::generateOtp);
        genCtx.getFilters().add(new AuthFilter(UserRole.USER));
        HttpContext valCtx = server.createContext("/otp/validate", userController::validateOtp);
        valCtx.getFilters().add(new AuthFilter(UserRole.USER));

        // Маршруты для администратора (роль ADMIN)
        HttpContext configCtx = server.createContext("/admin/config", adminController::updateOtpConfig);
        configCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
        HttpContext usersCtx = server.createContext("/admin/users", exchange -> {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                adminController.listUsers(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                adminController.deleteUser(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });
        usersCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
    }
}
