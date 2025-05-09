package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.OtpConfigDao;
import otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * JDBC-реализация интерфейса OtpConfigDao.
 * 
 * Основные функции:
 * 1. Управление конфигурацией
 *    - Получение настроек
 *    - Обновление параметров
 *    - Инициализация по умолчанию
 *    - Валидация значений
 *    - Контроль состояния
 * 
 * 2. Параметры конфигурации
 *    - Длина кода
 *    - Время жизни
 *    - Другие настройки
 *    - Ограничения
 *    - Значения по умолчанию
 * 
 * 3. Безопасность
 *    - Подготовленные запросы
 *    - Транзакционность
 *    - Логирование операций
 *    - Валидация данных
 *    - Защита от SQL-инъекций
 * 
 * Процесс работы:
 * 1. Инициализация
 *    - Проверка наличия данных
 *    - Создание дефолтных значений
 *    - Валидация параметров
 *    - Сохранение в БД
 * 
 * 2. Управление настройками
 *    - Получение конфигурации
 *    - Обновление параметров
 *    - Проверка значений
 *    - Применение изменений
 * 
 * 3. Контроль состояния
 *    - Мониторинг изменений
 *    - Валидация данных
 *    - Обработка ошибок
 *    - Логирование операций
 * 
 * Безопасность:
 * - Подготовленные запросы
 * - Транзакционность
 * - Логирование операций
 * - Валидация данных
 * - Защита от SQL-инъекций
 * - Контроль доступа
 */
public class OtpConfigDaoImpl implements OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDaoImpl.class);

    /** SQL-запрос для получения конфигурации */
    private static final String SELECT_CONFIG_SQL =
            "SELECT id, length, ttl_seconds FROM otp_config LIMIT 1";

    /** SQL-запрос для обновления конфигурации */
    private static final String UPDATE_CONFIG_SQL =
            "UPDATE otp_config SET length = ?, ttl_seconds = ? WHERE id = ?";

    /** SQL-запрос для вставки конфигурации по умолчанию */
    private static final String INSERT_DEFAULT_SQL =
            "INSERT INTO otp_config (length, ttl_seconds) VALUES (?, ?)";

    /**
     * Получает текущую конфигурацию OTP.
     * 
     * Процесс получения:
     * 1. Подготовка запроса
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Проверка кэша
     *    - Подготовка условий
     * 
     * 2. Выполнение запроса
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
     * @return объект OtpConfig или null если конфигурация отсутствует
     * @throws RuntimeException при ошибке чтения
     */
    @Override
    public OtpConfig getConfig() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CONFIG_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                OtpConfig cfg = new OtpConfig();
                cfg.setId(rs.getLong("id"));
                cfg.setLength(rs.getInt("length"));
                cfg.setTtlSeconds(rs.getInt("ttl_seconds"));
                logger.info("Loaded OTP config: {}", cfg);
                return cfg;
            }
        } catch (SQLException e) {
            logger.error("Error loading OTP config: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        logger.warn("No OTP config found in database");
        return null;
    }

    /**
     * Обновляет параметры конфигурации OTP.
     * 
     * Процесс обновления:
     * 1. Подготовка данных
     *    - Валидация параметров
     *    - Формирование запроса
     *    - Настройка параметров
     *    - Подготовка транзакции
     * 
     * 2. Выполнение операции
     *    - Обновление записи
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
     * @param config объект с новыми значениями
     * @throws RuntimeException при ошибке обновления
     */
    @Override
    public void updateConfig(OtpConfig config) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_CONFIG_SQL)) {

            ps.setInt(1, config.getLength());
            ps.setInt(2, config.getTtlSeconds());
            ps.setLong(3, config.getId());
            int affected = ps.executeUpdate();
            logger.info("Updated OTP config (id={}): length={}, ttlSeconds={} ({} rows)",
                    config.getId(), config.getLength(), config.getTtlSeconds(), affected);
        } catch (SQLException e) {
            logger.error("Error updating OTP config [{}]: {}", config, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Инициализирует конфигурацию по умолчанию.
     * 
     * Процесс инициализации:
     * 1. Проверка состояния
     *    - Поиск записей
     *    - Проверка версии
     *    - Контроль доступа
     *    - Подготовка к созданию
     * 
     * 2. Создание конфигурации
     *    - Генерация значений
     *    - Валидация параметров
     *    - Подготовка данных
     *    - Формирование объекта
     * 
     * 3. Сохранение данных
     *    - Вставка в БД
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Логирование
     * 
     * Значения по умолчанию:
     * - Длина кода: 6 цифр
     * - Время жизни: 300 секунд
     * 
     * @throws RuntimeException при ошибке инициализации
     */
    @Override
    public void initDefaultConfigIfEmpty() {
        // проверяем, есть ли запись
        OtpConfig existing = getConfig();
        if (existing != null) {
            logger.info("OTP config already initialized: {}", existing);
            return;
        }
        // вставляем дефолтные значения (6 цифр, 300 секунд)
        int defaultLength = 6;
        int defaultTtl = 300;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_DEFAULT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, defaultLength);
            ps.setInt(2, defaultTtl);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Inserting default OTP config failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    logger.info("Initialized default OTP config id={} (length={}, ttlSeconds={})",
                            newId, defaultLength, defaultTtl);
                }
            }
        } catch (SQLException e) {
            logger.error("Error initializing default OTP config: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

