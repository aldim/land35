package com.quizbattle.config;

import com.quizbattle.model.entity.User;
import com.quizbattle.model.entity.UserRole;
import com.quizbattle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataLoader {
    
    @Autowired
    private UserRepository userRepository;
    
    // Маппинг имен файлов на полные имена
    private static final Map<String, String> FULL_NAMES = new HashMap<>();
    
    static {
        FULL_NAMES.put("alex", "Александр Петров");
        FULL_NAMES.put("anna", "Анна Смирнова");
        FULL_NAMES.put("arina", "Арина Козлова");
        FULL_NAMES.put("di", "Дмитрий Волков");
        FULL_NAMES.put("eugen", "Евгений Морозов");
        FULL_NAMES.put("ivan", "Иван Соколов");
        FULL_NAMES.put("kate", "Екатерина Лебедева");
        FULL_NAMES.put("lena", "Елена Новикова");
        FULL_NAMES.put("leonid", "Леонид Федоров");
        FULL_NAMES.put("marin", "Марина Орлова");
        FULL_NAMES.put("nata", "Наталья Семенова");
        FULL_NAMES.put("nina", "Нина Егорова");
        FULL_NAMES.put("pavel", "Павел Павлов");
        FULL_NAMES.put("shadow", "Алексей Теневой");
        FULL_NAMES.put("sid", "Сидоров Игорь");
        FULL_NAMES.put("tor", "Тор Олегович");
        FULL_NAMES.put("vladimir", "Владимир Степанов");
    }
    
    @PostConstruct
    public void loadUsers() {
        try {
            // Обновляем существующих пользователей - устанавливаем роль PLAYER если не установлена
            updateExistingUsersRoles();
            
            // Создаем администратора master35
            createAdminUser();
            
            // Используем PathMatchingResourcePatternResolver для работы с ресурсами в JAR
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:avatars/*.png");
            
            if (resources.length == 0) {
                System.out.println("Аватары не найдены в каталоге avatars");
                return;
            }
            
            for (Resource resource : resources) {
                if (resource.exists()) {
                    createUserFromResource(resource);
                }
            }
            
            System.out.println("Данные пользователей успешно загружены из каталога avatars");
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке пользователей: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateExistingUsersRoles() {
        try {
            var allUsers = userRepository.findAll();
            for (var user : allUsers) {
                if (user.getRole() == null) {
                    user.setRole(UserRole.PLAYER);
                    userRepository.save(user);
                    System.out.println("Обновлена роль для пользователя: " + user.getUsername());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении ролей: " + e.getMessage());
        }
    }
    
    private void createAdminUser() {
        try {
            String username = "master35";
            
            // Проверяем, существует ли уже администратор
            if (userRepository.existsByUsername(username)) {
                System.out.println("Администратор " + username + " уже существует, пропускаем");
                return;
            }
            
            // Создаем администратора
            User admin = new User(
                username,
                "password123", // Стандартный пароль
                "Master 35",
                "master35",
                "👑", // Аватар для админа
                UserRole.ADMIN
            );
            
            userRepository.save(admin);
            System.out.println("Создан администратор: " + username);
            
        } catch (Exception e) {
            System.err.println("Ошибка при создании администратора: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createUserFromResource(Resource resource) {
        try {
            String fileName = resource.getFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".png")) {
                return;
            }
            
            String username = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // Проверяем, существует ли уже пользователь с таким username
            if (userRepository.existsByUsername(username)) {
                System.out.println("Пользователь " + username + " уже существует, пропускаем");
                return;
            }
            
            // Получаем полное имя из маппинга или генерируем по умолчанию
            String fullName = FULL_NAMES.getOrDefault(username, 
                capitalizeFirst(username) + " Пользователь");
            
            // Формируем путь к аватару (относительный путь для использования в приложении)
            String avatarPathStr = "/avatars/" + fileName;
            
            // Создаем пользователя (игрок)
            User user = new User(
                username,
                "password123", // Стандартный пароль для тестовых пользователей
                fullName,
                username, // nickname = username
                avatarPathStr,
                UserRole.PLAYER // Все игроки имеют роль PLAYER
            );
            
            userRepository.save(user);
            System.out.println("Создан пользователь: " + username + " (" + fullName + ")");
            
        } catch (Exception e) {
            System.err.println("Ошибка при создании пользователя из ресурса " + resource.getFilename() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

