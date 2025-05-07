package com.example.chat_app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "private")
    private boolean priv;

    @ManyToMany
    @JoinTable(name = "participants", joinColumns = @JoinColumn(name = "id_chat"), inverseJoinColumns = @JoinColumn(name = "id_user"))
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> message;
}
