package com.quizbattle.service;

import com.quizbattle.model.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    // Список доступных аватаров (эмодзи)
    public static final String[] AVATARS = {
        "🦊", "🐼", "🦁", "🐯", "🐸", "🦉", "🦋", "🐙",
        "🦄", "🐲", "🦖", "🐳", "🦀", "🐝", "🦜", "🐨",
        "🐰", "🐻", "🦈", "🐺"
    };
    
    /**
     * Создать новую комнату
     */
    public Room createRoom(String hostSessionId) {
        String code = generateRoomCode();
        Room room = new Room(code, hostSessionId);
        rooms.put(code, room);
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
     */
    public Player addPlayer(String roomCode, String playerName, String avatar) {
        Room room = getRoom(roomCode);
        if (room == null) {
            return null;
        }
        
        if (room.getPlayers().size() >= Room.MAX_PLAYERS) {
            return null;
        }
        
        String playerId = UUID.randomUUID().toString().substring(0, 8);
        Player player = new Player(playerId, playerName, avatar);
        
        if (room.addPlayer(player)) {
            return player;
        }
        
        return null;
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
     * Зарегистрировать нажатие кнопки
     */
    public ButtonPress pressButton(String roomCode, String playerId) {
        Room room = getRoom(roomCode);
        if (room == null || room.getGameState() != GameState.ACTIVE) {
            return null;
        }
        
        Player player = room.getPlayerById(playerId);
        if (player == null) {
            return null;
        }
        
        return room.registerButtonPress(playerId);
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
     * Генерация 4-значного кода комнаты
     */
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code;
        
        do {
            code = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (rooms.containsKey(code.toString()));
        
        return code.toString();
    }
    
    /**
     * Получить список доступных аватаров
     */
    public String[] getAvailableAvatars() {
        return AVATARS;
    }
}


