package com.example.chat_app.entities;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public class Message {
    private String content;
    private String producer;
    private String consumer;
    private String typeMessage;

    public Message(String content, String producer, String consumer, String typeMessage) {
        this.content = content;
        this.producer = producer;
        this.consumer = consumer;
        this.typeMessage = typeMessage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getTypeMessage() {
        return typeMessage;
    }

    public void setTypeMessage(String typeMessage) {
        this.typeMessage = typeMessage;
    }

    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                ", producer='" + producer + '\'' +
                ", consumer='" + consumer + '\'' +
                ", typeMessage='" + typeMessage + '\'' +
                '}';
    }
}


