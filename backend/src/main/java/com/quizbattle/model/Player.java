package com.quizbattle.model;

public class Player {
    private String id;
    private String name;
    private String avatar;
    private String sessionId;
    private boolean connected;
    private Integer teamId; // Номер команды (1-4)
    private boolean stunned; // Оглушен ли игрок (не может нажимать кнопку в текущем раунде)
    
    public Player() {}
    
    public Player(String id, String name, String avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.connected = false;
    }
    
    public Player(String id, String name, String avatar, Integer teamId) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.teamId = teamId;
        this.connected = false;
    }
    
    public Player(String id, String name, String avatar, String sessionId, boolean connected) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.sessionId = sessionId;
        this.connected = connected;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    
    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }
    
    public boolean isStunned() { return stunned; }
    public void setStunned(boolean stunned) { this.stunned = stunned; }
}
