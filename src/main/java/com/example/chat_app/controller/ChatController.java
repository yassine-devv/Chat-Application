package com.example.chat_app.controller;

import com.example.chat_app.config.RabbitMqConfig;
import com.example.chat_app.config.WebSocketConnectionsTracker;
import com.example.chat_app.entities.Message;
import com.example.chat_app.entities.MessageDTO;
import com.example.chat_app.entities.User;
import com.example.chat_app.rabbit.FanoutProducer;
import com.example.chat_app.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;

import static com.example.chat_app.config.RabbitMqConfig.EXCHANGE_FANOUT;

@Controller
public class ChatController {

    //public static String USERNAME = null;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;


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
    public void addUser(@Payload HashMap<String, String> chatMessage, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        System.out.println(principal.getName());

        System.out.println(chatMessage.get("producer"));
        headerAccessor.getSessionAttributes().put("username", chatMessage.get("producer"));

        String username = chatMessage.get("producer");

        if(username != null){
            userTracker.addUser(username);
        }

        HashMap<String, String> joinMessageToSend = new HashMap<>();

        joinMessageToSend.put("content", userTracker.getConnectedUsers().toString());
        joinMessageToSend.put("producer", "SERVER");
        joinMessageToSend.put("consumer", "all");
        joinMessageToSend.put("typeMessage", "JOIN");

        fanoutProducer.sendMessage(joinMessageToSend);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageDTO chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("Messaggio da chat controller: "+chatMessage.toString());
        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            rabbitTemplate.convertAndSend("chat.queue", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
