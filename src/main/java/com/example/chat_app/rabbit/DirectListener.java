package com.example.chat_app.rabbit;
import com.example.chat_app.entities.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DirectListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "chat.queue")
    public void listenOnQueueDefault(String json) throws JsonProcessingException {
        try {
            Message message = objectMapper.readValue(json, Message.class);

            String topic = "/topic/user." + message.getConsumer();
            messagingTemplate.convertAndSend(topic, message);

            System.out.println("Messaggio ricevuto: "+message.toString());

        } catch (JsonProcessingException e) {
            System.out.println("Errore nel parsing del messaggio JSON");
        }
    }

}
