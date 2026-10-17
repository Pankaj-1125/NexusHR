package com.nexushr.config;

import com.nexushr.security.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;

import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${nexushr.cors.allowed-origins:http://localhost}")
    private String allowedOrigins;

    public WebSocketConfig(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {
        registry.enableSimpleBroker(
                "/queue",
                "/topic"
        );

        registry.setApplicationDestinationPrefixes(
                "/app"
        );

        registry.setUserDestinationPrefix(
                "/user"
        );
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        String[] origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toArray(String[]::new);

        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {

        registration.interceptors(
                new ChannelInterceptor() {

                    @Override
                    public Message<?> preSend(
                            Message<?> message,
                            MessageChannel channel
                    ) {

                        StompHeaderAccessor accessor =
                                MessageHeaderAccessor.getAccessor(
                                        message,
                                        StompHeaderAccessor.class
                                );

                        if (accessor == null) {
                            return message;
                        }

                        if (StompCommand.CONNECT.equals(
                                accessor.getCommand()
                        )) {

                            String authorization =
                                    accessor.getFirstNativeHeader(
                                            "Authorization"
                                    );

                            if (authorization == null
                                    || !authorization.startsWith(
                                    "Bearer "
                            )) {
                                throw new MessagingException(
                                        "WebSocket authentication required"
                                );
                            }

                            String token =
                                    authorization.substring(7);

                            try {

                                String username =
                                        jwtService.extractUsername(
                                                token
                                        );

                                UserDetails userDetails =
                                        userDetailsService
                                                .loadUserByUsername(
                                                        username
                                                );

                                if (!jwtService.isTokenValid(
                                        token,
                                        userDetails
                                )) {
                                    throw new MessagingException(
                                            "Invalid WebSocket token"
                                    );
                                }

                                UsernamePasswordAuthenticationToken
                                        authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities()
                                        );

                                accessor.setUser(
                                        authentication
                                );

                            } catch (Exception exception) {

                                throw new MessagingException(
                                        "WebSocket authentication failed",
                                        exception
                                );
                            }
                        }

                        return message;
                    }
                }
        );
    }
}