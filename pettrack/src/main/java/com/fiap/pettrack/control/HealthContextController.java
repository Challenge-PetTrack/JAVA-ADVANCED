package com.fiap.pettrack.control;

import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.repository.IUsuarioRepository;
import com.fiap.pettrack.service.HealthContextService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/health-context")
@RequiredArgsConstructor
public class HealthContextController {
    private final HealthContextService service;
    private final IUsuarioRepository usuarios;

    @Operation(summary = "Consolida os dados do pet para o Health Report, sem chamar IA ou gravar relatorio")
    @GetMapping("/pets/{idPet}")
    public HealthContextDTO consultar(@PathVariable Long idPet, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria");
        }
        var usuario = usuarios.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
        return service.montar(idPet, usuario);
    }
}
