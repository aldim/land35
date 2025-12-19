package com.quizbattle.config;

import com.quizbattle.model.entity.User;
import com.quizbattle.model.entity.UserRole;
import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.repository.UserRepository;
import com.quizbattle.repository.RoomRepository;
import com.quizbattle.repository.RoomInvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
public class DataLoader {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomInvitationRepository roomInvitationRepository;
    
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
            // Удаляем пользователя master35, если он существует
            removeMaster35User();
            
            // Обновляем существующих пользователей - устанавливаем роль PLAYER если не установлена
            updateExistingUsersRoles();
            
            // Устанавливаем alex как администратора
            setAlexAsAdmin();
            
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
    
    private void removeMaster35User() {
        try {
            String username = "master35";
            var master35User = userRepository.findByUsername(username);
            
            if (master35User.isPresent()) {
                User user = master35User.get();
                
                // Находим все комнаты, созданные master35
                List<RoomEntity> rooms = roomRepository.findByHostUser(user);
                
                // Удаляем все приглашения для этих комнат
                for (RoomEntity room : rooms) {
                    roomInvitationRepository.deleteAll(roomInvitationRepository.findByRoom(room));
                }
                
                // Удаляем все комнаты
                roomRepository.deleteAll(rooms);
                
                // Теперь можно удалить пользователя
                userRepository.delete(user);
                System.out.println("Пользователь " + username + " и все связанные данные удалены");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при удалении пользователя master35: " + e.getMessage());
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
    
    private void setAlexAsAdmin() {
        try {
            String username = "alex";
            
            // Ищем пользователя alex
            var alexUser = userRepository.findByUsername(username);
            
            if (alexUser.isPresent()) {
                // Обновляем существующего пользователя
                User user = alexUser.get();
                user.setRole(UserRole.ADMIN);
                userRepository.save(user);
                System.out.println("Пользователь " + username + " установлен как администратор");
            } else {
                System.out.println("Пользователь " + username + " не найден, будет создан при загрузке аватаров");
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при установке администратора: " + e.getMessage());
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
            
            // Определяем роль: alex - ADMIN, остальные - PLAYER
            UserRole role = "alex".equals(username) ? UserRole.ADMIN : UserRole.PLAYER;
            
            // Создаем пользователя
            User user = new User(
                username,
                "password123", // Стандартный пароль для тестовых пользователей
                fullName,
                username, // nickname = username
                avatarPathStr,
                role
            );
            
            userRepository.save(user);
            String roleText = role == UserRole.ADMIN ? " (ADMIN)" : "";
            System.out.println("Создан пользователь: " + username + " (" + fullName + ")" + roleText);
            
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

