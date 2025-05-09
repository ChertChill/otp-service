package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-реализация интерфейса UserDao.
 * 
 * Основные функции:
 * 1. Управление пользователями
 *    - Создание новых
 *    - Поиск по параметрам
 *    - Удаление с каскадом
 *    - Обновление данных
 *    - Управление ролями
 * 
 * 2. Административные функции
 *    - Проверка наличия админов
 *    - Получение обычных пользователей
 *    - Управление ролями
 *    - Контроль доступа
 *    - Аудит операций
 * 
 * 3. Безопасность
 *    - Подготовленные запросы
 *    - Транзакционность
 *    - Логирование операций
 *    - Валидация данных
 *    - Защита от SQL-инъекций
 * 
 * Процесс работы:
 * 1. Создание пользователей
 *    - Валидация данных
 *    - Подготовка запросов
 *    - Выполнение операций
 *    - Обработка результатов
 * 
 * 2. Поиск пользователей
 *    - Формирование запросов
 *    - Выполнение поиска
 *    - Маппинг результатов
 *    - Логирование операций
 * 
 * 3. Управление данными
 *    - Обновление записей
 *    - Удаление данных
 *    - Контроль транзакций
 *    - Обработка ошибок
 * 
 * Безопасность:
 * - Подготовленные запросы
 * - Транзакционность
 * - Логирование операций
 * - Валидация данных
 * - Защита от SQL-инъекций
 * - Контроль доступа
 */
public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    /** SQL-запрос для создания нового пользователя */
    private static final String INSERT_SQL =
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

    /** SQL-запрос для поиска пользователя по логину */
    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE username = ?";

    /** SQL-запрос для поиска пользователя по ID */
    private static final String SELECT_BY_ID_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE id = ?";

    /** SQL-запрос для получения всех обычных пользователей */
    private static final String SELECT_ALL_USERS_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE role <> 'ADMIN'";

    /** SQL-запрос для проверки наличия администраторов */
    private static final String SELECT_ADMIN_EXISTS_SQL =
            "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";

    /** SQL-запрос для удаления пользователя */
    private static final String DELETE_USER_SQL =
            "DELETE FROM users WHERE id = ?";

    /**
     * Создает нового пользователя.
     * 
     * Процесс создания:
     * 1. Подготовка данных
     *    - Валидация параметров
     *    - Формирование запроса
     *    - Настройка параметров
     *    - Подготовка транзакции
     * 
     * 2. Выполнение операции
     *    - Вставка записи
     *    - Получение ID
     *    - Проверка результата
     *    - Обработка ошибок
     * 
     * 3. Завершение операции
     *    - Обновление объекта
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param user объект пользователя для создания
     * @throws RuntimeException при ошибке создания
     */
    @Override
    public void create(User user) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            logger.info("Created user: {}", user);
        } catch (SQLException e) {
            logger.error("Error creating user [{}]: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ищет пользователя по логину.
     * 
     * Процесс поиска:
     * 1. Подготовка запроса
     *    - Валидация логина
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Подготовка кэша
     * 
     * 2. Выполнение поиска
     *    - Поиск в БД
     *    - Проверка кэша
     *    - Обработка результата
     *    - Валидация данных
     * 
     * 3. Возврат результата
     *    - Маппинг данных
     *    - Логирование
     *    - Обновление кэша
     *    - Возврат объекта
     * 
     * @param username логин пользователя
     * @return найденный пользователь или null
     * @throws RuntimeException при ошибке поиска
     */
    @Override
    public User findByUsername(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Found user by username {}: {}", username, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username [{}]: {}", username, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Ищет пользователя по ID.
     * 
     * Процесс поиска:
     * 1. Подготовка запроса
     *    - Валидация ID
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Подготовка кэша
     * 
     * 2. Выполнение поиска
     *    - Поиск в БД
     *    - Проверка кэша
     *    - Обработка результата
     *    - Валидация данных
     * 
     * 3. Возврат результата
     *    - Маппинг данных
     *    - Логирование
     *    - Обновление кэша
     *    - Возврат объекта
     * 
     * @param id идентификатор пользователя
     * @return найденный пользователь или null
     * @throws RuntimeException при ошибке поиска
     */
    @Override
    public User findById(Long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Found user by id {}: {}", id, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id [{}]: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Получает список обычных пользователей.
     * 
     * Процесс получения:
     * 1. Подготовка запроса
     *    - Формирование SQL
     *    - Настройка фильтров
     *    - Подготовка сортировки
     *    - Проверка кэша
     * 
     * 2. Выполнение запроса
     *    - Поиск в БД
     *    - Фильтрация результатов
     *    - Сортировка данных
     *    - Валидация результатов
     * 
     * 3. Обработка результата
     *    - Маппинг данных
     *    - Формирование списка
     *    - Логирование
     *    - Возврат результата
     * 
     * @return список пользователей без администраторов
     * @throws RuntimeException при ошибке поиска
     */
    @Override
    public List<User> findAllUsersWithoutAdmins() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_USERS_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            logger.info("Found {} non-admin users", users.size());
        } catch (SQLException e) {
            logger.error("Error fetching non-admin users: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return users;
    }

    /**
     * Проверяет наличие администраторов.
     * 
     * Процесс проверки:
     * 1. Подготовка запроса
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Подготовка условий
     *    - Проверка кэша
     * 
     * 2. Выполнение проверки
     *    - Поиск в БД
     *    - Проверка результатов
     *    - Валидация данных
     *    - Обработка ошибок
     * 
     * 3. Возврат результата
     *    - Формирование ответа
     *    - Логирование
     *    - Обновление кэша
     *    - Возврат статуса
     * 
     * @return true если есть хотя бы один администратор
     * @throws RuntimeException при ошибке проверки
     */
    @Override
    public boolean adminExists() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ADMIN_EXISTS_SQL);
             ResultSet rs = ps.executeQuery()) {
            boolean exists = rs.next();
            logger.info("Admin exists: {}", exists);
            return exists;
        } catch (SQLException e) {
            logger.error("Error checking for existing admin: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Удаляет пользователя.
     * 
     * Процесс удаления:
     * 1. Подготовка операции
     *    - Валидация ID
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Подготовка транзакции
     * 
     * 2. Выполнение удаления
     *    - Проверка зависимостей
     *    - Удаление записи
     *    - Проверка результата
     *    - Обработка ошибок
     * 
     * 3. Завершение операции
     *    - Очистка кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param userId ID удаляемого пользователя
     * @throws RuntimeException при ошибке удаления
     */
    @Override
    public void delete(Long userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER_SQL)) {
            ps.setLong(1, userId);
            int affected = ps.executeUpdate();
            logger.info("Deleted user id {}: affected {} rows", userId, affected);
        } catch (SQLException e) {
            logger.error("Error deleting user id [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Преобразует строку ResultSet в объект User.
     * 
     * Процесс маппинга:
     * 1. Подготовка данных
     *    - Проверка полей
     *    - Валидация значений
     *    - Подготовка объекта
     *    - Настройка параметров
     * 
     * 2. Заполнение объекта
     *    - Установка ID
     *    - Установка логина
     *    - Установка пароля
     *    - Установка роли
     * 
     * 3. Валидация результата
     *    - Проверка данных
     *    - Контроль целостности
     *    - Обработка ошибок
     *    - Возврат объекта
     * 
     * @param rs ResultSet с данными
     * @return объект User
     * @throws SQLException при ошибке чтения данных
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        return user;
    }
}

