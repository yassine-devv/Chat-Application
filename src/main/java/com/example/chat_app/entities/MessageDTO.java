package com.example.chat_app.entities;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private String content;
    private Long producerId;
    private Long consumerId;
    private Long chatId;
    private String typeMessage;

    public MessageDTO(Message message) {
        this.content = message.getContent();
        this.producerId = message.getProducer().getId();
        this.consumerId = message.getConsumer().getId();
        this.typeMessage = message.getTypeMessage();
        this.chatId = message.getChat().getId();
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "content='" + content + '\'' +
                ", producerId=" + producerId +
                ", consumerId=" + consumerId +
                ", chatId=" + chatId +
                ", typeMessage='" + typeMessage + '\'' +
                '}';
    }
}
