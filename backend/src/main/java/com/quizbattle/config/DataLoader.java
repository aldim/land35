package com.quizbattle.config;

import com.quizbattle.model.entity.User;
import com.quizbattle.model.entity.UserRole;
import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.model.entity.Team;
import com.quizbattle.repository.UserRepository;
import com.quizbattle.repository.RoomRepository;
import com.quizbattle.repository.RoomInvitationRepository;
import com.quizbattle.repository.TeamRepository;
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
    
    @Autowired
    private TeamRepository teamRepository;
    
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
            
            // Создаем команды
            createTeams();
            
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
            
            // Связываем пользователей с командами
            assignUsersToTeams();
            
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
    
    /**
     * Создать команды в базе данных
     */
    private void createTeams() {
        try {
            // Команда 1: Ведьмачий ковеант
            createTeamIfNotExists("Ведьмачий ковеант", null);
            
            // Команда 2: Тифлинги
            createTeamIfNotExists("Тифлинги", null);
            
            // Команда 3: Орда Братва
            createTeamIfNotExists("Орда Братва", null);
            
            // Команда 4: Лесной союз
            createTeamIfNotExists("Лесной союз", null);
            
            System.out.println("Команды успешно созданы/обновлены");
        } catch (Exception e) {
            System.err.println("Ошибка при создании команд: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создать команду, если она не существует
     */
    private void createTeamIfNotExists(String teamName, String backgroundImage) {
        var existingTeam = teamRepository.findByName(teamName);
        if (existingTeam.isEmpty()) {
            Team team = new Team(teamName, backgroundImage);
            teamRepository.save(team);
            System.out.println("Создана команда: " + teamName);
        }
    }
    
    /**
     * Привязать пользователей к командам согласно распределению
     */
    private void assignUsersToTeams() {
        try {
            // Получаем команды
            Team team1 = teamRepository.findByName("Ведьмачий ковеант")
                    .orElseThrow(() -> new IllegalStateException("Команда 'Ведьмачий ковеант' не найдена"));
            Team team2 = teamRepository.findByName("Тифлинги")
                    .orElseThrow(() -> new IllegalStateException("Команда 'Тифлинги' не найдена"));
            Team team3 = teamRepository.findByName("Орда Братва")
                    .orElseThrow(() -> new IllegalStateException("Команда 'Орда Братва' не найдена"));
            Team team4 = teamRepository.findByName("Лесной союз")
                    .orElseThrow(() -> new IllegalStateException("Команда 'Лесной союз' не найдена"));
            
            // Команда 1: Ведьмачий ковеант - kate, leonid, lena, arina
            assignUserToTeam("kate", team1);
            assignUserToTeam("leonid", team1);
            assignUserToTeam("lena", team1);
            assignUserToTeam("arina", team1);
            
            // Команда 2: Тифлинги - anna, tor, vladimir, eugen
            assignUserToTeam("anna", team2);
            assignUserToTeam("tor", team2);
            assignUserToTeam("vladimir", team2);
            assignUserToTeam("eugen", team2);
            
            // Команда 3: Орда Братва - di, nata, sid, pavel
            assignUserToTeam("di", team3);
            assignUserToTeam("nata", team3);
            assignUserToTeam("sid", team3);
            assignUserToTeam("pavel", team3);
            
            // Команда 4: Лесной союз - shadow, ivan, nina, marin
            assignUserToTeam("shadow", team4);
            assignUserToTeam("ivan", team4);
            assignUserToTeam("nina", team4);
            assignUserToTeam("marin", team4);
            
            System.out.println("Пользователи успешно привязаны к командам");
        } catch (Exception e) {
            System.err.println("Ошибка при привязке пользователей к командам: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Привязать пользователя к команде
     */
    private void assignUserToTeam(String username, Team team) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTeam(team);
            userRepository.save(user);
            System.out.println("Пользователь " + username + " привязан к команде " + team.getName());
        } else {
            System.out.println("Пользователь " + username + " не найден, пропускаем привязку к команде");
        }
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

