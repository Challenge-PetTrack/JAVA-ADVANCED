package com.fiap.pettrack.control;

import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.repository.IUsuarioRepository;
import com.fiap.pettrack.service.HealthReportService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/health-reports")
@RequiredArgsConstructor
public class HealthReportController {
    private final HealthReportService service;
    private final IUsuarioRepository usuarios;

    @Operation(summary = "Obtem o token CSRF da sessao autenticada para solicitar uma geracao")
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrf) {
        return Map.of("headerName", csrf.getHeaderName(), "token", csrf.getToken());
    }

    @Operation(summary = "Gera Health Report com IA para um pet autorizado, sem persistir",
            description = "Requer sessao autenticada e token CSRF. O contexto e consultado pelo servidor.")
    @PostMapping("/pets/{idPet}/generate")
    public HealthReportDTO gerar(@PathVariable Long idPet, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria");
        }
        var usuario = usuarios.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
        return service.gerar(idPet, usuario);
    }
}
