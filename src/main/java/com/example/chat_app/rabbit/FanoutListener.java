package com.example.chat_app.rabbit;

import com.example.chat_app.entities.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.example.chat_app.config.RabbitMqConfig.QUEUE_NAME;

@Slf4j
@Service
public class FanoutListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = {QUEUE_NAME})
    public void listenOnQueueDefault(String json) throws JsonProcessingException {
        Message message = objectMapper.readValue(json, Message.class);

        messagingTemplate.convertAndSend("/topic/public", message);
    }

}
