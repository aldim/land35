package com.quizbattle.controller;

import com.quizbattle.dto.*;
import com.quizbattle.service.GameService;
import com.quizbattle.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final GameService gameService;
    
    public UserController(UserService userService, GameService gameService) {
        this.userService = userService;
        this.gameService = gameService;
    }
    
    /**
     * Регистрация нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            var user = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getFullName(),
                    request.getNickname(),
                    request.getAvatar()
            );
            return ResponseEntity.ok(new UserResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Авторизация пользователя
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var userOpt = userService.authenticateUser(request.getUsername(), request.getPassword());
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new UserResponse(userOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверное имя пользователя или пароль"));
    }
    
    /**
     * Получить список всех пользователей
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("users", users));
    }
    
    /**
     * Получить информацию о пользователе
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        var userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new UserResponse(userOpt.get()));
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Создать приглашение в комнату
     */
    @PostMapping("/invitations")
    public ResponseEntity<?> createInvitation(
            @RequestBody InvitationRequest request,
            @RequestParam Long hostUserId) {
        try {
            var invitation = userService.createInvitation(
                    request.getRoomCode(),
                    request.getInvitedUserId(),
                    hostUserId
            );
            return ResponseEntity.ok(new InvitationResponse(invitation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить список приглашений пользователя
     */
    @GetMapping("/{userId}/invitations")
    public ResponseEntity<?> getInvitations(@PathVariable Long userId) {
        try {
            List<InvitationResponse> invitations = userService.getPendingInvitationsForUser(userId)
                    .stream()
                    .map(InvitationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("invitations", invitations));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Принять приглашение
     */
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable Long invitationId,
            @RequestParam Long userId) {
        try {
            var invitation = userService.acceptInvitation(invitationId, userId);
            
            // Активируем комнату, если она еще не активна
            gameService.activateRoomFromDatabase(invitation.getRoom().getCode(), null);
            
            // Добавляем игрока в комнату
            var user = invitation.getInvitedUser();
            String playerName = user.getFullName(); // Используем полное имя вместо никнейма
            gameService.addPlayer(
                    invitation.getRoom().getCode(),
                    playerName,
                    user.getAvatar() != null ? user.getAvatar() : "",
                    user.getId()
            );
            
            return ResponseEntity.ok(new InvitationResponse(invitation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Отклонить приглашение
     */
    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(
            @PathVariable Long invitationId,
            @RequestParam Long userId) {
        try {
            var invitation = userService.rejectInvitation(invitationId, userId);
            return ResponseEntity.ok(new InvitationResponse(invitation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить приглашения для комнаты
     */
    @GetMapping("/rooms/{roomCode}/invitations")
    public ResponseEntity<?> getRoomInvitations(@PathVariable String roomCode) {
        try {
            List<InvitationResponse> invitations = userService.getInvitationsForRoom(roomCode)
                    .stream()
                    .map(InvitationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("invitations", invitations));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

