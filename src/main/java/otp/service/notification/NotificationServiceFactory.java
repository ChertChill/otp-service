package otp.service.notification;

import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationService;
import otp.service.notification.EmailNotificationService;
import otp.service.notification.SmsNotificationService;
import otp.service.notification.TelegramNotificationService;
import otp.service.notification.FileNotificationService;

/**
 * Фабрика для создания сервисов отправки уведомлений.
 * 
 * Основные функции:
 * 1. Создание сервисов
 *    - Выбор реализации по каналу
 *    - Инициализация сервисов
 *    - Обработка ошибок
 * 
 * Поддерживаемые каналы:
 * - EMAIL: EmailNotificationService
 * - SMS: SmsNotificationService
 * - TELEGRAM: TelegramNotificationService
 * - FILE: FileNotificationService
 * 
 * Примечание:
 * - Каждый сервис создается заново при запросе
 * - Неподдерживаемые каналы вызывают исключение
 * - Все сервисы реализуют интерфейс NotificationService
 */
public class NotificationServiceFactory {

    /**
     * Создает и возвращает сервис отправки уведомлений для указанного канала.
     * 
     * Процесс создания:
     * 1. Определение типа канала
     * 2. Создание соответствующего сервиса
     * 3. Возврат готового сервиса
     * 
     * Возможные ошибки:
     * - IllegalArgumentException: неподдерживаемый канал
     * 
     * @param channel канал отправки уведомлений
     * @return сервис для отправки уведомлений
     * @throws IllegalArgumentException если канал не поддерживается
     */
    public NotificationService getService(NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return new EmailNotificationService();
            case SMS:
                return new SmsNotificationService();
            case TELEGRAM:
                return new TelegramNotificationService();
            case FILE:
                return new FileNotificationService();
            default:
                throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }
}

