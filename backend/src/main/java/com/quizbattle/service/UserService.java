package com.quizbattle.service;

import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.model.entity.RoomInvitation;
import com.quizbattle.model.entity.User;
import com.quizbattle.repository.RoomInvitationRepository;
import com.quizbattle.repository.RoomRepository;
import com.quizbattle.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomInvitationRepository invitationRepository;
    
    public UserService(UserRepository userRepository, 
                      RoomRepository roomRepository, 
                      RoomInvitationRepository invitationRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.invitationRepository = invitationRepository;
    }
    
    /**
     * Регистрация нового пользователя
     */
    @Transactional
    public User registerUser(String username, String password, String fullName, String nickname, String avatar) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        
        User user = new User(username, password, fullName, nickname, avatar);
        return userRepository.save(user);
    }
    
    /**
     * Авторизация пользователя (проверка username и password)
     */
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Простая проверка пароля (в будущем можно добавить BCrypt)
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Получить пользователя по ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Получить пользователя по username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Получить всех пользователей
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Создать приглашение в комнату
     */
    @Transactional
    public RoomInvitation createInvitation(String roomCode, Long invitedUserId, Long hostUserId) {
        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        
        if (!room.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalArgumentException("Только хост может отправлять приглашения");
        }
        
        User invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        // Проверяем, не существует ли уже приглашение
        Optional<RoomInvitation> existingInvitation = 
                invitationRepository.findByRoomAndInvitedUser(room, invitedUser);
        
        if (existingInvitation.isPresent()) {
            RoomInvitation invitation = existingInvitation.get();
            // Если приглашение было отклонено или истекло, обновляем его статус
            if (invitation.getStatus() == RoomInvitation.InvitationStatus.REJECTED ||
                invitation.getStatus() == RoomInvitation.InvitationStatus.EXPIRED) {
                invitation.setStatus(RoomInvitation.InvitationStatus.PENDING);
                return invitationRepository.save(invitation);
            }
            throw new IllegalArgumentException("Приглашение уже существует");
        }
        
        RoomInvitation invitation = new RoomInvitation(room, invitedUser);
        return invitationRepository.save(invitation);
    }
    
    /**
     * Получить список приглашений пользователя
     */
    public List<RoomInvitation> getPendingInvitationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return invitationRepository.findByInvitedUserAndStatus(
                user, RoomInvitation.InvitationStatus.PENDING);
    }
    
    /**
     * Принять приглашение
     */
    @Transactional
    public RoomInvitation acceptInvitation(Long invitationId, Long userId) {
        RoomInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));
        
        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете принять это приглашение");
        }
        
        if (invitation.getStatus() != RoomInvitation.InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }
        
        invitation.setStatus(RoomInvitation.InvitationStatus.ACCEPTED);
        return invitationRepository.save(invitation);
    }
    
    /**
     * Отклонить приглашение
     */
    @Transactional
    public RoomInvitation rejectInvitation(Long invitationId, Long userId) {
        RoomInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));
        
        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете отклонить это приглашение");
        }
        
        if (invitation.getStatus() != RoomInvitation.InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }
        
        invitation.setStatus(RoomInvitation.InvitationStatus.REJECTED);
        return invitationRepository.save(invitation);
    }
    
    /**
     * Получить приглашения для комнаты
     */
    public List<RoomInvitation> getInvitationsForRoom(String roomCode) {
        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        return invitationRepository.findByRoom(room);
    }
    
    /**
     * Связать комнату с БД (используется при создании комнаты)
     */
    @Transactional
    public RoomEntity saveRoom(RoomEntity room) {
        return roomRepository.save(room);
    }
    
    /**
     * Получить комнату по коду
     */
    public Optional<RoomEntity> getRoomByCode(String code) {
        return roomRepository.findByCode(code);
    }
}

