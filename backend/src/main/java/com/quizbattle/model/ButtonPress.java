package com.quizbattle.model;

public class ButtonPress {
    private String playerId;
    private long timestamp;
    private int position;
    
    public ButtonPress() {}
    
    public ButtonPress(String playerId, long timestamp, int position) {
        this.playerId = playerId;
        this.timestamp = timestamp;
        this.position = position;
    }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
