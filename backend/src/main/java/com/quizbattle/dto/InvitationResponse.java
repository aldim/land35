package com.quizbattle.dto;

import com.quizbattle.model.entity.RoomInvitation;
import com.quizbattle.model.entity.User;

public class InvitationResponse {
    private Long id;
    private String roomCode;
    private UserInfo invitedUser;
    private String status;
    private String createdAt;
    
    public InvitationResponse() {}
    
    public InvitationResponse(RoomInvitation invitation) {
        this.id = invitation.getId();
        this.roomCode = invitation.getRoom().getCode();
        this.invitedUser = new UserInfo(invitation.getInvitedUser());
        this.status = invitation.getStatus().name();
        this.createdAt = invitation.getCreatedAt().toString();
    }
    
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
        private String nickname;
        private String avatar;
        
        public UserInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.fullName = user.getFullName();
            this.nickname = user.getNickname();
            this.avatar = user.getAvatar();
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
    
    public UserInfo getInvitedUser() {
        return invitedUser;
    }
    
    public void setInvitedUser(UserInfo invitedUser) {
        this.invitedUser = invitedUser;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

