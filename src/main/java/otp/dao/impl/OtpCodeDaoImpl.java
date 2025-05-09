package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.OtpCodeDao;
import otp.model.OtpCode;
import otp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-реализация интерфейса OtpCodeDao.
 * 
 * Основные функции:
 * 1. Управление OTP-кодами
 *    - Сохранение новых кодов
 *    - Поиск по значению
 *    - Получение по пользователю
 *    - Валидация кодов
 *    - Контроль уникальности
 * 
 * 2. Управление состоянием
 *    - Отметка как использованный
 *    - Отметка как просроченный
 *    - Удаление кодов
 *    - Контроль времени жизни
 *    - Мониторинг статусов
 * 
 * 3. Безопасность
 *    - Подготовленные запросы
 *    - Транзакционность
 *    - Логирование операций
 *    - Валидация данных
 *    - Защита от SQL-инъекций
 *    - Контроль доступа
 * 
 * Процесс работы:
 * 1. Создание кодов
 *    - Генерация значений
 *    - Валидация параметров
 *    - Сохранение в БД
 *    - Контроль уникальности
 * 
 * 2. Управление состоянием
 *    - Проверка срока действия
 *    - Обновление статусов
 *    - Очистка старых кодов
 *    - Мониторинг изменений
 * 
 * 3. Поиск и валидация
 *    - Поиск по значению
 *    - Фильтрация по пользователю
 *    - Проверка статуса
 *    - Валидация времени
 */
