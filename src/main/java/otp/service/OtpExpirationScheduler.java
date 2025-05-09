package otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Планировщик для автоматического обновления статуса просроченных OTP-кодов.
 * 
 * Основные функции:
 * 1. Периодическая проверка OTP-кодов
 *    - Запуск по расписанию
 *    - Настраиваемый интервал проверки
 *    - Помечание просроченных кодов как EXPIRED
 * 
 * 2. Управление жизненным циклом
 *    - Запуск планировщика
 *    - Корректное завершение работы
 *    - Обработка ошибок
 * 
 * Особенности реализации:
 * - Использование ScheduledExecutorService
 * - Одиночный поток выполнения
 * - Логирование всех операций
 * - Graceful shutdown при остановке
 */
public class OtpExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpService otpService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Интервал в минутах между запусками проверки */
    private final long intervalMinutes;

    /**
     * Создает новый экземпляр планировщика.
     * 
     * Зависимости:
     * - otpService: для работы с OTP-кодами
     * 
     * Параметры:
     * - intervalMinutes: интервал между проверками в минутах
     * 
     * @param otpService сервис для работы с OTP-кодами
     * @param intervalMinutes интервал между проверками в минутах
     */
    public OtpExpirationScheduler(OtpService otpService, long intervalMinutes) {
        this.otpService = otpService;
        this.intervalMinutes = intervalMinutes;
    }

    /**
     * Запускает планировщик проверки OTP-кодов.
     * 
     * Процесс запуска:
     * 1. Логирование начала работы
     * 2. Настройка периодического выполнения
     * 3. Установка начальной задержки и интервала
     * 
     * Примечание:
     * - Первый запуск через intervalMinutes после старта
     * - Последующие запуски каждые intervalMinutes
     * - Используется один поток для всех проверок
     */
    public void start() {
        logger.info("Starting OTP-expiration scheduler, interval={} min", intervalMinutes);
        scheduler.scheduleAtFixedRate(
                this::run,           // явно вызываем наш метод run()
                intervalMinutes,     // initial delay
                intervalMinutes,     // period
                TimeUnit.MINUTES
        );
    }

    /**
     * Выполняет одну итерацию проверки OTP-кодов.
     * 
     * Процесс проверки:
     * 1. Вызов otpService.markExpiredOtps()
     * 2. Логирование результата
     * 3. Обработка возможных ошибок
     * 
     * Безопасность:
     * - Обработка всех исключений
     * - Логирование ошибок
     * - Не прерывает работу планировщика при ошибках
     */
    public void run() {
        try {
            otpService.markExpiredOtps();
            logger.debug("OtpExpirationScheduler run(): expired codes processed");
        } catch (Exception e) {
            logger.error("Error in OTP-expiration task", e);
        }
    }

    /**
     * Останавливает планировщик проверки OTP-кодов.
     * 
     * Процесс остановки:
     * 1. Логирование остановки
     * 2. Немедленное завершение всех задач
     * 3. Освобождение ресурсов
     * 
     * Примечание:
     * - Выполняется при завершении работы приложения
     * - Отменяет все запланированные задачи
     * - Не дожидается завершения текущей задачи
     */
    public void stop() {
        logger.info("Stopping OTP-expiration scheduler");
        scheduler.shutdownNow();
    }
}

