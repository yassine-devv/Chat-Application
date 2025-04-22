package com.example.chat_app.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;

import java.util.List;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMqConfig {


    public static final String EXCHANGE_FANOUT = "publicFanout";
    public static final String QUEUE_NAME = "fanoutQueue";

    private final AmqpAdmin rabbitAdmin;

    public RabbitMqConfig(AmqpAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Bean
    public Queue chatQueue() {
        return new Queue("chat.queue", true); // durable = true
    }

    @Bean
    public Queue queueFanout(){
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public FanoutExchange exchangeFanout() {
        return new FanoutExchange(EXCHANGE_FANOUT);
    }

    @Bean
    public Declarables fanoutExchangeBindings(FanoutExchange exchangeFanout, Queue queueFanout) {
        return new Declarables(
                BindingBuilder.bind(queueFanout).to(exchangeFanout)
        );
    }
}
