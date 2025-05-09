# OTP Service

Backend-сервис на Java для генерации и проверки временных кодов (OTP) с отправкой через Email, SMS (SMPP эмулятор), Telegram и сохранением в файл. Сервис предназначен для защиты действий пользователей с помощью одноразовых паролей (OTP).

---

## Основные функции

- **Регистрация и аутентификация пользователей** с ролями: `ADMIN` и `USER`
- **Генерация и отправка OTP-кодов**:
  - Email (JavaMail)
  - SMS (SMPP-эмулятор)
  - Telegram Bot API
  - Сохранение OTP в файл
- **Проверка OTP-кодов** с учетом статусов: `ACTIVE`, `USED`, `EXPIRED`
- **Администрирование** (настройка TTL и длины OTP, управление пользователями)
- **Токенная авторизация** с проверкой ролей
- **Логирование** всех ключевых операций через SLF4J/Logback

---

## Технологии

- **Java 17**
- **PostgreSQL 17 + JDBC** – без Hibernate
- **Maven** – система сборки
- **JavaMail** для отправки Email
- **SMPP** – OpenSMPP-core, эмулятор SMPPsim
- **Telegram Bot API** – Apache HttpClient
- **HttpServer** – встроенный com.sun.net.httpserver
- **SLF4J/Logback** для логирования

---

## Установка и запуск

### 1. Подготовка

#### Системные требования
- Java 17 или выше
- PostgreSQL 17 или выше
- Maven 3.6 или выше

#### Установка зависимостей

1. **Java 17**
```bash
# Для Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Для macOS (через Homebrew)
brew install openjdk@17

# Для Windows
# Скачайте и установите с официального сайта Oracle или используйте OpenJDK
```

2. **PostgreSQL 17**
```bash
# Для Ubuntu/Debian
sudo apt update
sudo apt install postgresql-17

# Для macOS
brew install postgresql@17

# Для Windows
# Скачайте и установите с официального сайта PostgreSQL
```

3. **Maven**
```bash
# Для Ubuntu/Debian
sudo apt update
sudo apt install maven

# Для macOS
brew install maven

# Для Windows
# Скачайте и установите с официального сайта Maven
```

### 2. Настройка базы данных

1. Создайте базу данных и пользователя:
```sql
CREATE DATABASE otp_service;
CREATE USER otp_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE otp_service TO otp_user;
```

2. Настройте права доступа в `pg_hba.conf`:
```conf
# IPv4 local connections:
host    otp_service    otp_user    127.0.0.1/32    md5
```

### 3. Настройка проекта

1. Клонируйте репозиторий:
```bash
git clone https://github.com/chertchill/otp-service.git
cd otp-service
```

2. Настройте конфигурационные файлы в `src/main/resources`:

#### application.properties
```properties
# Настройки базы данных
db.url=jdbc:postgresql://localhost:5432/otp_service
db.user=otp_user
db.password=your_password

# Настройки сервера
server.port=8080
server.host=localhost

# Настройки безопасности
jwt.secret=your_jwt_secret_key
jwt.expiration=3600000
```

#### email.properties
```properties
# SMTP настройки
email.username=your_email@gmail.com
email.password=your_app_password
email.from=your_email@gmail.com
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.auth=true
mail.smtp.starttls.enable=true
```

#### sms.properties
```properties
# SMPP настройки
smpp.host=localhost
smpp.port=2775
smpp.system_id=smppclient1
smpp.password=password
smpp.system_type=OTP
smpp.source_addr=OTPService
```

#### telegram.properties
```properties
# Telegram Bot настройки
telegram.apiUrl=https://api.telegram.org/bot
telegram.token=your_bot_token
telegram.chatId=your_chat_id
```

### 4. Настройка каналов связи

#### Email (Gmail)
1. Включите двухфакторную аутентификацию
2. Создайте пароль приложения
3. Используйте его в `email.properties`

