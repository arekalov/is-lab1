package com.arekalov.islab1.config

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException

/**
 * CORS фильтр для работы с React фронтендом
 */
class CorsFilter : Filter {
    
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpResponse = response as HttpServletResponse
        val httpRequest = request as HttpServletRequest
        
        // Разрешаем CORS для всех доменов (в продакшене нужно ограничить)
        httpResponse.setHeader("Access-Control-Allow-Origin", "*")
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        httpResponse.setHeader("Access-Control-Max-Age", "3600")
        
        // Обрабатываем preflight запросы
        if ("OPTIONS" == httpRequest.method) {
            httpResponse.status = HttpServletResponse.SC_OK
            return
        }
        
        chain.doFilter(request, response)
    }
    
    override fun init(filterConfig: FilterConfig?) {}
    
    override fun destroy() {}
}
