package com.vionsys.hireai.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        // Register standard endpoint with SockJS fallback
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins)
                .withSockJS();

        // Also register raw WebSocket endpoint for clients that do not use SockJS
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destination prefix for messages routed from client to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // In-memory message broker for pub/sub subscriptions
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for user-specific queues (e.g. /user/{userId}/queue/messages)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Attach security interceptor to validate JWT token on connection
        registration.interceptors(webSocketAuthInterceptor);
    }
}
