package com.example.chat_app.service;

import com.example.chat_app.entities.*;
import com.example.chat_app.repository.ChatRepository;
import com.example.chat_app.repository.InvitationRepository;
import com.example.chat_app.repository.MessageRepository;
import com.example.chat_app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Service
public class RestService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    public List<Long> chatsUtentiPartecipano(List<Long> userIds) {
        String sql = """
                SELECT p.id_chat FROM participants p
                WHERE p.id_user IN :userIds GROUP BY p.id_chat
                HAVING COUNT(DISTINCT p.id_user) = :userCount
                """;

        List<Long> chatIds = entityManager.createNativeQuery(sql)
                .setParameter("userIds", userIds)
                .setParameter("userCount", userIds.size())
                .getResultList();
        return chatIds;
    }

    public List<Message> findMessagesByChatId(Long id){
        return messageRepository.findByChatId(id);
    }

    public List<String> getParticipantsInChat(Long idChat){
        String sql = "SELECT u.username FROM users u INNER JOIN participants p on u.id = p.id_user WHERE p.id_chat = :idChat";

        List<String> chat = entityManager.createNativeQuery(sql)
                .setParameter("idChat", idChat)
                .getResultList();

        return chat;
    }

    public Message saveMessage(MessageDTO messageDTO){
        //ricavo l'oggeto user producer con id
        Optional<User> userProducer = userService.findById(messageDTO.getProducerId());

        User producer = null;

        if(userProducer.isPresent()){
            producer = userProducer.get();
        }

        //ricavo l'oggeto user consumer con id
        Optional<User> userConsumer = userService.findById(messageDTO.getConsumerId());

        User consumer = null;

        if(userProducer.isPresent()){
            consumer = userConsumer.get();
        }

        Optional<Chat> chatOpt = chatRepository.findById(messageDTO.getChatId());

        Chat chat = null;

        if(chatOpt.isPresent()){
            chat = chatOpt.get();
        }

        Message message = new Message();
        message.setContent(messageDTO.getContent());
        message.setProducer(producer);
        message.setConsumer(consumer);
        message.setTypeMessage(messageDTO.getTypeMessage());
        message.setChat(chat);

        return messageRepository.save(message);
    }

    public List<Object[]> getAllChats(Long idUser){
        String sql = """
                    SELECT DISTINCT c.id AS chat_id, u.id AS other_user_id, u.username AS other_username 
                    FROM participants p1 JOIN participants p2 ON p1.id_chat = p2.id_chat JOIN users u ON p2.id_user = u.id JOIN chats c ON c.id = p1.id_chat 
                    WHERE p1.id_user = :idUser AND p2.id_user != :idUser;
                    """;

        List<Object[]> chats = entityManager.createNativeQuery(sql)
                .setParameter("idUser", idUser)
                .getResultList();
        return chats;
    }

    public List<HashMap<String, String>> formatResponseAllChats(List<Object[]> allChats){
        System.out.println(Arrays.asList(allChats));

        List<HashMap<String, String>> allChatsFormatted = new ArrayList<>();

        for (Object[] chat : allChats){
            HashMap<String, String> currentValuesChat = new HashMap<>();
            for (int i=0; i<chat.length; i++){
                switch (i){
                    case 0:
                        currentValuesChat.put("idChat", chat[i].toString());
                        break;
                    case 1:
                        currentValuesChat.put("idUser", chat[i].toString());
                        break;
                    case 2:
                        currentValuesChat.put("username_user", chat[i].toString());
                        break;
                }
            }
            allChatsFormatted.add(currentValuesChat);
        }

        return allChatsFormatted;
    }

    public Optional<User> getUsernameById(Long id){
        return userRepository.findById(id);
    }

    public List<String> getUsersWithChat(Long id){
        String sql = """
                SELECT DISTINCT u.username AS other_username 
                FROM participants p1 JOIN participants p2 ON p1.id_chat = p2.id_chat JOIN users u ON p2.id_user = u.id JOIN chats c ON c.id = p1.id_chat 
                WHERE p1.id_user = :idUser AND p2.id_user != :idUser;
                """;

        List<String> result = entityManager.createNativeQuery(sql)
                .setParameter("idUser", id)
                .getResultList();

        return result;
    }

    public void saveInvitation(String usernameInvitee, HttpSession session) {
        /*
         * 1)estrarre l'oggetto user invitato
         * 2)estrarre l'oggetto user stesso
         * */

        Optional<User> userInviteeObj = userService.findByUsername(usernameInvitee);

        User userInvitee = null;

        if(userInviteeObj.isPresent()){
            userInvitee = userInviteeObj.get();
        }

        //user stesso
        Optional<User> userInviterObj = userService.findByUsername(String.valueOf(session.getAttribute("username")));

        User userInviter = null;

        if(userInviterObj.isPresent()){
            userInviter = userInviterObj.get();
        }

        if(userInviter != null && userInvitee != null){
            Invitation invitation = new Invitation();
            invitation.setInviter(userInviter);
            invitation.setInvitee(userInvitee);
            invitation.setStatus(Status.PENDING);

            invitationRepository.save(invitation);
        }

    }
}
