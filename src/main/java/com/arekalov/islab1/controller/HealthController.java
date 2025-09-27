package com.arekalov.islab1.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check контроллер
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthController {
    
    @GET
    public Response healthCheck() {
        Map<String, String> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", Instant.now().toString());
        healthStatus.put("application", "Flats Management System");
        healthStatus.put("version", "1.0.0");
        
        return Response.ok(healthStatus).build();
    }
}
