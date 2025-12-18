package com.quizbattle.controller;

import com.quizbattle.model.Room;
import com.quizbattle.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
