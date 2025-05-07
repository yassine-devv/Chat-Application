package com.example.chat_app.controller;

import com.example.chat_app.entities.Message;
import com.example.chat_app.entities.User;
import com.example.chat_app.service.RestService;
import com.example.chat_app.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RestChatController {

    @Autowired
    private UserService userService;

    @Autowired
    private RestService restService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(value = "/chatWithUser", produces = "application/json")
    public HashMap<String, Long> getChatWithUserId(@RequestBody String username, HttpSession session){

        //converto il json della request in hashmap
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String, String>>() {};

        HashMap<String, String> o = null;

        try {
            o = objectMapper.readValue(username, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        System.out.println(o.get("username"));

        //ricavo l'oggeto dello user attraverso lo username
        Optional<User> user = userService.findByUsername(o.get("username"));

        Long id = null;

        //ricavo l'id dello user
        if(user.isPresent()){
            var userObj = user.get();
            id = userObj.getId();
        }

        System.out.println("Id Sessione: "+String.valueOf(session.getAttribute("id")));
        System.out.println("Id: "+String.valueOf(id));

        List<Long> userIds = new ArrayList<>();

        userIds.add(Long.parseLong(String.valueOf(session.getAttribute("id"))));
        userIds.add(id);

        List<Long> arrWitIdChat = restService.chatsUtentiPartecipano(userIds);

        System.out.println(Arrays.asList(arrWitIdChat));

        HashMap<String, Long> response = new HashMap<String, Long>();

        response.put("id-chat", arrWitIdChat.get(0));

        return response;
    }

    @GetMapping(value = "/getAllChats", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> getAllChats(HttpSession session){
        List<Object[]> chatsFromService = restService.getAllChats(Long.parseLong(String.valueOf(session.getAttribute("id"))));

        List<HashMap<String, String>> allChats = restService.formatResponseAllChats(chatsFromService);

        System.out.println(Arrays.asList(allChats));

        String response = """
                <turbo-stream action='update' target="all-chats">
                    <template>
                """;

        if(!allChats.isEmpty()){
            for (HashMap<String, String> valuesSingleChat : allChats){
                response += """
                <div class="label-chat" id="%s" onclick="openChat(event)">
                    <span id="idUser-%s">%s</span>
                </div>
            """.formatted(valuesSingleChat.get("idChat"), valuesSingleChat.get("idUser"), valuesSingleChat.get("username_user"));
            }
        }else {
            response += """
                    <span>Nessuna chat presente con altri utenti</span>
                    """;
        }

        response += """
                    </template>
                </turbo-stream>
                """;

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(response);
    }

    @PostMapping(value = "/{idChat}", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> chat(@PathVariable String idChat, HttpSession session){
        //prendo l'altro partecipante della chat attraverso l'id della chat
        List<String> participants = restService.getParticipantsInChat(Long.parseLong(idChat));

        String usernameParticipant = null;

        for(String username : participants){
            if(!username.equals(session.getAttribute("username"))){
                usernameParticipant = username;
            }
        }

        //ricavo l'id dell'altro partecipante attraverso lo username
        Optional<User> user = userService.findByUsername(usernameParticipant);

        Long id = null;

        if(user.isPresent()){
            var userObj = user.get();
            id = userObj.getId();
        }

        List<Message> allMessagesInChat = restService.findMessagesByChatId(Long.parseLong(idChat));

        String response = """
                <turbo-stream action='update' target="area-chat">
                        <template>
                            <div class="header-username">
                                <span id="consumer" class="label-username">%s</span>
                            </div>
                            <div id="area-messages">
                """.formatted(usernameParticipant);

        for(Message message : allMessagesInChat){
            if(message.getProducer().getUsername().equals(usernameParticipant)){
                response += """
                    <div class="consumer-message">
                        <span>%s</span><br>
                    </div>
                    """.formatted(message.getContent());
            }

            if(message.getProducer().getUsername().equals(session.getAttribute("username"))){
                response += """
                    <div class="producer-message">
                        <span>%s</span><br>
                    </div>
                    """.formatted(message.getContent());
            }
        }

        response += """
                        </div>
                        <div class="area-send-message">
                            <form id="sendMessageForm" name="sendMessageForm">
                                <input type="text" id="message" placeholder="Scrivi un messaggio..." autocomplete="off" class="form-control"/>
                                <button type="submit" class="accent message-submit">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-send" viewBox="0 0 16 16">
                                      <path d="M15.854.146a.5.5 0 0 1 .11.54l-5.819 14.547a.75.75 0 0 1-1.329.124l-3.178-4.995L.643 7.184a.75.75 0 0 1 .124-1.33L15.314.037a.5.5 0 0 1 .54.11ZM6.636 10.07l2.761 4.338L14.13 2.576zm6.787-8.201L1.591 6.602l4.339 2.76z"/>
                                    </svg>
                                </button>
                            </form>
                        </div>
                        <script>
                            document.getElementById("area-messages").scrollTo(0, document.getElementById("area-messages").scrollHeight);
                            document.getElementById("sendMessageForm").onsubmit = function(e) {
                                e.preventDefault();
                                const message = document.getElementById("message").value.trim();
                                if (message && window.stompClient) {
                                    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                                        producerId: "%s",
                                        consumerId: "%s",
                                        content: message,
                                        typeMessage: "CHAT",
                                        chatId: "%s"
                                    }));
                                    document.getElementById("area-messages").innerHTML += `<div class="producer-message"><span>${message}</span><br></div>`;
                                    document.getElementById("area-messages").scrollTo(0, document.getElementById("area-messages").scrollHeight);
                                }
                                this.reset();
                            };
                        </script>
                    </template>
                </turbo-stream>
                """.formatted(String.valueOf(session.getAttribute("id")), String.valueOf(id), idChat);

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(response);
    }

    @PostMapping(value = "/getUsernameById/{producerId}", produces = "application/json")
    public List<String> getUsernameById(@PathVariable String producerId){
        Optional<User> user = restService.getUsernameById(Long.parseLong(producerId));

        String username = null;

        if(user.isPresent()){
            username = user.get().getUsername();
        }

        System.out.println(username);

        List<String> usernameList = new ArrayList<>();

        usernameList.add(username);

        return usernameList;
    }

}
