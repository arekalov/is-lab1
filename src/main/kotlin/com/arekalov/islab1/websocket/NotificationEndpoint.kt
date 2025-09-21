package com.arekalov.islab1.websocket

import com.arekalov.islab1.service.NotificationService
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint

/**
 * WebSocket endpoint для real-time уведомлений
 */
@ServerEndpoint("/notifications")
class NotificationEndpoint @Inject constructor(
    private val notificationService: NotificationService
) {
    
    /**
     * Обработка подключения клиента
     */
    @OnOpen
    fun onOpen(session: Session) {
        println("WebSocket подключение открыто: ${session.id}")
        notificationService.addSession(session.id, session)
        
        // Отправляем приветственное сообщение
        try {
            session.basicRemote.sendText("""
                {
                    "type": "CONNECTED",
                    "data": {
                        "message": "Подключение к системе уведомлений установлено",
                        "sessionId": "${session.id}"
                    },
                    "timestamp": "${java.time.Instant.now()}"
                }
            """.trimIndent())
        } catch (e: Exception) {
            println("Ошибка отправки приветственного сообщения: ${e.message}")
        }
    }
    
    /**
     * Обработка сообщений от клиента
     */
    @OnMessage
    fun onMessage(message: String, session: Session) {
        println("Получено сообщение от ${session.id}: $message")
        
        // Здесь можно обрабатывать команды от клиента
        // Например, подписка на определенные типы уведомлений
        try {
            when (message.trim()) {
                "ping" -> {
                    session.basicRemote.sendText("""
                        {
                            "type": "PONG",
                            "data": {
                                "message": "pong"
                            },
                            "timestamp": "${java.time.Instant.now()}"
                        }
                    """.trimIndent())
                }
                else -> {
                    session.basicRemote.sendText("""
                        {
                            "type": "ERROR",
                            "data": {
                                "message": "Неизвестная команда: $message"
                            },
                            "timestamp": "${java.time.Instant.now()}"
                        }
                    """.trimIndent())
                }
            }
        } catch (e: Exception) {
            println("Ошибка обработки сообщения: ${e.message}")
        }
    }
    
    /**
     * Обработка ошибок
     */
    @OnError
    fun onError(session: Session, throwable: Throwable) {
        println("WebSocket ошибка для сессии ${session.id}: ${throwable.message}")
        notificationService.removeSession(session.id)
    }
    
    /**
     * Обработка закрытия соединения
     */
    @OnClose
    fun onClose(session: Session, closeReason: CloseReason) {
        println("WebSocket соединение закрыто для сессии ${session.id}: ${closeReason.reasonPhrase}")
        notificationService.removeSession(session.id)
    }
}
