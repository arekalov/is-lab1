package com.arekalov.islab1.websocket;

import com.arekalov.islab1.config.ObjectMapperProducer;
import com.arekalov.islab1.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/websocket/updates")
public class UpdatesWebSocket {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @Inject
    private ObjectMapperProducer objectMapperProducer;

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle incoming messages if needed
    }

    public void broadcast(String type, String action, Object data) {
        WebSocketMessage message = new WebSocketMessage(type, action, data);
        String jsonMessage;
        try {
            jsonMessage = objectMapperProducer.getObjectMapper().writeValueAsString(message);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        sessions.values().forEach(session -> {
            try {
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
