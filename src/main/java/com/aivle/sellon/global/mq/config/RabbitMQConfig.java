package com.aivle.sellon.global.mq.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "app.events";
    public static final String MAIN_INBOUND_QUEUE = "main.inbound";
    private static final String AI_ROUTING_KEY_PATTERN = "ai.#";
    private static final String DEAD_LETTER_EXCHANGE = "app.events.dlx";
    private static final String DEAD_LETTER_ROUTING_KEY = "main.inbound.dead";

    // CI/로컬은 자체 RabbitMQ 컨테이너를 매번 새로 띄우므로 앱이 토폴로지를 직접 선언해야 한다.
    // prod는 RabbitMQ Topology Operator(ops K8s CR)가 이미 동일 스펙으로 선언해뒀고, BE 유저는
    // configure 권한이 없어(least-privilege) 여기서 다시 선언을 시도하면 ACCESS_REFUSED로
    // 커넥션이 끊기고 readiness가 죽는다 - application-prod.yaml에서 false로 끈다.
    @Bean
    @ConditionalOnProperty(name = "rabbitmq.topology.self-managed", havingValue = "true", matchIfMissing = true)
    public TopicExchange appEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "rabbitmq.topology.self-managed", havingValue = "true", matchIfMissing = true)
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
    @ConditionalOnProperty(name = "rabbitmq.topology.self-managed", havingValue = "true", matchIfMissing = true)
    public Binding mainInboundBinding(Queue mainInboundQueue, TopicExchange appEventsExchange) {
        return BindingBuilder.bind(mainInboundQueue).to(appEventsExchange).with(AI_ROUTING_KEY_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    /**
     * 재시도해도 소용없다고 리스너가 판단한 실패(AmqpRejectAndDontRequeueException)는
     * 재시도 대상에서 뺀다. 이게 없으면 잘못된 payload 하나가 2초 간격으로 5번 재시도된 뒤에야
     * DLQ로 가서, 즉시 DLQ로 보내려던 의도가 사라진다.
     */
    @Bean
    public RabbitListenerRetrySettingsCustomizer nonRetryableFailureCustomizer() {
        return settings -> settings.setExceptionExcludes(List.of(AmqpRejectAndDontRequeueException.class));
    }
}
