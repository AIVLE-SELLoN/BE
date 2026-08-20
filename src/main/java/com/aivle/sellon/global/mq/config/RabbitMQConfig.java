package com.aivle.sellon.global.mq.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

// exchange/queue/binding은 RabbitMQ Topology Operator(ops 쪽 K8s CR)가 이미 선언해서 관리한다.
// BE 유저는 configure 권한이 없어서(least-privilege) 여기서 빈으로 다시 선언하면 기동 시
// "ACCESS_REFUSED - configure access" 로 커넥션이 계속 끊기고 헬스 readiness가 죽는다.
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "app.events";
    public static final String MAIN_INBOUND_QUEUE = "main.inbound";

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
