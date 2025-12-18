package com.quizbattle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Клиенты подписываются на /topic для broadcast и /queue для персональных сообщений
        config.enableSimpleBroker("/topic", "/queue");
        // Клиенты отправляют сообщения на /app
        config.setApplicationDestinationPrefixes("/app");
        // Префикс для персональных сообщений (/user/{sessionId}/queue/...)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint для подключения
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new WebSocketHandshakeHandler())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