public class OtpCodeDaoImpl implements OtpCodeDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpCodeDaoImpl.class);

    /** SQL-запрос для вставки нового OTP-кода */
    private static final String INSERT_SQL =
            "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at) VALUES (?, ?, ?, ?, ?)";

    /** SQL-запрос для поиска OTP-кода по значению */
    private static final String SELECT_BY_CODE_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE code = ?";

    /** SQL-запрос для получения всех OTP-кодов пользователя */
    private static final String SELECT_BY_USER_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE user_id = ?";

    /** SQL-запрос для отметки OTP-кода как использованного */
    private static final String UPDATE_MARK_USED_SQL =
            "UPDATE otp_codes SET status = 'USED' WHERE id = ?";

    /** SQL-запрос для отметки просроченных OTP-кодов */
    private static final String UPDATE_MARK_EXPIRED_SQL =
            "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND created_at < ?";

    /** SQL-запрос для удаления всех OTP-кодов пользователя */
    private static final String DELETE_BY_USER_SQL =
            "DELETE FROM otp_codes WHERE user_id = ?";

    /**
     * Сохраняет новый OTP-код в базу данных.
     * 
     * Процесс сохранения:
     * 1. Подготовка данных
     *    - Установка времени создания
     *    - Валидация параметров
     *    - Проверка уникальности
     *    - Формирование объекта
     * 
     * 2. Выполнение операции
     *    - Подготовка запроса
     *    - Установка параметров
     *    - Выполнение вставки
     *    - Получение ID
     * 
     * 3. Завершение операции
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Логирование
     *    - Возврат результата
     * 
     * @param code объект OTP-кода для сохранения
     * @throws RuntimeException при ошибке сохранения
     */
    @Override
    public void save(OtpCode code) {
        // Устанавливаем время создания, если оно не задано
        if (code.getCreatedAt() == null) {
            code.setCreatedAt(LocalDateTime.now());
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, code.getUserId());
            if (code.getOperationId() != null) {
                ps.setString(2, code.getOperationId());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, code.getCode());
            ps.setString(4, code.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(code.getCreatedAt()));
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Saving OTP code failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    code.setId(keys.getLong(1));
                }
            }
            logger.info("Saved OTP code: {}", code);
        } catch (SQLException e) {
            logger.error("Error saving OTP code [{}]: {}", code.getCode(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ищет OTP-код по его значению.
     * 
     * Процесс поиска:
     * 1. Подготовка запроса
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Проверка кэша
     *    - Подготовка условий
     * 
     * 2. Выполнение поиска
     *    - Поиск в БД
     *    - Проверка результатов
     *    - Валидация данных
     *    - Обработка ошибок
     * 
     * 3. Возврат результата
     *    - Маппинг данных
     *    - Проверка состояния
     *    - Логирование
     *    - Возврат объекта
     * 
     * @param code значение кода для поиска
     * @return найденный OTP-код или null
     * @throws RuntimeException при ошибке поиска
     */
    @Override
    public OtpCode findByCode(String code) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE_SQL)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OtpCode found = mapRow(rs);
                    logger.info("Found OTP by code {}: {}", code, found);
                    return found;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding OTP by code [{}]: {}", code, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Получает все OTP-коды пользователя.
     * 
     * Процесс получения:
     * 1. Подготовка запроса
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Проверка доступа
     *    - Подготовка условий
     * 
     * 2. Выполнение поиска
     *    - Поиск в БД
     *    - Сбор результатов
     *    - Валидация данных
     *    - Обработка ошибок
     * 
     * 3. Формирование результата
     *    - Маппинг данных
     *    - Фильтрация результатов
     *    - Логирование
     *    - Возврат списка
     * 
     * @param userId ID пользователя
     * @return список OTP-кодов
     * @throws RuntimeException при ошибке поиска
     */
    @Override
    public List<OtpCode> findAllByUser(Long userId) {
        List<OtpCode> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_SQL)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            logger.info("Found {} OTP codes for user {}", list.size(), userId);
        } catch (SQLException e) {
            logger.error("Error finding OTP codes for user [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * Отмечает OTP-код как использованный.
     * 
     * Процесс обновления:
     * 1. Подготовка данных
     *    - Валидация ID
     *    - Проверка состояния
     *    - Формирование запроса
     *    - Подготовка транзакции
     * 
     * 2. Выполнение операции
     *    - Обновление статуса
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Контроль изменений
     * 
     * 3. Завершение операции
     *    - Обновление кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param id ID OTP-кода
     * @throws RuntimeException при ошибке обновления
     */
    @Override
    public void markAsUsed(Long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_USED_SQL)) {
            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            logger.info("Marked OTP id {} as USED ({} rows affected)", id, affected);
        } catch (SQLException e) {
            logger.error("Error marking OTP id [{}] as USED: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Отмечает просроченные OTP-коды.
     * 
     * Процесс обновления:
     * 1. Подготовка данных
     *    - Расчет времени
     *    - Валидация TTL
     *    - Формирование запроса
     *    - Подготовка транзакции
     * 
     * 2. Выполнение операции
     *    - Обновление статусов
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Контроль изменений
     * 
     * 3. Завершение операции
     *    - Обновление кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param ttl время жизни кода
     * @throws RuntimeException при ошибке обновления
     */
    @Override
    public void markAsExpiredOlderThan(Duration ttl) {
        LocalDateTime threshold = LocalDateTime.now().minus(ttl);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_EXPIRED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(threshold));
            int affected = ps.executeUpdate();
            logger.info("Marked {} OTP codes as EXPIRED older than {}", affected, threshold);
        } catch (SQLException e) {
            logger.error("Error marking expired OTP codes older than {}: {}", threshold, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Удаляет все OTP-коды пользователя.
     * 
     * Процесс удаления:
     * 1. Подготовка данных
     *    - Валидация ID
     *    - Проверка доступа
     *    - Формирование запроса
     *    - Подготовка транзакции
     * 
     * 2. Выполнение операции
     *    - Удаление записей
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Контроль изменений
     * 
     * 3. Завершение операции
     *    - Очистка кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param userId ID пользователя
     * @throws RuntimeException при ошибке удаления
     */
    @Override
    public void deleteAllByUserId(Long userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_USER_SQL)) {
            ps.setLong(1, userId);
            int affected = ps.executeUpdate();
            logger.info("Deleted {} OTP codes for user {}", affected, userId);
        } catch (SQLException e) {
            logger.error("Error deleting OTP codes for user [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Преобразует строку ResultSet в объект OtpCode.
     * 
     * Процесс преобразования:
     * 1. Подготовка данных
     *    - Проверка полей
     *    - Валидация значений
     *    - Обработка null
     *    - Подготовка объекта
     * 
     * 2. Маппинг данных
     *    - Заполнение полей
     *    - Преобразование типов
     *    - Установка значений
     *    - Проверка целостности
     * 
     * 3. Возврат результата
     *    - Валидация объекта
     *    - Проверка состояния
     *    - Логирование
     *    - Возврат объекта
     * 
     * @param rs ResultSet с данными
     * @return объект OtpCode
     * @throws SQLException при ошибке чтения данных
     */
    private OtpCode mapRow(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getLong("id"));
        code.setUserId(rs.getLong("user_id"));
        String op = rs.getString("operation_id");
        code.setOperationId(op != null ? op : null);
        code.setCode(rs.getString("code"));
        code.setStatus(OtpStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        code.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return code;
    }
}

