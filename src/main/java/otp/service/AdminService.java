package otp.service;

import otp.dao.OtpConfigDao;
import otp.dao.OtpCodeDao;
import otp.dao.UserDao;
import otp.model.OtpConfig;
import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Сервис для административных операций.
 * Предоставляет функционал для управления пользователями и настройками OTP.
 * 
 * Основные функции:
 * 1. Управление конфигурацией OTP
 *    - Изменение длины генерируемых кодов
 *    - Настройка времени жизни кодов
 * 
 * 2. Управление пользователями
 *    - Получение списка обычных пользователей
 *    - Удаление пользователей и их данных
 * 
 * Безопасность:
 * - Доступ только для администраторов
 * - Каскадное удаление данных пользователя
 * - Защита от удаления администраторов
 */
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final OtpConfigDao configDao;
    private final UserDao userDao;
    private final OtpCodeDao codeDao;

    /**
     * Создает новый экземпляр AdminService.
     * 
     * Зависимости:
     * - configDao: для работы с конфигурацией OTP
     * - userDao: для управления пользователями
     * - codeDao: для работы с OTP-кодами
     * 
     * @param configDao DAO для работы с конфигурацией OTP
     * @param userDao DAO для работы с пользователями
     * @param codeDao DAO для работы с OTP-кодами
     */
    public AdminService(OtpConfigDao configDao, UserDao userDao, OtpCodeDao codeDao) {
        this.configDao = configDao;
        this.userDao = userDao;
        this.codeDao = codeDao;
    }

    /**
     * Обновляет конфигурацию OTP-кодов.
     * 
     * Процесс обновления:
     * 1. Создание нового объекта конфигурации
     * 2. Сохранение в БД
     * 3. Логирование изменений
     * 
     * Ограничения:
     * - length: минимальная длина 4 символа
     * - ttlSeconds: минимальное время жизни 60 секунд
     * 
     * @param length новая длина генерируемых кодов
     * @param ttlSeconds новое время жизни кодов в секундах
     * @throws IllegalArgumentException если параметры не соответствуют ограничениям
     */
    public void updateOtpConfig(int length, int ttlSeconds) {
        // Создаем объект OtpConfig (id обычно не важен при обновлении)
        OtpConfig cfg = new OtpConfig(1L, length, ttlSeconds);
        configDao.updateConfig(cfg);
        logger.info("OTP config updated: length={}, ttlSeconds={}", length, ttlSeconds);
    }

    /**
     * Возвращает список всех пользователей, исключая администраторов.
     * 
     * Процесс получения:
     * 1. Запрос к БД через userDao
     * 2. Фильтрация администраторов
     * 3. Возврат списка обычных пользователей
     * 
     * @return список пользователей без администраторов
     */
    public List<User> getAllUsersWithoutAdmins() {
        return userDao.findAllUsersWithoutAdmins();
    }

    /**
     * Удаляет пользователя и все связанные с ним OTP-коды.
     * 
     * Процесс удаления:
     * 1. Удаление всех OTP-кодов пользователя
     * 2. Удаление записи пользователя
     * 3. Логирование операции
     * 
     * Примечание:
     * - Операция выполняется в транзакции
     * - Удаление администраторов запрещено
     * 
     * @param userId ID удаляемого пользователя
     * @throws IllegalArgumentException если пользователь не найден или является администратором
     */
    public void deleteUserAndCodes(Long userId) {
        codeDao.deleteAllByUserId(userId);
        userDao.delete(userId);
        logger.info("Deleted user {} and their OTP codes", userId);
    }
}


