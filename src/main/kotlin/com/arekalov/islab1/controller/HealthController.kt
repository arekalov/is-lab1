package com.arekalov.islab1.controller

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant

/**
 * Health check контроллер
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
class HealthController {
    
    @GET
    fun healthCheck(): Response {
        val healthStatus = mapOf(
            "status" to "UP",
            "timestamp" to Instant.now().toString(),
            "application" to "Flats Management System",
            "version" to "1.0.0"
        )
        
        return Response.ok(healthStatus).build()
    }
}
