package com.aivle.sellon.global.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "app.events";
    public static final String MAIN_INBOUND_QUEUE = "main.inbound";
    private static final String AI_ROUTING_KEY_PATTERN = "ai.#";
    private static final String DEAD_LETTER_EXCHANGE = "app.events.dlx";
    private static final String DEAD_LETTER_ROUTING_KEY = "main.inbound.dead";

    @Bean
    public TopicExchange appEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue mainInboundQueue() {
        return QueueBuilder.durable(MAIN_INBOUND_QUEUE)
                .quorum()
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .withArgument("x-delivery-limit", 5)
                .withArgument("x-message-ttl", 86_400_000)
                .build();
    }

    @Bean
    public Binding mainInboundBinding(Queue mainInboundQueue, TopicExchange appEventsExchange) {
        return BindingBuilder.bind(mainInboundQueue).to(appEventsExchange).with(AI_ROUTING_KEY_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
