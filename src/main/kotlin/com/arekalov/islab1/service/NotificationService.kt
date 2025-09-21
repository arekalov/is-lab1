package com.arekalov.islab1.service

import com.arekalov.islab1.dto.FlatDTO
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.Session
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис для отправки уведомлений через WebSocket
 */
@ApplicationScoped
class NotificationService @Inject constructor(
    private val objectMapper: ObjectMapper
) {
    
    private val sessions = ConcurrentHashMap<String, Session>()
    
    /**
     * Добавить WebSocket сессию
     */
    fun addSession(sessionId: String, session: Session) {
        sessions[sessionId] = session
    }
    
    /**
     * Удалить WebSocket сессию
     */
    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
    }
    
    /**
     * Уведомление о создании квартиры
     */
    fun notifyFlatCreated(flat: FlatDTO) {
        val notification = WebSocketNotification(
            type = "FLAT_CREATED",
            data = flat
        )
        broadcastNotification(notification)
    }
    
    /**
     * Уведомление об обновлении квартиры
     */
    fun notifyFlatUpdated(flat: FlatDTO) {
        val notification = WebSocketNotification(
            type = "FLAT_UPDATED",
            data = flat
        )
        broadcastNotification(notification)
    }
    
    /**
     * Уведомление об удалении квартиры
     */
    fun notifyFlatDeleted(flatId: Long) {
        val notification = WebSocketNotification(
            type = "FLAT_DELETED",
            data = mapOf("id" to flatId)
        )
        broadcastNotification(notification)
    }
    
    /**
     * Отправить уведомление всем подключенным клиентам
     */
    private fun broadcastNotification(notification: WebSocketNotification) {
        val message = objectMapper.writeValueAsString(notification)
        
        sessions.values.removeIf { session ->
            try {
                if (session.isOpen) {
                    session.basicRemote.sendText(message)
                    false // оставляем сессию
                } else {
                    true // удаляем закрытую сессию
                }
            } catch (e: Exception) {
                println("Ошибка отправки уведомления: ${e.message}")
                true // удаляем проблемную сессию
            }
        }
    }
}

/**
 * Структура WebSocket уведомления
 */
data class WebSocketNotification(
    val type: String,
    val data: Any,
    val timestamp: String = java.time.Instant.now().toString()
)
