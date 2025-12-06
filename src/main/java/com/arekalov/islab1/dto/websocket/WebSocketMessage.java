package com.arekalov.islab1.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для WebSocket сообщений
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {
    /**
     * Тип сообщения (например, "flat", "house")
     */
    private String type;
    
    /**
     * Действие (например, "create", "update", "delete")
     */
    private String action;
    
    /**
     * Данные сообщения
     */
    private Object data;
}

