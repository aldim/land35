package com.quizbattle.controller;

import com.quizbattle.model.*;
import com.quizbattle.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api")
public class RestController {
    
    private final GameService gameService;
    
    public RestController(GameService gameService) {
        this.gameService = gameService;
    }
    
    /**
     * Проверить существование комнаты
     */
    @GetMapping("/room/{code}")
    public ResponseEntity<?> checkRoom(@PathVariable String code) {
        Room room = gameService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "code", room.getCode(),
            "playerCount", room.getPlayers().size(),
            "maxPlayers", Room.MAX_PLAYERS
        ));
    }
    
    /**
     * Получить состояние комнаты для экрана
     */
    @GetMapping("/room/{code}/state")
    public ResponseEntity<?> getRoomState(@PathVariable String code) {
        Room room = gameService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Создаем GameMessage с состоянием комнаты
        GameMessage roomState = GameMessage.roomState(room);
        
        // Конвертируем в Map для JSON ответа
        Map<String, Object> response = new HashMap<>();
        response.put("type", roomState.getType() != null ? roomState.getType().toString() : "ROOM_STATE");
        response.put("roomCode", roomState.getRoomCode() != null ? roomState.getRoomCode() : code);
        response.put("gameState", roomState.getGameState() != null ? roomState.getGameState().toString() : "WAITING");
        response.put("players", roomState.getPlayers() != null ? roomState.getPlayers() : java.util.Collections.emptyList());
        response.put("winnerId", roomState.getWinnerId() != null ? roomState.getWinnerId() : "");
        response.put("winnerName", roomState.getWinnerName() != null ? roomState.getWinnerName() : "");
        response.put("winnerAvatar", roomState.getWinnerAvatar() != null ? roomState.getWinnerAvatar() : "");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить список доступных аватаров
     */
    @GetMapping("/avatars")
    public ResponseEntity<?> getAvatars() {
        return ResponseEntity.ok(Map.of("avatars", gameService.getAvailableAvatars()));
    }
    
    /**
     * Проверить существование игрока в комнате
     */
    @GetMapping("/room/{code}/player/{playerId}")
    public ResponseEntity<?> checkPlayer(@PathVariable String code, @PathVariable String playerId) {
        Room room = gameService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        var player = room.getPlayerById(playerId);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "id", player.getId(),
            "name", player.getName(),
            "avatar", player.getAvatar(),
            "connected", player.isConnected()
        ));
    }
}
