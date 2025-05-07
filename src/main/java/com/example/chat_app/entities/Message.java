package com.example.chat_app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content")
    private String content;

    @ManyToOne
    @JoinColumn(name = "producer")
    private User producer;

    @ManyToOne
    @JoinColumn(name = "consumer")
    private User consumer;

    @Column(name = "type_message")
    private String typeMessage;

    @ManyToOne
    @JoinColumn(name = "chat")
    private Chat chat;
}


