package com.aivle.sellon.global.mq.listener;

import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * main.inbound 큐(ai.# 전체 수신)의 유일한 컨슈머. eventType으로 처리할 핸들러를 찾아 넘긴다.
 * 도메인별로 큐/리스너를 따로 두지 않는 이유: 바인딩이 ai.#이라 큐를 나누려면
 * 라우팅 키 컨벤션 자체를 바꿔야 하는데, 지금은 eventType 분기로 충분하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainInboundListener {

    private final List<MqEventHandler> handlers;

    @RabbitListener(queues = RabbitMQConfig.MAIN_INBOUND_QUEUE)
    public void onMessage(JsonNode envelope) {
        String eventType = envelope.path("eventType").asString(null);
        String eventId = envelope.path("eventId").asString(null);
        String traceId = envelope.path("traceId").asString(null);

        List<MqEventHandler> matched = handlers.stream()
                .filter(h -> h.supports(eventType))
                .toList();

        // 처리할 핸들러가 없는 이벤트는 ack 후 사라진다. 핸들러가 늘어나기 전까지는
        // 무엇이 유실되는지 로그로만 남긴다.
        if (matched.isEmpty()) {
            log.warn("처리 대상이 아닌 이벤트 폐기 - eventType={}, eventId={}, traceId={}",
                    eventType, eventId, traceId);
            return;
        }

        log.info("핸들러 {}개 매칭 - eventType={}, handlers={}, eventId={}, traceId={}",
                matched.size(), eventType,
                matched.stream().map(h -> h.getClass().getSimpleName()).toList(), eventId, traceId);

        for (MqEventHandler handler : matched) {
            handler.handle(envelope, eventId, traceId);
        }
    }
}
