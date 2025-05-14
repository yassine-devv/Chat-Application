package com.example.chat_app.controller;

import com.example.chat_app.config.WebSocketConnectionsTracker;
import com.example.chat_app.entities.*;
import com.example.chat_app.service.RestService;
import com.example.chat_app.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
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

    @Autowired
    private WebSocketConnectionsTracker webSocketConnectionsTracker;

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

        String[] arrColors = {"00CCCC","0066CC", "80FF00", "00FF00", "FF90CC", "CC6600"};

        Random randomNumbers = new Random();

        String response = """
                <turbo-stream action='update' target="all-chats">
                    <template>
                """;

        if(!allChats.isEmpty()){
            for (HashMap<String, String> valuesSingleChat : allChats){
                String firstCharUsername = String.valueOf(valuesSingleChat.get("username_user").toCharArray()[0]).toUpperCase();
                System.out.println(firstCharUsername);
                response += """
                <div class="label-chat" id="%s" onclick="openChat(event)">
                    <div class="image-profile" style="margin-right: 2%%; background-color: #%s;"><span>%s</span></div>
                    %s
                </div>
            """.formatted(valuesSingleChat.get("idChat"), arrColors[randomNumbers.nextInt(5)+1], firstCharUsername, valuesSingleChat.get("username_user"));
            }
        }/*else {
            response += """
                    <span>Nessuna chat presente con altri utenti</span>
                    """;
        }*/

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

    /*
    funzione che torna una lista con gli username degli utenti che hanno la chat con lo user connesso
    */
    @PostMapping(value = "/getUsersWithChat", produces = "application/json")
    private List<String> getUsersWithChat(HttpSession session){
        return restService.getUsersWithChat(Long.parseLong(String.valueOf(session.getAttribute("id"))));
    }

    @PostMapping(value = "/sendInvitation", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> sendInvitation(@RequestBody String idUserInvitee, HttpSession session){ // ritornare turbo che modifica il pulsante
        idUserInvitee = idUserInvitee.substring(1, idUserInvitee.length() - 1); //rimuovo i doppi apici

        System.out.println(idUserInvitee);

        Invitation saved = restService.saveInvitation(idUserInvitee, session);

        String html = """
                <turbo-stream action='replace' target="btn-invite-%s">
                    <template>
                    <button type="button" class="btn btn-primary btn-invite" id="btn-pending-invite-%s" disabled>
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-clock-history" viewBox="0 0 16 16">
                          <path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.022zm2.004.45a7 7 0 0 0-.985-.299l.219-.976q.576.129 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654l-.615.789a7 7 0 0 0-.418-.302zm1.834 1.79a7 7 0 0 0-.653-.796l.724-.69q.406.429.747.91zm.744 1.352a7 7 0 0 0-.214-.468l.893-.45a8 8 0 0 1 .45 1.088l-.95.313a7 7 0 0 0-.179-.483m.53 2.507a7 7 0 0 0-.1-1.025l.985-.17q.1.58.116 1.17zm-.131 1.538q.05-.254.081-.51l.993.123a8 8 0 0 1-.23 1.155l-.964-.267q.069-.247.12-.501m-.952 2.379q.276-.436.486-.908l.914.405q-.24.54-.555 1.038zm-.964 1.205q.183-.183.35-.378l.758.653a8 8 0 0 1-.401.432z"/>
                          <path d="M8 1a7 7 0 1 0 4.95 11.95l.707.707A8.001 8.001 0 1 1 8 0z"/>
                          <path d="M7.5 3a.5.5 0 0 1 .5.5v5.21l3.248 1.856a.5.5 0 0 1-.496.868l-3.5-2A.5.5 0 0 1 7 9V3.5a.5.5 0 0 1 .5-.5"/>
                        </svg>
                    </button>
                    <script>
                    if (window.stompClient) {
                        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                            producerId: "%s",
                            consumerId: "%s",
                            content: "%s ti ha mandato un invito!",
                            typeMessage: "INVITATION",
                            chatId: %s
                        }));
                    }
                    </script>
                    </template>
                </turbo-stream>
                """.formatted(idUserInvitee, idUserInvitee, String.valueOf(session.getAttribute("id")), idUserInvitee, String.valueOf(session.getAttribute("username")), saved.getId());

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }

    @PostMapping(value = "/getUsersConnected", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> getUsersConnected(HttpSession session){
        /*
        * prendere utenti da WebSocketConnectionsTracker
        * e formattare
        * */

        /*
            query per prendere lo status di invitation tra i due utenti
        select i.status from invitations i
        JOIN users inviter ON i.inviter = inviter.id
        JOIN users invitee ON i.invitee = invitee.id where i.inviter = 2 and i.invitee = 3 or i.inviter = 3 and i.invitee = 2;
           */

        String usernameSelf = String.valueOf(session.getAttribute("username"));
        String idUserSelf = String.valueOf(session.getAttribute("id"));

        Set<String> usersConnected = webSocketConnectionsTracker.getConnectedUsers(); //set con tutti gli users connessi

        List<String> usersWithChat = restService.getUsersWithChat(Long.valueOf(String.valueOf(session.getAttribute("id")))); //list con tutti gli users che hanno una chat con l'utente stessso

        String html = """
                <turbo-stream action='update' targets=".list-users-connected">
                    <template>
                """;

        System.out.println(Arrays.asList(usersConnected));
        System.out.println(Arrays.asList(usersWithChat));

        String[] arrColors = {"00CCCC","0066CC", "80FF00", "00FF00", "FF90CC", "CC6600"};
        Random randomNumbers = new Random();

        if(usersConnected.size()==1){ // se c'è solo l'utente connesso
            html += """
                    <span id="span-no-user-connected">Nessun utente &eacute; connesso</span>
                    """;
        }else{ // se ci sono altri utenti connessi
            for(String userConnected : usersConnected) {
                if (!userConnected.equals(usernameSelf)) {
                    String firstCharUsername = String.valueOf(userConnected.toCharArray()[0]).toUpperCase();
                    String randomColorAvatar = arrColors[randomNumbers.nextInt(5) + 1];

                    if(usersWithChat.contains(userConnected)){ //quando l'utente connesso [i] non ha una chat con l'utente
                        html += """
                            <div class="single-user">
                                <div class="image-profile" style="background-color: #%s;"><span>%s</span></div>
                                <span>%s</span>
                            </div>
                            """.formatted(randomColorAvatar, firstCharUsername, userConnected);
                    }else{

                        Long idCurrentUser = userService.findByUsername(userConnected).get().getId(); //estrae l'id dell'utente corrente nel for

                        List<Object[]> resultQueryInvitation = restService.getInvitation(Long.parseLong(idUserSelf), idCurrentUser); //prende lo status degli inviti tra i due utenti

                        if(!resultQueryInvitation.isEmpty()){
                            Object[] invitation = resultQueryInvitation.get(0); //il risultato della query si trova nel primo elemento della lista

                            //se c'è un invito e il suo status è ancora in attesa
                            if((invitation[3].toString()).equals("PENDING")){
                                // se lo user[i] è l'invitato o l'invitate
                                if(userConnected.equals(invitation[1].toString())){
                                    html += """
                                    <div class="single-user">
                                        <div class="image-profile" style="background-color: #%s;"><span>%s</span></div>
                                        <span>%s</span>
                                    
                                        <button type="button" class="btn btn-primary btn-invite" id="btn-pending-invite-%s" disabled>
                                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-clock-history" viewBox="0 0 16 16">
                                              <path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.022zm2.004.45a7 7 0 0 0-.985-.299l.219-.976q.576.129 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654l-.615.789a7 7 0 0 0-.418-.302zm1.834 1.79a7 7 0 0 0-.653-.796l.724-.69q.406.429.747.91zm.744 1.352a7 7 0 0 0-.214-.468l.893-.45a8 8 0 0 1 .45 1.088l-.95.313a7 7 0 0 0-.179-.483m.53 2.507a7 7 0 0 0-.1-1.025l.985-.17q.1.58.116 1.17zm-.131 1.538q.05-.254.081-.51l.993.123a8 8 0 0 1-.23 1.155l-.964-.267q.069-.247.12-.501m-.952 2.379q.276-.436.486-.908l.914.405q-.24.54-.555 1.038zm-.964 1.205q.183-.183.35-.378l.758.653a8 8 0 0 1-.401.432z"/>
                                              <path d="M8 1a7 7 0 1 0 4.95 11.95l.707.707A8.001 8.001 0 1 1 8 0z"/>
                                              <path d="M7.5 3a.5.5 0 0 1 .5.5v5.21l3.248 1.856a.5.5 0 0 1-.496.868l-3.5-2A.5.5 0 0 1 7 9V3.5a.5.5 0 0 1 .5-.5"/>
                                            </svg>
                                        </button>
                                    </div>
                                    """.formatted(randomColorAvatar, firstCharUsername, userConnected, idCurrentUser);
                                }else{
                                    html += """
                                        <div class="single-user">
                                            <div class="image-profile" style="background-color: #%s;"><span>%s</span></div>
                                            <span>%s</span>
                                        </div>
                                        """.formatted(randomColorAvatar, firstCharUsername, userConnected);
                                }
                            }
                        }else{
                            //nel caso in cui non c'è nessun invito in pending e non hanno una chat
                            html += """
                                    <div class="single-user">
                                        <div class="image-profile" style="background-color: #%s;"><span>%s</span></div>
                                        <span>%s</span>
                                    
                                        <button type="button" class="btn btn-primary btn-invite" id="btn-invite-%s" onclick="sendInvitation('%s')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-person-fill-add" viewBox="0 0 16 16">
                                              <path d="M12.5 16a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7m.5-5v1h1a.5.5 0 0 1 0 1h-1v1a.5.5 0 0 1-1 0v-1h-1a.5.5 0 0 1 0-1h1v-1a.5.5 0 0 1 1 0m-2-6a3 3 0 1 1-6 0 3 3 0 0 1 6 0"/>
                                              <path d="M2 13c0 1 1 1 1 1h5.256A4.5 4.5 0 0 1 8 12.5a4.5 4.5 0 0 1 1.544-3.393Q8.844 9.002 8 9c-5 0-6 3-6 4"/>
                                            </svg>
                                        </button>
                                    </div>
                                    """.formatted(randomColorAvatar, firstCharUsername, userConnected, idCurrentUser, idCurrentUser);
                        }
                    }

                }
            }
        }

        html += """
                    </template>
                </turbo-stream>
                """;

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }

    @PostMapping(value = "/actionOnInvitation", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> actionOnInvitation(@RequestBody String json, HttpSession session){
        //converto il json che mi arriva in stringa lo converto in hashmap
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>(){};

        HashMap<String, String> jsonToMap = null;

        String html = null;

        try {
            jsonToMap = objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if(jsonToMap != null){
            Long idInvitation = Long.parseLong(jsonToMap.get("id_invitation"));
            boolean result = Boolean.parseBoolean(jsonToMap.get("result"));
            Long idCurrentUser = Long.parseLong(jsonToMap.get("id_current_user"));

            int resultUpdate = 0;

            //se la richiesta di invito è stata accettata
            if(result){
                resultUpdate = restService.saveInvitation(idInvitation, "ACCEPTED");

                Chat chatSaved = restService.saveChat();
                System.out.println(chatSaved.getId());

                Long idChat = chatSaved.getId();
                int insertParticipants = restService.addParticipantsToChat(idChat, Long.parseLong(String.valueOf(session.getAttribute("id"))), idCurrentUser);

                List<String> countPendingInvitations = restService.getCountPendingInvitations(Long.parseLong(String.valueOf(session.getAttribute("id"))));

                System.out.println("Chat creata con successo!");

                html = """
                            <turbo-stream action="replace" target="label-count-invitations">
                                <template>
                                <span id="label-count-invitations">%s</span>
                                <script>
                                    if (window.stompClient) {
                                        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                                            producerId: "%s",
                                            consumerId: "%s",
                                            content: "%s ha accettato l'invito!",
                                            typeMessage: "ACCEPTED",
                                            chatId: null
                                        }));
                                    }
                                </script>
                            </template>
                            </turbo-stream>
                            <turbo-stream action="remove" target="invitation-%s"></turbo-stream>
                            <turbo-stream action="remove" target="btn-pending-invite-%s"></turbo-stream>
                            """.formatted(countPendingInvitations.get(0), String.valueOf(session.getAttribute("id")), idCurrentUser, String.valueOf(session.getAttribute("username")), String.valueOf(idInvitation), idCurrentUser);
            }
        }

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }

    @GetMapping(value = "/getCountPendingInvitations", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> editCountPendingInvitation(HttpSession session){
        List<String> resultCountPendingInvitations = restService.getCountPendingInvitations(Long.parseLong(String.valueOf(session.getAttribute("id"))));

        String html = """
                <turbo-stream action='replace' target="label-count-invitations">
                    <template>
                    <span id="label-count-invitations">%s</span>
                    </template>
                </turbo-stream>
                """.formatted(resultCountPendingInvitations.get(0));

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }

    @GetMapping(value = "/getAllInvitations", produces = "text/vnd.turbo-stream.html")
    public ResponseEntity<String> getAllPendingInvitations(HttpSession session){
        List<Object[]> resultAllInvitations = restService.getAllPendingInvitations(Long.parseLong(String.valueOf(session.getAttribute("id"))));
        List<HashMap<String, String>> allInvitations = restService.formatResultAllInvitations(resultAllInvitations);

        System.out.println(Arrays.asList(allInvitations));

        String html = """
                <turbo-stream action='update' targets=".list-all-invitations">
                    <template>
                """;

        String[] arrColors = {"00CCCC","0066CC", "80FF00", "00FF00", "FF90CC", "CC6600"};
        Random randomNumbers = new Random();

        for(HashMap<String, String> singleInvitation : allInvitations){
            String firstCharUsername = String.valueOf(singleInvitation.get("username_inviter").toCharArray()[0]).toUpperCase();
            String randomColorAvatar = arrColors[randomNumbers.nextInt(5) + 1];
            html += """
                    <div class="single-user" id="invitation-%s">
                        <div class="image-profile" style="background-color: #%s;"><span>%s</span></div>
                        <span>%s</span>
                        <div class="sec-buttons-action-invitation" id="%s">
                            <button type="button" class="btn-action-invite" onclick="actionOnInvitation('%s', true, '%s')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="green" class="bi bi-check-lg" viewBox="0 0 16 16">
                                  <path d="M12.736 3.97a.733.733 0 0 1 1.047 0c.286.289.29.756.01 1.05L7.88 12.01a.733.733 0 0 1-1.065.02L3.217 8.384a.757.757 0 0 1 0-1.06.733.733 0 0 1 1.047 0l3.052 3.093 5.4-6.425z"/>
                                </svg>
                            </button>
                            <button type="button" class="btn-action-invite" onclick="actionOnInvitation('%s', false, '%s')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="red" class="bi bi-x-lg" viewBox="0 0 16 16">
                                  <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                    """.formatted(singleInvitation.get("id_invitation"), randomColorAvatar, firstCharUsername, singleInvitation.get("username_inviter"), singleInvitation.get("id_invitation"), singleInvitation.get("id_invitation"), singleInvitation.get("id_inviter"), singleInvitation.get("id_invitation"), singleInvitation.get("id_inviter"));
        }

        html += """
                    </template>
                </turbo-stream>
                """;

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }
}
