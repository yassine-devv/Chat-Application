package com.example.chat_app.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invitations")
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inviter")
    private User inviter;

    @ManyToOne
    @JoinColumn(name = "invitee")
    private User invitee;

    @Enumerated(EnumType.STRING)
    private Status status;
}