#### SMS (SMPP эмулятор)
1. Скачайте SMPPsim с [официального сайта](http://www.seleniumsoftware.com/downloads.html)
2. Распакуйте и запустите:
```bash
cd SMPPsim
./startsmppsim.bat  # Windows
./startsmppsim.sh   # Linux/macOS
```

#### Telegram Bot
1. Создайте бота через [@BotFather](https://t.me/BotFather)
2. Получите токен и добавьте в `telegram.properties`
3. Начните диалог с ботом
4. Получите chat_id через API:
```bash
curl https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
```

### 5. Сборка и запуск

1. Соберите проект:
```bash
mvn clean package
```

2. Запустите приложение:
```bash
java -jar target/otp-service.jar
```

3. Проверьте логи:
```bash
tail -f logs/otp-service.log
```

---

## 📂 Структура проекта

```
otp-protection-service/
├── src/                      # Исходный код и ресурсы
│   └── main/
│       ├── java/             # Java-код
│       │   └── otp/
│       │       ├── api/      # HTTP-контроллеры – API-слой
│       │       ├── config/   # Конфигурация приложения – Загрузка конфигураций
│       │       ├── dao/      # Доступ к базе данных – JDBC-реализация
│       │       ├── model/    # Модели данных (DTO и сущности)
│       │       ├── service/  # Бизнес-логика и сервисы
│       │       └── util/     # Вспомогательные классы и утилиты
│       └── resources/        # Конфигурационные файлы и ресурсы
│           ├── application.properties  # Общие настройки приложения
│           ├── email.properties        # Настройки Email
│           ├── logback.xml             # Конфигурация логирования
│           ├── sms.properties          # Настройки SMS
│           └── telegram.properties     # Настройки Telegram
├── pom.xml                   # Конфигурация Maven
└── README.md                 # Описание проекта
```

---

## Роли и авторизация

### Роли пользователей

#### ADMIN
- Полный доступ к системе
- Управление пользователями
- Настройка параметров OTP
- Просмотр статистики
- Управление безопасностью

#### USER
- Генерация OTP-кодов
- Проверка OTP-кодов
- Просмотр своей истории
- Базовые операции

### Токены JWT

- Генерируются при успешной аутентификации
- Содержат информацию о роли пользователя
- Имеют ограниченный срок действия
- Передаются в заголовке Authorization

---

## 📖 API Endpoints

### Аутентификация

#### Регистрация
```http
POST /register
Content-Type: application/json

{
    "username": "user1",
    "password": "password123",
    "role": "USER"
}
```

#### Вход
```http
POST /login
Content-Type: application/json

{
    "username": "user1",
    "password": "password123"
}
```

### OTP операции

#### Генерация OTP
```http
POST /otp/generate
Authorization: Bearer <token>
Content-Type: application/json

{
    "operationId": "op123",
    "channel": "EMAIL"
}
```

#### Проверка OTP
```http
POST /otp/validate
Authorization: Bearer <token>
Content-Type: application/json

{
    "code": "123456"
}
```

### Административные функции

#### Настройка OTP
```http
PATCH /admin/config
Authorization: Bearer <admin_token>
Content-Type: application/json

{
    "length": 6,
    "ttlSeconds": 300
}
```

#### Управление пользователями
```http
GET /admin/users
Authorization: Bearer <admin_token>

DELETE /admin/users/{userId}
Authorization: Bearer <admin_token>
```

---

## Тестирование

### Подготовка к тестированию

1. Создайте тестового администратора:
```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
  }'
```

2. Создайте тестового пользователя:
```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user123",
    "role": "USER"
  }'
```

### Проверка функционала

1. **Аутентификация**
```bash
# Получение токена
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user123"
  }'
```

2. **OTP операции**
```bash
# Генерация OTP
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "operationId": "test_op",
    "channel": "EMAIL"
  }'

# Проверка OTP
curl -X POST http://localhost:8080/otp/validate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456"
  }'
```

3. **Административные функции**
```bash
# Изменение конфигурации
curl -X PATCH http://localhost:8080/admin/config \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "length": 6,
    "ttlSeconds": 300
  }'

# Просмотр пользователей
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer <admin_token>"
```

---

## Устранение неполадок

### Общие проблемы

1. **Ошибка подключения к БД**
- Проверьте настройки в `application.properties`
- Убедитесь, что PostgreSQL запущен
- Проверьте права доступа пользователя

2. **Проблемы с Email**
- Проверьте настройки SMTP
- Убедитесь, что включена двухфакторная аутентификация
- Проверьте пароль приложения

3. **Проблемы с SMS**
- Проверьте, что SMPPsim запущен
- Проверьте порт и настройки в `sms.properties`
- Проверьте логи SMPPsim

4. **Проблемы с Telegram**
- Проверьте токен бота
- Убедитесь, что бот активен
- Проверьте chat_id

### Логи

- Основной лог: `logs/otp-service.log`
- Лог SMPPsim: `SMPPsim/logs/smppsim.log`
- Логи PostgreSQL: `/var/log/postgresql/postgresql-17-main.log`

---

## Дополнительная документация

- [JavaMail API](https://javaee.github.io/javamail/)
- [SMPP Protocol](http://opensmpp.org/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT Documentation](https://jwt.io/)

