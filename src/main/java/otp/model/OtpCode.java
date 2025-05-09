package otp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сущность одноразового кода (OTP).
 * 
 * Основные функции:
 * 1. Хранение данных кода
 *    - Уникальный идентификатор
 *    - Связь с пользователем
 *    - Привязка к операции
 *    - Значение кода
 *    - Статус и время
 * 
 * 2. Управление жизненным циклом
 *    - Создание (ACTIVE)
 *    - Использование (USED)
 *    - Истечение (EXPIRED)
 *    - Валидация состояния
 * 
 * 3. Безопасность
 *    - Одноразовое использование
 *    - Временное действие
 *    - Привязка к операции
 *    - Аудит изменений
 * 
 * Жизненный цикл:
 * 1. Создание
 *    - Генерация кода
 *    - Установка ACTIVE
 *    - Запись времени
 * 
 * 2. Использование
 *    - Валидация кода
 *    - Установка USED
 *    - Завершение операции
 * 
 * 3. Истечение
 *    - Проверка TTL
 *    - Установка EXPIRED
 *    - Архивация данных
 */
public class OtpCode {
    private Long id;
    private Long userId;
    private String operationId;   // идентификатор операции, к которой привязан код (может быть null)
    private String code;          // сам OTP
    private OtpStatus status;     // статус кода: ACTIVE, EXPIRED, USED
    private LocalDateTime createdAt;

    /**
     * Пустой конструктор для фреймворков и JDBC.
     * Используется при:
     * - Маппинге данных из БД
     * - Создании объектов через DI
     * - Сериализации/десериализации
     */
    public OtpCode() {
    }

    /**
     * Полный конструктор для создания нового OTP-кода.
     * 
     * @param id уникальный идентификатор записи в БД
     * @param userId идентификатор пользователя, для которого сгенерирован код
     * @param operationId идентификатор операции (может быть null)
     * @param code сгенерированный OTP-код
     * @param status начальный статус кода
     * @param createdAt дата и время создания кода
     * 
     * @throws IllegalArgumentException если параметры не соответствуют ограничениям
     */
    public OtpCode(Long id,
                   Long userId,
                   String operationId,
                   String code,
                   OtpStatus status,
                   LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Возвращает уникальный идентификатор записи в БД.
     * 
     * @return идентификатор записи или null для новых объектов
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает уникальный идентификатор записи в БД.
     * 
     * @param id идентификатор записи
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор пользователя, для которого сгенерирован код.
     * 
     * @return идентификатор пользователя
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Устанавливает идентификатор пользователя.
     * 
     * @param userId идентификатор пользователя
     * @throws IllegalArgumentException если userId равен null
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Возвращает идентификатор операции, к которой привязан код.
     * 
     * @return идентификатор операции или null, если код не привязан
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Устанавливает идентификатор операции.
     * 
     * @param operationId идентификатор операции (может быть null)
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * Возвращает сам OTP-код.
     * 
     * @return значение OTP-кода
     */
    public String getCode() {
        return code;
    }

    /**
     * Устанавливает OTP-код.
     * 
     * @param code значение OTP-кода
     * @throws IllegalArgumentException если код не соответствует формату
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Возвращает текущий статус кода.
     * 
     * @return статус кода (ACTIVE, USED, EXPIRED)
     */
    public OtpStatus getStatus() {
        return status;
    }

    /**
     * Устанавливает новый статус кода.
     * 
     * @param status новый статус
     * @throws IllegalArgumentException если статус недопустим для текущего состояния
     */
    public void setStatus(OtpStatus status) {
        this.status = status;
    }

    /**
     * Возвращает дату и время создания кода.
     * 
     * @return время создания
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает дату и время создания кода.
     * 
     * @param createdAt время создания
     * @throws IllegalArgumentException если время в будущем
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Сравнивает текущий объект с другим на равенство.
     * 
     * @param o объект для сравнения
     * @return true если объекты равны по всем полям
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtpCode otpCode = (OtpCode) o;
        return Objects.equals(id, otpCode.id)
                && Objects.equals(userId, otpCode.userId)
                && Objects.equals(operationId, otpCode.operationId)
                && Objects.equals(code, otpCode.code)
                && status == otpCode.status
                && Objects.equals(createdAt, otpCode.createdAt);
    }

    /**
     * Возвращает хеш-код объекта.
     * 
     * @return хеш-код, вычисленный на основе всех полей
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, operationId, code, status, createdAt);
    }

    /**
     * Возвращает строковое представление объекта.
     * 
     * @return строка в формате "OtpCode{id=X, userId=Y, operationId=Z, code=W, status=V, createdAt=U}"
     */
    @Override
    public String toString() {
        return "OtpCode{" +
                "id=" + id +
                ", userId=" + userId +
                ", operationId='" + operationId + '\'' +
                ", code='" + code + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

