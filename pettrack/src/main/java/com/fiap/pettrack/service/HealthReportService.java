package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthReportService {
    private final HealthContextService contextos;
    private final HealthReportClient client;

    public HealthReportDTO gerar(Long idPet, Usuario usuario) {
        // Autoriza e consulta dados no servidor ANTES de qualquer chamada ao provedor.
        return client.gerar(contextos.montar(idPet, usuario));
    }
}
