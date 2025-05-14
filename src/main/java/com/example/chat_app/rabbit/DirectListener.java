package com.example.chat_app.rabbit;
import com.example.chat_app.entities.MessageDTO;
import com.example.chat_app.entities.User;
import com.example.chat_app.service.RestService;
import com.example.chat_app.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class DirectListener {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RestService restService;

    @RabbitListener(queues = "chat.queue")
    public void listenOnQueueDefault(String json) {
        try {
            System.out.println("JSON: "+json);

            MessageDTO message = objectMapper.readValue(json, MessageDTO.class); // mapping del json nel messagedto class

            System.out.println(message.toString());

            String consumerUsername = null;

            if(message.getConsumerId() != null){
                Optional<User> userFounded = userService.findById(message.getConsumerId()); //ricavo lo user con l'id

                if(userFounded.isPresent()){ //se è stato trovato lo user
                    consumerUsername = userFounded.get().getUsername(); //prendo lo username
                }
            }

            if(consumerUsername != null){
                String topic = "/topic/user." + consumerUsername; //topic con l'username del consumer

                messagingTemplate.convertAndSend(topic, message);

                if(message.getTypeMessage().equals("CHAT")){
                    restService.saveMessage(message);
                }
            }

            System.out.println("Messaggio ricevuto: "+message.toString());

        } catch (JsonProcessingException e) {
            System.out.println("Errore nel parsing del messaggio JSON");
        }
    }

}
