package com.example.iot_monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/iot-websocket")
                .setAllowedOriginPatterns("*") // Em desenvolvimento
                .withSockJS(); // Habilitar SockJS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Ative um broker de mensagens simples
        registry.enableSimpleBroker(
                        "/topic",   // Para broadcast
                        "/queue",   // Para mensagens ponto-a-ponto
                        "/user"     // Para mensagens privadas
                )
                .setTaskScheduler(heartbeatScheduler())
                .setHeartbeatValue(new long[]{5000, 5000});  // Heartbeat a cada 5 segundos

        // Prefixo para mensagens direcionadas a métodos @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Prefixo para mensagens privadas
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setErrorHandler(t -> {
            System.err.println("Erro no agendador de heartbeat: " + t.getMessage());
        });
        scheduler.initialize();
        return scheduler;
    }
}