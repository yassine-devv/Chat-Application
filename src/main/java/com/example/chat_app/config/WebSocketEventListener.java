package com.example.chat_app.config;

import com.example.chat_app.entities.Message;
import com.example.chat_app.rabbit.FanoutProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WebSocketEventListener {


    private final WebSocketConnectionsTracker userTracker;
    private final FanoutProducer fanoutProducer;

    public WebSocketEventListener(WebSocketConnectionsTracker userTracker, FanoutProducer fanoutProducer) {
        this.userTracker = userTracker;
        this.fanoutProducer = fanoutProducer;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("Nuova connessione");
    }


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            userTracker.removeUser(username);
        }

        HashMap<String, String> leftMessageToSend = new HashMap<>();

        leftMessageToSend.put("content", userTracker.getConnectedUsers().toString());
        leftMessageToSend.put("producer", "SERVER");
        leftMessageToSend.put("consumer", "all");
        leftMessageToSend.put("typeMessage", "LEFT");

        fanoutProducer.sendMessage(leftMessageToSend);

        System.out.println(username+" si è disconnesso");
    }
}