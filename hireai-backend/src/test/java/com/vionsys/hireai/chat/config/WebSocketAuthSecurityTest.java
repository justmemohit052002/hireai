package com.vionsys.hireai.chat.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.vionsys.hireai.security.CustomUserDetailsService;
import com.vionsys.hireai.security.jwt.JwtService;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthSecurityTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService);
    }

    @Test
    void whenConnectFrameHasNoAuthHeader_throwsAuthenticationCredentialsNotFoundException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                interceptor.preSend(message, channel)
        );
    }

    @Test
    void whenConnectFrameHasValidBearerToken_authenticatesUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer valid.jwt.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        UserDetails userDetails = new User("user@example.com", "pass", Collections.emptyList());
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
        StompHeaderAccessor resAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resAccessor.getUser());
    }
}
