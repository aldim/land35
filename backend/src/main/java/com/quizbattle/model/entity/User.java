package com.quizbattle.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password; // В будущем можно добавить шифрование
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "avatar", length = 500)
    private String avatar; // URL или путь к аватарке
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private UserRole role = UserRole.PLAYER; // По умолчанию игрок
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team; // Команда игрока
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public User() {}
    
    public User(String username, String password, String fullName, String nickname, String avatar) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = UserRole.PLAYER;
    }
    
    public User(String username, String password, String fullName, String nickname, String avatar, UserRole role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Team getTeam() {
        return team;
    }
    
    public void setTeam(Team team) {
        this.team = team;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

