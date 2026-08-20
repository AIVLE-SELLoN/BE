package com.aivle.sellon.global.mq.listener;

import tools.jackson.databind.JsonNode;

/**
 * main.inbound 큐로 들어오는 이벤트 하나를 eventType 기준으로 처리하는 단위.
 * 큐는 하나뿐이고(ai.# 전체 수신), {@link MainInboundListener}가 eventType으로
 * 어느 핸들러에 넘길지 고른다.
 */
public interface MqEventHandler {

    boolean supports(String eventType);

    /**
     * @param envelope 전체 envelope(payload 포함). 처리는 envelope.get("payload")부터 시작한다.
     * @param eventId  DLQ 사유 로깅용
     * @param traceId  DLQ 사유 로깅용
     */
    void handle(JsonNode envelope, String eventId, String traceId);
}
