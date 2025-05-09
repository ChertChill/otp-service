package otp.service.notification;

/**
 * Интерфейс для отправки OTP-кодов пользователям.
 * 
 * Основные функции:
 * 1. Отправка OTP-кодов
 *    - Поддержка различных каналов связи
 *    - Форматирование сообщений
 *    - Обработка ошибок доставки
 * 
 * Реализации:
 * - EmailNotificationService: отправка по email
 * - SmsNotificationService: отправка через SMS
 * - TelegramNotificationService: отправка через Telegram
 * - FileNotificationService: запись в файл
 * 
 * Безопасность:
 * - Безопасная передача кодов
 * - Логирование операций
 * - Обработка ошибок доставки
 */
public interface NotificationService {
    /**
     * Отправляет одноразовый код пользователю через выбранный канал.
     * 
     * Процесс отправки:
     * 1. Форматирование сообщения
     * 2. Отправка через выбранный канал
     * 3. Логирование результата
     * 
     * Параметры:
     * - recipient: адрес получателя
     *   * email: email-адрес
     *   * SMS: номер телефона
     *   * Telegram: chatId
     *   * файл: путь к файлу
     * - code: OTP-код для отправки
     * 
     * @param recipient адрес или идентификатор получателя
     * @param code строковое представление OTP-кода
     */
    void sendCode(String recipient, String code);
}