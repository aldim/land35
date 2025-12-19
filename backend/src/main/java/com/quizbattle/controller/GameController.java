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
     * Создание новой комнаты (только для администратора)
     */
    @MessageMapping("/create-room")
    public void createRoom(@Payload Map<String, Object> payload, Principal principal) {
        String sessionId = principal.getName();
        
        // userId обязателен - комната может быть создана только авторизованным администратором
        Long userId = null;
        Object userIdObj = payload != null ? payload.get("userId") : null;
        if (userIdObj != null) {
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    sendError(sessionId, "Неверный формат userId");
                    return;
                }
            }
        }
        
        if (userId == null) {
            sendError(sessionId, "Требуется авторизация. Только администратор может создавать комнаты.");
            return;
        }
        
        // Проверяем параметр forceNew (если true, всегда создает новую комнату)
        boolean forceNew = false;
        Object forceNewObj = payload != null ? payload.get("forceNew") : null;
        if (forceNewObj != null) {
            if (forceNewObj instanceof Boolean) {
                forceNew = (Boolean) forceNewObj;
            } else if (forceNewObj instanceof String) {
                forceNew = Boolean.parseBoolean((String) forceNewObj);
            }
        }
        
        try {
            // Создаем комнату с сохранением в БД (проверка роли внутри)
            Room room = gameService.createRoom(userId, sessionId, forceNew);
            log.info("Room created: {} by userId: {}, session: {}, forceNew: {}", room.getCode(), userId, sessionId, forceNew);
            
            // Отправляем код комнаты обратно ведущему
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/personal", 
                GameMessage.roomCreated(room.getCode())
            );
            
            // Отправляем состояние комнаты с игроками
            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/personal",
                GameMessage.roomState(room)
            );
        } catch (IllegalArgumentException e) {
            sendError(sessionId, e.getMessage());
        }
    }
    
    /**
     * Добавление игрока ведущим (ОТКЛЮЧЕНО)
     * Игроки теперь загружаются автоматически из БД при создании комнаты
     */
    @MessageMapping("/add-player")
    public void addPlayer(@Payload Map<String, Object> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = (String) payload.get("roomCode");
        
        log.info("Add player request ignored (auto-loading enabled): roomCode={}", roomCode);
        
        // Отправляем сообщение о том, что функционал отключен
        sendError(sessionId, "Ручное добавление игроков отключено. Все игроки загружаются автоматически из базы данных.");
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
     * Удаление игрока ведущим (ОТКЛЮЧЕНО)
     * Игроки загружаются из БД и не могут быть удалены вручную
     */
    @MessageMapping("/remove-player")
    public void removePlayer(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        String playerId = payload.get("playerId");
        
        log.info("Remove player request ignored (auto-loading enabled): roomCode={}, playerId={}", roomCode, playerId);
        
        // Отправляем сообщение о том, что функционал отключен
        sendError(sessionId, "Удаление игроков отключено. Все игроки загружаются автоматически из базы данных.");
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
    
    /**
     * Оглушить игрока (админ может заблокировать кнопку игрока на один раунд)
     */
    @MessageMapping("/stun-player")
    public void stunPlayer(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = payload.get("roomCode");
        String playerId = payload.get("playerId");
        
        if (!gameService.isHost(roomCode, sessionId)) {
            sendError(sessionId, "Только хост комнаты может оглушать игроков");
            return;
        }
        
        boolean success = gameService.stunPlayer(roomCode, playerId, sessionId);
        if (!success) {
            sendError(sessionId, "Не удалось оглушить игрока");
            return;
        }
        
        Room room = gameService.getRoom(roomCode);
        log.info("Player stunned: {} in room: {}", playerId, roomCode);
        
        // Отправляем обновленное состояние комнаты всем участникам
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.roomState(room)
        );
    }
    
    /**
     * Обновить текущую главу и часть викторины
     */
    @MessageMapping("/update-chapter")
    public void updateChapter(@Payload Map<String, Object> payload, Principal principal) {
        String sessionId = principal.getName();
        String roomCode = (String) payload.get("roomCode");
        
        // Получаем chapter и part (могут быть Integer или String)
        Integer chapter = null;
        Integer part = null;
        
        Object chapterObj = payload.get("chapter");
        if (chapterObj != null) {
            if (chapterObj instanceof Number) {
                chapter = ((Number) chapterObj).intValue();
            } else if (chapterObj instanceof String) {
                try {
                    chapter = Integer.parseInt((String) chapterObj);
                } catch (NumberFormatException e) {
                    sendError(sessionId, "Неверный формат chapter");
                    return;
                }
            }
        }
        
        Object partObj = payload.get("part");
        if (partObj != null) {
            if (partObj instanceof Number) {
                part = ((Number) partObj).intValue();
            } else if (partObj instanceof String) {
                try {
                    part = Integer.parseInt((String) partObj);
                } catch (NumberFormatException e) {
                    sendError(sessionId, "Неверный формат part");
                    return;
                }
            }
        }
        
        if (!gameService.isHost(roomCode, sessionId)) {
            sendError(sessionId, "Только хост комнаты может обновлять главу");
            return;
        }
        
        boolean success = gameService.updateChapter(roomCode, chapter, part, sessionId);
        if (!success) {
            sendError(sessionId, "Не удалось обновить главу");
            return;
        }
        
        Room room = gameService.getRoom(roomCode);
        log.info("Chapter updated: chapter={}, part={} in room: {}", chapter, part, roomCode);
        
        // Отправляем обновление главы всем участникам
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomCode,
            GameMessage.chapterUpdated(room)
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
