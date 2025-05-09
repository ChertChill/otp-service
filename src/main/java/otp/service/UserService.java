package otp.service;

import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import otp.util.PasswordEncoder;
import otp.util.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Сервис для управления пользователями системы.
 * 
 * Основные функции:
 * 1. Регистрация пользователей
 *    - Создание новых пользователей
 *    - Проверка уникальности логина
 *    - Ограничение на количество администраторов
 * 
 * 2. Аутентификация
 *    - Проверка учетных данных
 *    - Генерация JWT токенов
 *    - Безопасное хранение паролей
 * 
 * 3. Управление пользователями
 *    - Поиск пользователей
 *    - Удаление пользователей
 *    - Фильтрация по ролям
 * 
 * Безопасность:
 * - Хеширование паролей
 * - Проверка уникальности логинов
 * - Ограничение на количество администраторов
 * - Логирование всех операций
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;

    /**
     * Создает новый экземпляр сервиса пользователей.
     * 
     * Зависимости:
     * - userDao: для работы с базой данных пользователей
     * 
     * @param userDao DAO для работы с пользователями
     */
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Регистрирует нового пользователя в системе.
     * 
     * Процесс регистрации:
     * 1. Проверка уникальности логина
     * 2. Проверка возможности создания администратора
     * 3. Хеширование пароля
     * 4. Создание пользователя в базе данных
     * 
     * Возможные ошибки:
     * - IllegalArgumentException: логин уже занят
     * - IllegalStateException: попытка создать второго администратора
     * 
     * @param username логин пользователя
     * @param password пароль пользователя
     * @param role роль пользователя
     */
    public void register(String username, String password, UserRole role) {
        if (userDao.findByUsername(username) != null) {
            logger.warn("Attempt to register with existing username: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }
        if (role == UserRole.ADMIN && adminExists()) {  // Используем новый метод adminExists
            logger.warn("Attempt to register second ADMIN: {}", username);
            throw new IllegalStateException("Administrator already exists");
        }

        String hashed = PasswordEncoder.hash(password);
        User user = new User(null, username, hashed, role);
        userDao.create(user);
        logger.info("Registered new user: {} with role {}", username, role);
    }

    /**
     * Проверяет наличие администратора в системе.
     * 
     * Процесс проверки:
     * 1. Получение списка всех пользователей без администраторов
     * 2. Проверка наличия администратора
     * 
     * @return true если администратор существует, false в противном случае
     */
    public boolean adminExists() {
        List<User> users = userDao.findAllUsersWithoutAdmins();  // Получаем всех пользователей без администраторов
        return users.isEmpty();  // Если список пуст, значит администратор не существует
    }

    /**
     * Аутентифицирует пользователя и генерирует JWT токен.
     * 
     * Процесс аутентификации:
     * 1. Поиск пользователя по логину
     * 2. Проверка пароля
     * 3. Генерация JWT токена
     * 
     * Возможные ошибки:
     * - IllegalArgumentException: неверный логин или пароль
     * 
     * @param username логин пользователя
     * @param password пароль пользователя
     * @return JWT токен для аутентификации
     */
    public String login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.warn("Login failed: user not found {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (!PasswordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Login failed: wrong password for {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }
        String token = TokenManager.generateToken(user);
        logger.info("User {} logged in, token generated", username);
        return token;
    }

    /**
     * Находит пользователя по его идентификатору.
     * 
     * @param id идентификатор пользователя
     * @return найденный пользователь или null
     */
    public User findById(Long id) {
        return userDao.findById(id);
    }

    /**
     * Возвращает список всех пользователей, исключая администраторов.
     * 
     * @return список пользователей без администраторов
     */
    public List<User> findAllWithoutAdmins() {
        return userDao.findAllUsersWithoutAdmins();
    }

    /**
     * Удаляет пользователя из системы.
     * 
     * Процесс удаления:
     * 1. Удаление пользователя из базы данных
     * 2. Логирование операции
     * 
     * @param id идентификатор удаляемого пользователя
     */
    public void deleteUser(Long id) {
        userDao.delete(id);
        logger.info("Deleted user with id {}", id);
    }
}
