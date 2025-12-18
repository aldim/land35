package com.quizbattle.model;

import java.util.List;

public class GameMessage {
    private MessageType type;
    private String roomCode;
    private String playerId;
    private String playerName;
    private String avatar;
    private GameState gameState;
    private List<Player> players;
    private String winnerId;
    private String winnerName;
    private String winnerAvatar;
    private List<ButtonPress> buttonPresses;
    private String error;
    
    public GameMessage() {}
    
    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }
    
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    
    public String getWinnerAvatar() { return winnerAvatar; }
    public void setWinnerAvatar(String winnerAvatar) { this.winnerAvatar = winnerAvatar; }
    
    public List<ButtonPress> getButtonPresses() { return buttonPresses; }
    public void setButtonPresses(List<ButtonPress> buttonPresses) { this.buttonPresses = buttonPresses; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    // Factory methods
    public static GameMessage roomCreated(String roomCode) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ROOM_CREATED);
        msg.setRoomCode(roomCode);
        return msg;
    }
    
    public static GameMessage playerJoined(Room room, Player player) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.PLAYER_JOINED);
        msg.setRoomCode(room.getCode());
        msg.setPlayerId(player.getId());
        msg.setPlayerName(player.getName());
        msg.setAvatar(player.getAvatar());
        msg.setPlayers(room.getPlayers());
        msg.setGameState(room.getGameState());
        return msg;
    }
    
    public static GameMessage playerLeft(Room room, Player player) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.PLAYER_LEFT);
        msg.setRoomCode(room.getCode());
        msg.setPlayerId(player.getId());
        msg.setPlayerName(player.getName());
        msg.setPlayers(room.getPlayers());
        return msg;
    }
    
    public static GameMessage roundStarted(Room room) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ROUND_STARTED);
        msg.setRoomCode(room.getCode());
        msg.setGameState(room.getGameState());
        return msg;
    }
    
    public static GameMessage buttonPressed(Room room, Player player, ButtonPress press) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.BUTTON_PRESSED);
        msg.setRoomCode(room.getCode());
        msg.setPlayerId(player.getId());
        msg.setPlayerName(player.getName());
        msg.setAvatar(player.getAvatar());
        msg.setGameState(room.getGameState());
        msg.setButtonPresses(room.getButtonPresses());
        
        if (room.getWinnerId() != null) {
            Player winner = room.getPlayerById(room.getWinnerId());
            if (winner != null) {
                msg.setWinnerId(winner.getId());
                msg.setWinnerName(winner.getName());
                msg.setWinnerAvatar(winner.getAvatar());
            }
        }
        
        return msg;
    }
    
    public static GameMessage roundEnded(Room room) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ROUND_ENDED);
        msg.setRoomCode(room.getCode());
        msg.setGameState(room.getGameState());
        msg.setButtonPresses(room.getButtonPresses());
        
        if (room.getWinnerId() != null) {
            Player winner = room.getPlayerById(room.getWinnerId());
            if (winner != null) {
                msg.setWinnerId(winner.getId());
                msg.setWinnerName(winner.getName());
                msg.setWinnerAvatar(winner.getAvatar());
            }
        }
        
        return msg;
    }
    
    public static GameMessage roundReset(Room room) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ROUND_RESET);
        msg.setRoomCode(room.getCode());
        msg.setGameState(room.getGameState());
        msg.setPlayers(room.getPlayers());
        return msg;
    }
    
    public static GameMessage roomState(Room room) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ROOM_STATE);
        msg.setRoomCode(room.getCode());
        msg.setGameState(room.getGameState());
        msg.setPlayers(room.getPlayers());
        msg.setButtonPresses(room.getButtonPresses());
        
        if (room.getWinnerId() != null) {
            Player winner = room.getPlayerById(room.getWinnerId());
            if (winner != null) {
                msg.setWinnerId(winner.getId());
                msg.setWinnerName(winner.getName());
                msg.setWinnerAvatar(winner.getAvatar());
            }
        }
        
        return msg;
    }
    
    public static GameMessage error(String errorMessage) {
        GameMessage msg = new GameMessage();
        msg.setType(MessageType.ERROR);
        msg.setError(errorMessage);
        return msg;
    }
}
