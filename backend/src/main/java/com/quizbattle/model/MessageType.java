package com.quizbattle.model;

public enum MessageType {
    // От сервера
    ROOM_CREATED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    ROUND_STARTED,
    BUTTON_PRESSED,
    ROUND_RESET,
    ROOM_STATE,
    ERROR,
    
    // От клиента
    CREATE_ROOM,
    JOIN_ROOM,
    ADD_PLAYER,
    REMOVE_PLAYER,
    START_ROUND,
    PRESS_BUTTON,
    RESET_ROUND
}


