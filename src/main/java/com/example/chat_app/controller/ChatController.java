package com.example.chat_app.controller;

import com.example.chat_app.config.RabbitMqConfig;
import com.example.chat_app.config.WebSocketConnectionsTracker;
import com.example.chat_app.entities.Message;
import com.example.chat_app.rabbit.FanoutProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Arrays;

import static com.example.chat_app.config.RabbitMqConfig.EXCHANGE_FANOUT;

@Controller
public class ChatController {

    //public static String USERNAME = null;

    @Autowired
    private ObjectMapper objectMapper;

    private final WebSocketConnectionsTracker userTracker;
    private final FanoutProducer fanoutProducer;
    private final RabbitTemplate rabbitTemplate;
    public static String CHAT_QUEUE = null;

    public ChatController(WebSocketConnectionsTracker userTracker, FanoutProducer fanoutProducer, RabbitTemplate rabbitTemplate) {
        this.userTracker = userTracker;
        this.fanoutProducer = fanoutProducer;
        this.rabbitTemplate = rabbitTemplate;
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println(chatMessage.getProducer());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getProducer());

        String username = chatMessage.getProducer();

        if(username != null){
            userTracker.addUser(username);
        }

        System.out.println(Arrays.asList(this.userTracker.getConnectedUsers()));

        fanoutProducer.sendMessage(new Message(userTracker.getConnectedUsers().toString(), "SERVER", "all", "JOIN"));
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            rabbitTemplate.convertAndSend("chat.queue", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
