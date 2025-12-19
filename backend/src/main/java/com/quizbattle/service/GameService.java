package com.quizbattle.service;

import com.quizbattle.model.*;
import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.model.entity.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class GameService {
    
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final UserService userService;
    private SimpMessagingTemplate messagingTemplate;
    
    public GameService(UserService userService) {
        this.userService = userService;
    }
    
    // Инжектим через setter, чтобы избежать циклической зависимости
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    // Список доступных аватаров (эмодзи)
    public static final String[] AVATARS = {
        "🦊", "🐼", "🦁", "🐯", "🐸", "🦉", "🦋", "🐙",
        "🦄", "🐲", "🦖", "🐳", "🦀", "🐝", "🦜", "🐨",
        "🐰", "🐻", "🦈", "🐺"
    };
    
    /**
     * Создать новую комнату (сохраняет в БД и создает игровую сессию)
     */
    @Transactional
    public Room createRoom(Long hostUserId, String hostSessionId) {
        User hostUser = userService.getUserById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        String code = generateRoomCode();
        
        // Создаем RoomEntity и сохраняем в БД
        RoomEntity roomEntity = new RoomEntity(code, hostUser);
        userService.saveRoom(roomEntity);
        
        // Создаем Room для игровой сессии
        Room room = new Room(code, hostSessionId);
        rooms.put(code, room);
        return room;
    }
    
    /**
     * Создать комнату в гостевом режиме (только в памяти, без сохранения в БД)
     * Для обратной совместимости со старым фронтендом
     */
    public Room createRoomGuest(String hostSessionId) {
        String code = generateRoomCode();
        Room room = new Room(code, hostSessionId);
        rooms.put(code, room);
        return room;
    }
    
    /**
     * Активировать комнату из БД (для игрока, принявшего приглашение)
     */
    public Room activateRoomFromDatabase(String roomCode, String sessionId) {
        RoomEntity roomEntity = userService.getRoomByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        
        // Если комната уже активна в памяти, возвращаем её
        Room existingRoom = rooms.get(roomCode.toUpperCase());
        if (existingRoom != null) {
            return existingRoom;
        }
        
        // Создаем новую игровую сессию для комнаты
        Room room = new Room(roomCode, sessionId);
        rooms.put(roomCode.toUpperCase(), room);
        return room;
    }
    
    /**
     * Получить комнату по коду
     */
    public Room getRoom(String code) {
        if (code == null) {
            return null;
        }
        return rooms.get(code.toUpperCase());
    }
    
    /**
     * Добавить игрока в комнату (вызывается ведущим)
     * Если userId указан, используем данные из БД, иначе создаем нового игрока
     */
    public Player addPlayer(String roomCode, String playerName, String avatar, Long userId) {
        Room room = getRoom(roomCode);
        if (room == null) {
            // Пытаемся активировать комнату из БД
            room = activateRoomFromDatabase(roomCode, null);
        }
        
        if (room.getPlayers().size() >= Room.MAX_PLAYERS) {
            return null;
        }
        
        // Если userId указан, используем данные пользователя из БД
        if (userId != null) {
            User user = userService.getUserById(userId).orElse(null);
            if (user != null) {
                String playerId = userId.toString(); // Используем ID пользователя как playerId
                String name = user.getNickname() != null && !user.getNickname().isEmpty() 
                        ? user.getNickname() 
                        : user.getFullName();
                String userAvatar = user.getAvatar() != null && !user.getAvatar().isEmpty()
                        ? user.getAvatar()
                        : avatar;
                Player player = new Player(playerId, name, userAvatar);
                if (room.addPlayer(player)) {
                    return player;
                }
                return null;
            }
        }
        
        // Создаем нового игрока без связи с БД (гость)
        String playerId = UUID.randomUUID().toString().substring(0, 8);
        Player player = new Player(playerId, playerName, avatar);
        
        if (room.addPlayer(player)) {
            return player;
        }
        
        return null;
    }
    
    /**
     * Добавить игрока в комнату (без userId, для обратной совместимости)
     */
    public Player addPlayer(String roomCode, String playerName, String avatar) {
        return addPlayer(roomCode, playerName, avatar, null);
    }
    
    /**
     * Подключить игрока к сессии WebSocket
     */
    public Player connectPlayer(String roomCode, String playerId, String sessionId) {
        Room room = getRoom(roomCode);
        if (room == null) {
            return null;
        }
        
        Player player = room.getPlayerById(playerId);
        if (player != null) {
            player.setSessionId(sessionId);
            player.setConnected(true);
        }
        
        return player;
    }
    
    /**
     * Удалить игрока из комнаты
     */
    public Player removePlayer(String roomCode, String playerId) {
        Room room = getRoom(roomCode);
        if (room == null) {
            return null;
        }
        
        Player player = room.getPlayerById(playerId);
        if (player != null) {
            room.removePlayer(playerId);
        }
        
        return player;
    }
    
    /**
     * Начать раунд
     */
    public boolean startRound(String roomCode, String sessionId) {
        Room room = getRoom(roomCode);
        if (room == null || !room.getHostSessionId().equals(sessionId)) {
            return false;
        }
        
        room.startRound();
        return true;
    }
    
    /**
     * Зарегистрировать нажатие кнопки с clientTimestamp
     */
    public ButtonPress pressButton(String roomCode, String playerId, long clientTimestamp) {
        Room room = getRoom(roomCode);
        if (room == null || room.getGameState() != GameState.ACTIVE) {
            return null;
        }
        
        Player player = room.getPlayerById(playerId);
        if (player == null) {
            return null;
        }
        
        // Регистрируем нажатие
        boolean isFirstPress = room.registerButtonPress(playerId, clientTimestamp);
        
        if (isFirstPress) {
            // Это первое нажатие - запускаем таймер для определения победителя
            long bufferWindow = room.calculateBufferWindow();
            
            room.setWinnerDeterminationTask(
                scheduler.schedule(() -> {
                    synchronized (room) {
                        room.determineWinner();
                        // Уведомляем всех о результате
                        notifyRoomState(roomCode);
                    }
                }, bufferWindow, TimeUnit.MILLISECONDS)
            );
        }
        
        // Возвращаем последнее нажатие
        var presses = room.getButtonPresses();
        if (!presses.isEmpty()) {
            return presses.get(presses.size() - 1);
        }
        
        return null;
    }
    
    /**
     * Уведомить всех в комнате об изменении состояния
     */
    private void notifyRoomState(String roomCode) {
        if (messagingTemplate == null) {
            return;
        }
        
        Room room = getRoom(roomCode);
        if (room == null) {
            return;
        }
        
        GameMessage message = GameMessage.roundEnded(room);
        if (message != null) {
            messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                message
            );
        }
    }
    
    /**
     * Сбросить раунд для нового вопроса
     */
    public boolean resetRound(String roomCode, String sessionId) {
        Room room = getRoom(roomCode);
        if (room == null || !room.getHostSessionId().equals(sessionId)) {
            return false;
        }
        
        room.resetRound();
        return true;
    }
    
    /**
     * Отключить игрока при потере соединения
     */
    public void disconnectPlayer(String sessionId) {
        for (Room room : rooms.values()) {
            Player player = room.getPlayerBySessionId(sessionId);
            if (player != null) {
                player.setConnected(false);
            }
        }
    }
    
    /**
     * Удалить комнату
     */
    public void deleteRoom(String code) {
        rooms.remove(code.toUpperCase());
    }
    
    /**
     * Проверить, является ли сессия хостом комнаты
     */
    public boolean isHost(String roomCode, String sessionId) {
        Room room = getRoom(roomCode);
        return room != null && room.getHostSessionId().equals(sessionId);
    }
    
    /**
     * Генерация 4-значного кода комнаты (проверяет и в памяти, и в БД)
     */
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code;
        
        do {
            code = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (rooms.containsKey(code.toString().toUpperCase()) || 
                 userService.getRoomByCode(code.toString()).isPresent());
        
        return code.toString();
    }
    
    /**
     * Получить список доступных аватаров
     */
    public String[] getAvailableAvatars() {
        return AVATARS;
    }
}


