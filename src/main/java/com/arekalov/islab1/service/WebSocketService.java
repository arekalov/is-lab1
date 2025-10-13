package com.arekalov.islab1.service;

import com.arekalov.islab1.websocket.UpdatesWebSocket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebSocketService {
    
    @Inject
    private UpdatesWebSocket updatesWebSocket;

    public void notifyFlatUpdate(String action, Object data) {
        updatesWebSocket.broadcast("FLAT", action, data);
    }

    public void notifyHouseUpdate(String action, Object data) {
        updatesWebSocket.broadcast("HOUSE", action, data);
    }
}
