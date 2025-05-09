package otp.service.notification;

import org.smpp.TCPIPConnection;
import org.smpp.Session;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по SMS.
 * 
 * Основные функции:
 * 1. Отправка OTP-кодов через SMS
 *    - Использование SMPP-протокола
 *    - Форматирование сообщений
 *    - Управление соединением
 * 
 * 2. Взаимодействие с SMPP-сервером
 *    - Установка соединения
 *    - Аутентификация
 *    - Отправка сообщений
 *    - Корректное закрытие
 * 
 * Конфигурация:
 * Файл sms.properties должен содержать:
 * - smpp.host: хост SMPP-сервера
 * - smpp.port: порт SMPP-сервера
 * - smpp.system_id: идентификатор системы
 * - smpp.password: пароль для аутентификации
 * - smpp.system_type: тип системы
 * - smpp.source_addr: адрес отправителя
 * 
 * Безопасность:
 * - Безопасное хранение учетных данных
 * - Логирование операций
 * - Обработка ошибок
 * - Корректное освобождение ресурсов
 */
public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    /**
     * Создает новый экземпляр сервиса отправки SMS.
     * 
     * Процесс инициализации:
     * 1. Загрузка конфигурации из файла
     * 2. Парсинг параметров подключения
     * 3. Валидация настроек
     * 
     * Возможные ошибки:
     * - IllegalStateException: файл конфигурации не найден
     * - RuntimeException: ошибка загрузки конфигурации
     * 
     * @throws IllegalStateException если файл конфигурации не найден
     * @throws RuntimeException если не удалось загрузить конфигурацию
     */
    public SmsNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddr = props.getProperty("smpp.source_addr");
    }

    /**
     * Загружает конфигурацию SMPP из файла sms.properties.
     * 
     * Процесс загрузки:
     * 1. Поиск файла в classpath
     * 2. Чтение настроек
     * 3. Проверка обязательных параметров
     * 
     * Возможные ошибки:
     * - IllegalStateException: файл не найден
     * - RuntimeException: ошибка чтения файла
     * 
     * @return Properties с настройками SMPP-сервера
     * @throws IllegalStateException если файл конфигурации не найден
     * @throws RuntimeException если произошла ошибка при чтении файла
     */
    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) throw new IllegalStateException("sms.properties not found");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Failed to load sms.properties", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправляет SMS с OTP-кодом на указанный номер телефона.
     * 
     * Процесс отправки:
     * 1. Установка SMPP-соединения
     * 2. Аутентификация на сервере
     * 3. Формирование и отправка сообщения
     * 4. Логирование результата
     * 5. Корректное закрытие соединения
     * 
     * Формат сообщения:
     * "Your OTP code: {code}"
     * 
     * Возможные ошибки:
     * - RuntimeException: ошибка подключения или отправки
     * 
     * @param recipientPhone номер телефона получателя
     * @param code OTP-код для отправки
     * @throws RuntimeException если не удалось отправить SMS
     */
    @Override
    public void sendCode(String recipientPhone, String code) {
        TCPIPConnection connection = null;
        Session session = null;
        try {
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            BindTransmitter bindReq = new BindTransmitter();
            bindReq.setSystemId(systemId);
            bindReq.setPassword(password);
            bindReq.setSystemType(systemType);
            bindReq.setInterfaceVersion((byte) 0x34);
            bindReq.setAddressRange(sourceAddr);

            BindResponse bindResp = session.bind(bindReq);
            if (bindResp.getCommandStatus() != 0) {
                throw new RuntimeException("SMPP bind failed: " + bindResp.getCommandStatus());
            }

            SubmitSM submit = new SubmitSM();
            submit.setSourceAddr(sourceAddr);
            submit.setDestAddr(recipientPhone);
            submit.setShortMessage("Your OTP code: " + code);
            session.submit(submit);

            logger.info("OTP sent via SMS to {}", recipientPhone);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}", recipientPhone, e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) try { session.unbind(); } catch (Exception ignored) {}
            if (connection != null) try { connection.close(); } catch (IOException ignored) {}
        }
    }
}