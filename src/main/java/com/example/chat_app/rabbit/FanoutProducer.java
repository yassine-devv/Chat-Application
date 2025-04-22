package com.example.chat_app.rabbit;


import com.example.chat_app.entities.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import static com.example.chat_app.config.RabbitMqConfig.EXCHANGE_FANOUT;

@Slf4j
@Service
public class FanoutProducer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private final RabbitTemplate rabbitTemplate;

    public FanoutProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(Message mess) {
        try {
            String json = objectMapper.writeValueAsString(mess);
            rabbitTemplate.convertAndSend(EXCHANGE_FANOUT, "", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
