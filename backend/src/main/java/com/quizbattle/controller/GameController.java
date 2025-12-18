package com.quizbattle.controller;

import com.quizbattle.model.*;
import com.quizbattle.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class GameController {
    
    private static final Logger log = LoggerFactory.getLogger(GameController.class);
    
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        // Передаем messagingTemplate в GameService для уведомлений
        gameService.setMessagingTemplate(messagingTemplate);
    }
    
    /**
     * Создание новой комнаты (от ведущего)
     */
    @MessageMapping("/create-room")
    public void createRoom(Principal principal) {
        String sessionId = principal.getName();
        Room room = gameService.createRoom(sessionId);
        
        log.info("Room created: {} by session: {}", room.getCode(), sessionId);
        
        // Отправляем код комнаты обратно ведущему
        messagingTemplate.convertAndSendToUser(
            sessionId, 
            "/queue/personal", 
            GameMessage.roomCreated(room.getCode())
        );
    }
    
    /**
     * Добавление игрока ведущим
     */
    @MessageMapping("/add-player")
    public void addPlayer(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        String playerName = payload.get("playerName");
        String avatar = payload.get("avatar");
        
        log.info("Add player request: roomCode={}, playerName={}, avatar={}", roomCode, playerName, avatar);
        
        if (roomCode == null || roomCode.isEmpty()) {
            sendError(sessionId, "Код комнаты не указан");
            return;
        }
        
        if (!gameService.isHost(roomCode, sessionId)) {
            sendError(sessionId, "Только ведущий может добавлять игроков");
            return;
        }
        
        Player player = gameService.addPlayer(roomCode, playerName, avatar);
        if (player == null) {
            sendError(sessionId, "Не удалось добавить игрока. Возможно, комната заполнена.");
            return;
        }
        
        Room room = gameService.getRoom(roomCode);
        log.info("Player added: {} ({}) to room: {}", player.getName(), player.getId(), roomCode);
        
        // Уведомляем всех в комнате о новом игроке
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode, 
            GameMessage.playerJoined(room, player)
        );
    }
    
    /**
     * Подключение игрока к сессии
     */
    @MessageMapping("/join-room")
    public void joinRoom(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        String playerId = payload.get("playerId");
        
        Room room = gameService.getRoom(roomCode);
        if (room == null) {
            sendError(sessionId, "Комната не найдена");
            return;
        }
        
        Player player = gameService.connectPlayer(roomCode, playerId, sessionId);
        if (player == null) {
            sendError(sessionId, "Игрок не найден в комнате");
            return;
        }
        
        log.info("Player connected: {} to room: {}", player.getName(), roomCode);
        
        // Отправляем текущее состояние игроку
        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/personal",
            GameMessage.roomState(room)
        );
        
        // Уведомляем всех о подключении
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.playerJoined(room, player)
        );
    }
    
    /**
     * Удаление игрока ведущим
     */
    @MessageMapping("/remove-player")
    public void removePlayer(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        String playerId = payload.get("playerId");
        
        if (!gameService.isHost(roomCode, sessionId)) {
            sendError(sessionId, "Только ведущий может удалять игроков");
            return;
        }
        
        Player player = gameService.removePlayer(roomCode, playerId);
        if (player != null) {
            Room room = gameService.getRoom(roomCode);
            log.info("Player removed: {} from room: {}", player.getName(), roomCode);
            
            messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                GameMessage.playerLeft(room, player)
            );
        }
    }
    
    /**
     * Запуск раунда
     */
    @MessageMapping("/start-round")
    public void startRound(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        
        if (!gameService.startRound(roomCode, sessionId)) {
            sendError(sessionId, "Не удалось начать раунд");
            return;
        }
        
        Room room = gameService.getRoom(roomCode);
        log.info("Round started in room: {}", roomCode);
        
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.roundStarted(room)
        );
    }
    
    /**
     * Нажатие кнопки игроком
     */
    @MessageMapping("/press-button")
    public void pressButton(@Payload Map<String, Object> payload) {
        String roomCode = (String) payload.get("roomCode");
        String playerId = (String) payload.get("playerId");
        
        // Получаем clientTimestamp (может быть Long или String)
        long clientTimestamp;
        Object timestampObj = payload.get("clientTimestamp");
        if (timestampObj instanceof Number) {
            clientTimestamp = ((Number) timestampObj).longValue();
        } else if (timestampObj instanceof String) {
            clientTimestamp = Long.parseLong((String) timestampObj);
        } else {
            // Fallback: используем текущее время сервера
            clientTimestamp = System.currentTimeMillis();
        }
        
        ButtonPress press = gameService.pressButton(roomCode, playerId, clientTimestamp);
        if (press == null) {
            return; // Игнорируем невалидные нажатия
        }
        
        Room room = gameService.getRoom(roomCode);
        Player player = room.getPlayerById(playerId);
        
        log.info("Button pressed by: {} in room: {}, clientTime: {}, serverTime: {}", 
            player.getName(), roomCode, clientTimestamp, System.currentTimeMillis());
        
        // Отправляем промежуточное обновление (нажатие зарегистрировано, но победитель еще не определен)
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.buttonPressed(room, player, press)
        );
        
        // Если раунд завершен (победитель определен), отправляем финальное сообщение
        if (room.getGameState() == GameState.ROUND_ENDED && room.getWinnerId() != null) {
            messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                GameMessage.roundEnded(room)
            );
        }
    }
    
    /**
     * Сброс раунда для нового вопроса
     */
    @MessageMapping("/reset-round")
    public void resetRound(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        
        if (!gameService.resetRound(roomCode, sessionId)) {
            sendError(sessionId, "Не удалось сбросить раунд");
            return;
        }
        
        Room room = gameService.getRoom(roomCode);
        log.info("Round reset in room: {}", roomCode);
        
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.roundReset(room)
        );
    }
    
    /**
     * Получение состояния комнаты
     */
    @MessageMapping("/get-room-state")
    public void getRoomState(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        
        Room room = gameService.getRoom(roomCode);
        if (room == null) {
            sendError(sessionId, "Комната не найдена");
            return;
        }
        
        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/personal",
            GameMessage.roomState(room)
        );
    }
    
    private void sendError(String sessionId, String message) {
        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/personal",
            GameMessage.error(message)
        );
    }
}
