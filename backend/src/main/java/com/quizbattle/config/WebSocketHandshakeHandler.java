package com.quizbattle.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {
    
    @Override
    protected Principal determineUser(ServerHttpRequest request, 
                                       WebSocketHandler wsHandler, 
                                       Map<String, Object> attributes) {
        // Генерируем уникальный ID для каждого подключения
        String id = UUID.randomUUID().toString();
        return new StompPrincipal(id);
    }
}


