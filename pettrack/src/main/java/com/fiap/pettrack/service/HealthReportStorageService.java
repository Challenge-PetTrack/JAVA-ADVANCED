package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.*;
import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class HealthReportStorageService {
    private final HealthReportRepository repository;
    private final HealthContextService contextos;
    private final Clock clock;

    @Transactional
    public HealthReportDTO salvar(HealthReportDTO report, HealthContextDTO contexto, Usuario usuario) {
        contextos.autorizar(report.idPet(), usuario);
        var salvo = report.comoPersistido();
        repository.salvar(salvo, contexto, usuario.getId(), OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS));
        return salvo;
    }
}
