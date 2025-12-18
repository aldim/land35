package com.quizbattle.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private String code;
    private String hostSessionId;
    private List<Player> players;
    private GameState gameState;
    private String winnerId;
    private Long roundStartTime;
    private List<ButtonPress> buttonPresses;
    
    public static final int MAX_PLAYERS = 20;
    
    public Room() {}
    
    public Room(String code, String hostSessionId) {
        this.code = code;
        this.hostSessionId = hostSessionId;
        this.players = new CopyOnWriteArrayList<>();
        this.gameState = GameState.WAITING;
        this.buttonPresses = new CopyOnWriteArrayList<>();
    }
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getHostSessionId() { return hostSessionId; }
    public void setHostSessionId(String hostSessionId) { this.hostSessionId = hostSessionId; }
    
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
    
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public Long getRoundStartTime() { return roundStartTime; }
    public void setRoundStartTime(Long roundStartTime) { this.roundStartTime = roundStartTime; }
    
    public List<ButtonPress> getButtonPresses() { return buttonPresses; }
    public void setButtonPresses(List<ButtonPress> buttonPresses) { this.buttonPresses = buttonPresses; }
    
    // Business methods
    public boolean addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS) {
            return false;
        }
        players.add(player);
        return true;
    }
    
    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
    }
    
    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
    
    public Player getPlayerBySessionId(String sessionId) {
        return players.stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .findFirst()
                .orElse(null);
    }
    
    public void startRound() {
        this.gameState = GameState.ACTIVE;
        this.winnerId = null;
        this.roundStartTime = System.currentTimeMillis();
        this.buttonPresses.clear();
    }
    
    public void endRound() {
        this.gameState = GameState.ROUND_ENDED;
    }
    
    public void resetRound() {
        this.gameState = GameState.WAITING;
        this.winnerId = null;
        this.roundStartTime = null;
        this.buttonPresses.clear();
    }
    
    public synchronized ButtonPress registerButtonPress(String playerId) {
        boolean alreadyPressed = buttonPresses.stream()
                .anyMatch(bp -> bp.getPlayerId().equals(playerId));
        
        if (alreadyPressed) {
            return null;
        }
        
        long pressTime = System.currentTimeMillis();
        int position = buttonPresses.size() + 1;
        
        ButtonPress press = new ButtonPress(playerId, pressTime, position);
        buttonPresses.add(press);
        
        if (position == 1) {
            this.winnerId = playerId;
            this.gameState = GameState.ROUND_ENDED;
        }
        
        return press;
    }
}
