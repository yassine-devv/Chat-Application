package com.example.chat_app.entities;

import jakarta.persistence.*;
import lombok.*;
import org.thymeleaf.expression.Messages;

import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String username;
    private String password;

    @ManyToMany(mappedBy = "users")
    private Set<Chat> chats = new HashSet<>();

    // messaggi inviati
    @OneToMany(mappedBy = "producer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> sentMessage;

    // messaggi ricevuti
    @OneToMany(mappedBy = "consumer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> receivedMessages;

}
