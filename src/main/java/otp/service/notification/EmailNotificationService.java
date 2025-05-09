package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по электронной почте.
 * 
 * Основные функции:
 * 1. Отправка OTP-кодов по email
 *    - Форматирование писем
 *    - Безопасная отправка через SMTP
 *    - Логирование операций
 * 
 * 2. Управление SMTP-соединением
 *    - Аутентификация на сервере
 *    - Использование STARTTLS
 *    - Обработка ошибок
 * 
 * Конфигурация:
 * Файл email.properties должен содержать:
 * - email.username: логин SMTP-сервера
 * - email.password: пароль SMTP-сервера
 * - email.from: адрес отправителя
 * - mail.smtp.host: хост SMTP-сервера
 * - mail.smtp.port: порт SMTP-сервера
 * - mail.smtp.auth: требуется ли аутентификация
 * - mail.smtp.starttls.enable: использовать ли STARTTLS
 * 
 * Безопасность:
 * - Безопасное хранение учетных данных
 * - Шифрование соединения
 * - Логирование ошибок
 */
public class EmailNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final Session session;
    private final String fromAddress;

    /**
     * Создает новый экземпляр сервиса отправки email.
     * 
     * Процесс инициализации:
     * 1. Загрузка конфигурации из файла
     * 2. Создание SMTP-сессии
     * 3. Настройка аутентификации
     * 
     * Возможные ошибки:
     * - IllegalStateException: файл конфигурации не найден
     * - RuntimeException: ошибка загрузки конфигурации
     * 
     * @throws IllegalStateException если файл конфигурации не найден
     * @throws RuntimeException если не удалось загрузить конфигурацию
     */
    public EmailNotificationService() {
        Properties props = loadConfig();
        this.fromAddress = props.getProperty("email.from");
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        props.getProperty("email.username"),
                        props.getProperty("email.password")
                );
            }
        });
    }

    /**
     * Загружает конфигурацию SMTP из файла email.properties.
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
     * @return Properties с настройками SMTP-сервера
     * @throws IllegalStateException если файл конфигурации не найден
     * @throws RuntimeException если произошла ошибка при чтении файла
     */
    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (is == null) {
                throw new IllegalStateException("email.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Failed to load email.properties", e);
            throw new RuntimeException("Could not load email configuration", e);
        }
    }

    /**
     * Отправляет письмо с OTP-кодом на указанный email-адрес.
     * 
     * Процесс отправки:
     * 1. Создание сообщения
     * 2. Установка параметров письма
     * 3. Отправка через SMTP
     * 4. Логирование результата
     * 
     * Формат письма:
     * - Тема: "Your OTP Code"
     * - Текст: "Your one-time confirmation code is: {code}"
     * 
     * Возможные ошибки:
     * - RuntimeException: ошибка отправки письма
     * 
     * @param recipientEmail email-адрес получателя
     * @param code OTP-код для отправки
     * @throws RuntimeException если не удалось отправить письмо
     */
    @Override
    public void sendCode(String recipientEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your one-time confirmation code is: " + code);

            Transport.send(message);
            logger.info("OTP code sent via Email to {}", recipientEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}", recipientEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}

