package com.workflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private TaskScheduler messageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setPoolSize(1);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic")
                .setHeartbeatValue(new long[] { 30000, 30000 })
                .setTaskScheduler(messageBrokerTaskScheduler());

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] originsArray = allowedOrigins.split("\\s*,\\s*");

        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns(originsArray)
                .withSockJS();
    }

    private AuthorizationManager<Message<?>> messageAuthorizationManager() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                .nullDestMatcher().authenticated() // Requires auth for CONNECT, DISCONNECT, etc.
                .simpSubscribeDestMatchers("/user/queue/errors").permitAll()
                .simpDestMatchers("/app/**").authenticated()
                .simpSubscribeDestMatchers("/user/**").authenticated()
                .anyMessage().denyAll()
                .build();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 1. FIRST: Extract JWT from STOMP headers and populate accessor.setUser()
        registration.interceptors(jwtChannelInterceptor);

        // 2. SECOND: Propagate the Authentication to Spring Security's Context
        registration.interceptors(new SecurityContextChannelInterceptor());

        // 3. THIRD: Enforce the Authorization rules
        registration.interceptors(new AuthorizationChannelInterceptor(messageAuthorizationManager()));
    }
}