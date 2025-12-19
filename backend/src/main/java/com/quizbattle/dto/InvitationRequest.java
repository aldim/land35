package com.quizbattle.dto;

public class InvitationRequest {
    private String roomCode;
    private Long invitedUserId;
    
    public InvitationRequest() {}
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
    
    public Long getInvitedUserId() {
        return invitedUserId;
    }
    
    public void setInvitedUserId(Long invitedUserId) {
        this.invitedUserId = invitedUserId;
    }
}

