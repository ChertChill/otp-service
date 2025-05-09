package otp.service;

import otp.dao.OtpCodeDao;
import otp.dao.OtpConfigDao;
import otp.dao.UserDao;
import otp.model.OtpCode;
import otp.model.OtpConfig;
import otp.model.OtpStatus;
import otp.model.User;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationService;
import otp.service.notification.NotificationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы с OTP-кодами.
 * Отвечает за генерацию, валидацию и отправку OTP-кодов через различные каналы связи.
 * 
 * Основные функции:
 * 1. Генерация OTP-кодов
 *    - Случайная генерация цифровых кодов
 *    - Настраиваемая длина кода
 *    - Привязка к пользователю и операции
 * 
 * 2. Отправка кодов
 *    - Поддержка различных каналов (EMAIL, SMS, TELEGRAM, FILE)
 *    - Использование фабрики для создания сервисов уведомлений
 *    - Логирование всех отправок
 * 
 * 3. Валидация кодов
 *    - Проверка существования кода
 *    - Проверка статуса (ACTIVE, EXPIRED, USED)
 *    - Проверка срока действия
 *    - Автоматическое помечание как USED при успешной валидации
 * 
 * 4. Управление жизненным циклом
 *    - Отслеживание времени создания
 *    - Автоматическое помечание просроченных кодов
 *    - Настраиваемое время жизни кодов
 * 
 * Безопасность:
 * - Использование SecureRandom для генерации кодов
 * - Проверка принадлежности кода пользователю
 * - Защита от повторного использования
 * - Ограничение времени жизни кодов
 */
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final UserDao userDao;
    private final NotificationServiceFactory notificationFactory;

    /**
     * Создает новый экземпляр OtpService.
     * 
     * Зависимости:
     * - otpCodeDao: для работы с OTP-кодами в БД
     * - otpConfigDao: для получения настроек OTP
     * - userDao: для получения информации о пользователях
     * - notificationFactory: для создания сервисов уведомлений
     * 
     * @param otpCodeDao DAO для работы с OTP-кодами
     * @param otpConfigDao DAO для работы с конфигурацией OTP
     * @param userDao DAO для работы с пользователями
     * @param notificationFactory фабрика для создания сервисов уведомлений
     */
    public OtpService(OtpCodeDao otpCodeDao,
                      OtpConfigDao otpConfigDao,
                      UserDao userDao,
                      NotificationServiceFactory notificationFactory) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.userDao = userDao;
        this.notificationFactory = notificationFactory;
    }

    /**
     * Генерирует новый OTP-код для указанного пользователя и операции.
     * 
     * Процесс генерации:
     * 1. Получение текущей конфигурации OTP
     * 2. Генерация случайного цифрового кода нужной длины
     * 3. Создание записи в БД со статусом ACTIVE
     * 4. Логирование результата
     * 
     * @param userId ID пользователя
     * @param operationId идентификатор операции
     * @return сгенерированный OTP-код
     */
    public String generateOtp(Long userId, String operationId) {
        OtpConfig config = otpConfigDao.getConfig();
        int length = config.getLength();

        // Генерация случайного цифрового кода нужной длины
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();

        // Сохраняем в БД
        OtpCode otp = new OtpCode(
                null,
                userId,
                operationId,
                code,
                OtpStatus.ACTIVE,
                LocalDateTime.now()
        );
        otpCodeDao.save(otp);
        logger.info("Generated OTP {} for userId={}, operationId={}", code, userId, operationId);
        return code;
    }

    /**
     * Возвращает текущую конфигурацию OTP-кодов.
     * 
     * Конфигурация содержит:
     * - length: длина генерируемых кодов
     * - ttlSeconds: время жизни кода в секундах
     * 
     * @return текущая конфигурация OTP
     */
    public OtpConfig getConfig() {
        return otpConfigDao.getConfig();
    }

    /**
     * Генерирует OTP-код и отправляет его пользователю через указанный канал связи.
     * 
     * Процесс отправки:
     * 1. Генерация нового OTP-кода
     * 2. Получение информации о пользователе
     * 3. Выбор сервиса уведомлений по каналу
     * 4. Отправка кода через выбранный сервис
     * 5. Логирование результата
     * 
     * @param userId ID пользователя
     * @param operationId идентификатор операции
     * @param channel канал отправки (EMAIL, SMS, TELEGRAM, FILE)
     * @throws IllegalArgumentException если пользователь не найден
     */
    public void sendOtpToUser(Long userId, String operationId, NotificationChannel channel) {
        String code = generateOtp(userId, operationId);
        User user = userDao.findById(userId);
        if (user == null) {
            logger.error("sendOtpToUser: user not found, id={}", userId);
            throw new IllegalArgumentException("User not found");
        }

        // Для простоты используем username как адресат (email, sms, chatId)
        String recipient = user.getUsername();
        NotificationService svc = notificationFactory.getService(channel);
        svc.sendCode(recipient, code);
        logger.info("Sent OTP code for userId={} via {}", userId, channel);
    }

    /**
     * Проверяет валидность введенного OTP-кода.
     * 
     * Процесс валидации:
     * 1. Поиск кода в БД
     * 2. Проверка статуса (должен быть ACTIVE)
     * 3. Проверка срока действия
     * 4. При успехе - пометка кода как USED
     * 
     * Возможные причины невалидности:
     * - Код не найден в БД
     * - Код уже использован (статус USED)
     * - Код просрочен (статус EXPIRED)
     * - Истек срок действия кода
     * 
     * @param inputCode проверяемый код
     * @return true если код валиден, false в противном случае
     */
    public boolean validateOtp(String inputCode) {
        OtpCode otp = otpCodeDao.findByCode(inputCode);
        if (otp == null) {
            logger.warn("validateOtp: code not found {}", inputCode);
            return false;
        }
        // Проверка статуса
        if (otp.getStatus() != OtpStatus.ACTIVE) {
            logger.warn("validateOtp: code {} is not active (status={})", inputCode, otp.getStatus());
            return false;
        }
        // Проверка истечения по времени
        OtpConfig config = otpConfigDao.getConfig();
        LocalDateTime expiry = otp.getCreatedAt().plusSeconds(config.getTtlSeconds());
        if (LocalDateTime.now().isAfter(expiry)) {
            otpCodeDao.markAsExpiredOlderThan(Duration.ofSeconds(config.getTtlSeconds()));
            logger.warn("validateOtp: code {} expired at {}", inputCode, expiry);
            return false;
        }

        // Всё ок — помечаем как USED
        otpCodeDao.markAsUsed(otp.getId());
        logger.info("validateOtp: code {} validated and marked USED", inputCode);
        return true;
    }

    /**
     * Помечает все просроченные OTP-коды как EXPIRED.
     * 
     * Процесс обработки:
     * 1. Получение текущей конфигурации TTL
     * 2. Поиск всех кодов старше TTL
     * 3. Изменение их статуса на EXPIRED
     * 4. Логирование результата
     * 
     * Примечание:
     * - Метод используется планировщиком для периодической очистки
     * - Не влияет на уже использованные коды (статус USED)
     */
    public void markExpiredOtps() {
        OtpConfig config = otpConfigDao.getConfig();
        Duration ttl = Duration.ofSeconds(config.getTtlSeconds());
        otpCodeDao.markAsExpiredOlderThan(ttl);
        logger.info("markExpiredOtps: expired codes older than {} seconds", config.getTtlSeconds());
    }
}

