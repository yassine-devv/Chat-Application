package com.example.chat_app.service;

import com.example.chat_app.entities.*;
import com.example.chat_app.repository.ChatRepository;
import com.example.chat_app.repository.InvitationRepository;
import com.example.chat_app.repository.MessageRepository;
import com.example.chat_app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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

    public List<Message> findByChatIdWithPagination(Long chatId, int limit, int offset){
        return messageRepository.findByChatIdWithPagination(chatId, limit, offset);
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

    public Invitation saveInvitation(String idUserInvitee, HttpSession session) {
        /*
         * 1)estrarre l'oggetto user invitato
         * 2)estrarre l'oggetto user stesso
         * */

        Optional<User> userInviteeObj = userService.findById(Long.parseLong(idUserInvitee));

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

        Invitation saved = null;

        if(userInviter != null && userInvitee != null){
            Invitation invitation = new Invitation();
            invitation.setInviter(userInviter);
            invitation.setInvitee(userInvitee);
            invitation.setStatus(Status.PENDING);

            saved = invitationRepository.save(invitation);
        }

        return saved;
    }

    // funzione che dati i due id degli utenti come parametri ritorna lo status dell'invito tra i due
    public List<Object[]> getInvitation(Long idUser1, Long idUser2){
        String sql = """
                select i.id, invitee.username as invitee, inviter.username as inviter, i.status from invitations i
                JOIN users inviter ON i.inviter = inviter.id
                JOIN users invitee ON i.invitee = invitee.id where i.inviter = :idUser1 and i.invitee = :idUser2 or i.inviter = :idUser2 and i.invitee = :idUser1;
                """;

        // [0]id [1]invitee [2]inviter [3]status
        List<Object[]> result = entityManager.createNativeQuery(sql)
                .setParameter("idUser1", idUser1)
                .setParameter("idUser2", idUser2)
                .getResultList();

        return result;
    }

    @Transactional
    public int saveInvitation(Long id, String status){
        /*
        * UPDATE invitations i
            SET status = 'PENDING'
            WHERE i.id = 3;
        * */

        String sql = """
                UPDATE invitations
                SET status = :status
                WHERE id = :id;
                """;

        int rowsUpdated = entityManager.createNativeQuery(sql)
                .setParameter("status", status)
                .setParameter("id", id)
                .executeUpdate();

        return rowsUpdated;
    }

    public Chat saveChat(){
        Chat chat = new Chat();
        chat.setPriv(true);

        return chatRepository.save(chat);
    }

    @Transactional
    public int addParticipantsToChat(Long idChat, Long idUser1, Long idUser2){
        // insert into participants (id_user, id_chat) values (1, 1), (2, 1)
        String sql = """
                insert into participants (id_user, id_chat) values (?, ?), (?, ?)
                """;

        int rowsInserted = entityManager.createNativeQuery(sql)
                .setParameter(1, idUser1)
                .setParameter(2, idChat)
                .setParameter(3, idUser2)
                .setParameter(4, idChat)
                .executeUpdate();

        return rowsInserted;
    }

    public List<String> getCountPendingInvitations(Long id){
        // select count(i.invitee) from invitations i  where i.status = 'PENDING' and i.invitee = 2
        String sql = """
                select count(i.invitee) from invitations i  where i.status = 'PENDING' and i.invitee = :id;
                """;

        // [0]id [1]invitee [2]inviter [3]status
        List<String> result = entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getResultList();

        return result;
    }

    public List<Object[]> getAllPendingInvitations(Long id){
        //select i.id, inviter, invitee, i.status from invitations i join users inviter ON i.inviter = inviter.id join users invitee ON i.invitee = invitee.id where i.invitee = 2 and i.status = 'PENDING';
        String sql = """
                select inviter.username as username_inviter, i.id as Id_invitation, inviter as id_inviter, invitee, i.status from invitations i join users inviter ON i.inviter = inviter.id join users invitee ON i.invitee = invitee.id where i.invitee = :id and i.status = 'PENDING';
                """;

        // [0]id [1]invitee [2]inviter [3]status
        List<Object[]> result = entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getResultList();

        return result;
    }

    public List<HashMap<String, String>> formatResultAllInvitations(List<Object[]> allInvitations){
        List<HashMap<String, String>> allInvitationsFormatted = new ArrayList<>();

        for (Object[] chat : allInvitations){
            HashMap<String, String> currentValuesInv = new HashMap<>();
            for (int i=0; i<chat.length; i++){
                switch (i){
                    case 0:
                        currentValuesInv.put("username_inviter", chat[i].toString());
                        break;
                    case 1:
                        currentValuesInv.put("id_invitation", chat[i].toString());
                        break;
                    case 2:
                        currentValuesInv.put("id_inviter", chat[i].toString());
                        break;
                    case 3:
                        currentValuesInv.put("id_invitee", chat[i].toString());
                        break;
                    case 4:
                        currentValuesInv.put("status", chat[i].toString());
                        break;
                }
            }
            allInvitationsFormatted.add(currentValuesInv);
        }

        return allInvitationsFormatted;
    }

    public String detectProducerMessage(String usernameProducer, String usernameParticipant, String usernameSession, String messageContent){
        String response = null;

        if(usernameProducer.equals(usernameParticipant)){
            response += """
                    <div class="consumer-message">
                        <span>%s</span><br>
                    </div>
                    """.formatted(messageContent);
        }

        if(usernameProducer.equals(usernameSession)){
            response += """
                    <div class="producer-message">
                        <span>%s</span><br>
                    </div>
                    """.formatted(messageContent);
        }

        return response;
    }
}
