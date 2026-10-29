package com.fiap.pettrack.control;

import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.dto.HealthReportHistoryDTO.*;
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
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;

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

    @Operation(summary = "Gera e salva Health Report com IA para um pet autorizado",
            description = "Requer sessao autenticada e token CSRF. O contexto e consultado pelo servidor.")
    @PostMapping("/pets/{idPet}/generate")
    public HealthReportDTO gerar(@PathVariable Long idPet, Authentication authentication) {
        return service.gerar(idPet, usuario(authentication));
    }

    @GetMapping("/pets/{idPet}")
    @Operation(summary = "Lista metadados dos relatorios do pet, com paginacao")
    public Pagina listar(@PathVariable Long idPet, @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "20") int tamanho, Authentication authentication) {
        return service.listar(idPet, pagina, tamanho, usuario(authentication));
    }

    @GetMapping("/pets/{idPet}/latest")
    @Operation(summary = "Consulta o ultimo relatorio salvo e o contexto original")
    public Detalhe ultimo(@PathVariable Long idPet, Authentication authentication) {
        return service.ultimo(idPet, usuario(authentication));
    }

    @GetMapping("/pets/{idPet}/{idGeracao}")
    @Operation(summary = "Consulta um relatorio salvo e seu contexto original")
    public Detalhe buscar(@PathVariable Long idPet, @PathVariable UUID idGeracao, Authentication authentication) {
        return service.buscar(idPet, idGeracao, usuario(authentication));
    }

    @ModelAttribute
    public void impedirCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    private com.fiap.pettrack.model.Usuario usuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria");
        }
        var usuario = usuarios.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
        return usuario;
    }
}
