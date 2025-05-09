package otp.dao;

import otp.model.OtpCode;
import java.time.Duration;
import java.util.List;

/**
 * Интерфейс для работы с одноразовыми кодами (OTP) в базе данных.
 * 
 * Основные функции:
 * 1. Управление кодами
 *    - Сохранение новых кодов
 *    - Поиск по значению
 *    - Получение по пользователю
 *    - Валидация данных
 *    - Контроль состояния
 * 
 * 2. Управление состоянием
 *    - Отметка как использованный
 *    - Отметка как просроченный
 *    - Удаление кодов
 *    - Обновление статусов
 *    - Контроль времени жизни
 * 
 * 3. Безопасность
 *    - Проверка уникальности
 *    - Контроль времени жизни
 *    - Каскадное удаление
 *    - Валидация данных
 *    - Аудит операций
 * 
 * Процесс работы:
 * 1. Создание кодов
 *    - Генерация значений
 *    - Валидация данных
 *    - Сохранение в БД
 *    - Контроль уникальности
 * 
 * 2. Управление состоянием
 *    - Проверка срока действия
 *    - Обновление статусов
 *    - Удаление кодов
 *    - Очистка данных
 * 
 * 3. Поиск и валидация
 *    - Поиск по значению
 *    - Поиск по пользователю
 *    - Проверка состояния
 *    - Валидация данных
 * 
 * Безопасность:
 * - Проверка уникальности
 * - Контроль времени жизни
 * - Каскадное удаление
 * - Валидация данных
 * - Аудит операций
 * - Защита от атак
 */
public interface OtpCodeDao {

    /**
     * Сохраняет новый одноразовый код в базе данных.
     * 
     * Процесс сохранения:
     * 1. Валидация данных
     *    - Проверка значения
     *    - Контроль уникальности
     *    - Валидация параметров
     *    - Подготовка к сохранению
     * 
     * 2. Подготовка объекта
     *    - Генерация ID
     *    - Установка времени
     *    - Настройка статуса
     *    - Формирование данных
     * 
     * 3. Сохранение в БД
     *    - Вставка записи
     *    - Проверка результата
     *    - Обработка ошибок
     *    - Логирование
     * 
     * @param code объект OtpCode (id и createdAt могут быть null — будут заполнены БД)
     * @throws IllegalArgumentException если данные невалидны
     * @throws SQLException при ошибке БД
     */
    void save(OtpCode code);

    /**
     * Ищет код по его значению.
     * 
     * Процесс поиска:
     * 1. Подготовка запроса
     *    - Валидация значения
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
     *    - Проверка состояния
     *    - Формирование объекта
     *    - Обновление кэша
     *    - Логирование
     * 
     * @param code строка кода
     * @return объект OtpCode или null если не найден
     * @throws IllegalArgumentException если код невалиден
     * @throws SQLException при ошибке БД
     */
    OtpCode findByCode(String code);

    /**
     * Получает все коды пользователя.
     * 
     * Процесс получения:
     * 1. Подготовка запроса
     *    - Валидация ID
     *    - Формирование SQL
     *    - Настройка сортировки
     *    - Подготовка кэша
     * 
     * 2. Выполнение запроса
     *    - Поиск в БД
     *    - Фильтрация результатов
     *    - Сортировка данных
     *    - Валидация результатов
     * 
     * 3. Обработка результата
     *    - Формирование списка
     *    - Обновление кэша
     *    - Логирование
     *    - Возврат данных
     * 
     * @param userId идентификатор пользователя
     * @return список всех OtpCode для данного пользователя
     * @throws IllegalArgumentException если ID невалиден
     * @throws SQLException при ошибке БД
     */
    List<OtpCode> findAllByUser(Long userId);

    /**
     * Отмечает код как использованный.
     * 
     * Процесс обновления:
     * 1. Подготовка операции
     *    - Поиск кода
     *    - Проверка состояния
     *    - Валидация данных
     *    - Подготовка к обновлению
     * 
     * 2. Обновление статуса
     *    - Изменение состояния
     *    - Обновление времени
     *    - Проверка результата
     *    - Обработка ошибок
     * 
     * 3. Завершение операции
     *    - Обновление кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param id идентификатор записи OtpCode
     * @throws IllegalArgumentException если ID невалиден
     * @throws SQLException при ошибке БД
     */
    void markAsUsed(Long id);

    /**
     * Отмечает просроченные коды.
     * 
     * Процесс обновления:
     * 1. Подготовка операции
     *    - Расчет времени
     *    - Формирование SQL
     *    - Настройка параметров
     *    - Подготовка условий
     * 
     * 2. Обновление статусов
     *    - Поиск кодов
     *    - Изменение состояния
     *    - Обновление времени
     *    - Проверка результатов
     * 
     * 3. Завершение операции
     *    - Обновление кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param ttl время жизни кода (Duration), все коды с createdAt + ttl < now() станут EXPIRED
     * @throws IllegalArgumentException если TTL невалиден
     * @throws SQLException при ошибке БД
     */
    void markAsExpiredOlderThan(Duration ttl);

    /**
     * Удаляет все коды пользователя.
     * 
     * Процесс удаления:
     * 1. Подготовка операции
     *    - Поиск кодов
     *    - Проверка зависимостей
     *    - Валидация данных
     *    - Подготовка к удалению
     * 
     * 2. Удаление данных
     *    - Каскадное удаление
     *    - Очистка связей
     *    - Проверка результатов
     *    - Обработка ошибок
     * 
     * 3. Завершение операции
     *    - Очистка кэша
     *    - Логирование
     *    - Аудит изменений
     *    - Возврат результата
     * 
     * @param userId идентификатор пользователя
     * @throws IllegalArgumentException если ID невалиден
     * @throws SQLException при ошибке БД
     */
    void deleteAllByUserId(Long userId);
}

