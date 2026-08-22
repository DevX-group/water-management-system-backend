package com.backend.water_management_system.internal_chat.config;

import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
/**
 * Configures the STOMP endpoint, broker destinations, and CONNECT-time JWT
 * authentication.
 */
public class InternalChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    /** Enables broker destinations used for conversation topics and user queues. */
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    /**
     * Registers the browser endpoint used to establish the internal-chat WebSocket
     * connection.
     */
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/internal-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    /** Validates the bearer token before allowing a STOMP client to connect. */
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
                    return message;
                }

                String authorization = accessor.getFirstNativeHeader("Authorization");
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Missing or invalid JWT for STOMP connection.");
                }

                String token = authorization.substring(7);
                // The existing JWT and user-details services remain the source of
                // authentication truth.
                if (!jwtService.isTokenValid(token)) {
                    throw new IllegalArgumentException("Invalid or expired JWT for STOMP connection.");
                }

                String nic = jwtService.extractNic(token);
                UserPrincipal principal = (UserPrincipal) customUserDetailsService.loadUserByUsername(nic);
                if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
                    throw new IllegalArgumentException("User account is not allowed to access internal chat.");
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                        null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);
                return message;
            }
        });
    }
}
