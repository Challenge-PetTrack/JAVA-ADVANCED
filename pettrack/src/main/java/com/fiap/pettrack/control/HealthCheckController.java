package com.fiap.pettrack.control;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health Check", description = "Endpoint ultraleve para verificação de disponibilidade no Azure App Service")
public class HealthCheckController {

    @Operation(summary = "Verifica se a aplicação está online e respondendo na porta HTTP")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP - PETTRACK");
    }
}
