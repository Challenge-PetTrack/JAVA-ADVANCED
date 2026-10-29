package com.fiap.pettrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.dto.HealthReportDTO;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class HealthReportFixtures {
    static ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    static HealthContextDTO contexto() {
        try (var input = HealthReportFixtures.class.getResourceAsStream("/health-report/context.json")) {
            return mapper().readValue(input, HealthContextDTO.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static HealthReportDTO resultado() {
        return new HealthReportDTO(UUID.randomUUID(), 1L, OffsetDateTime.parse("2026-10-27T15:00:05Z"),
                contexto().geradoEm(), "gemini", "gemini-3.8-flash", "health-report-v1", false, false,
                new HealthReportDTO.Relatorio("Revisar os dados com o veterinario.", HealthReportDTO.NivelAtencao.MODERADO,
                        List.of(new HealthReportDTO.Motivo("BCS com tendencia crescente.", List.of("bcs"))),
                        List.of("Acompanhar os registros junto ao veterinario."), List.of("Dados limitados ao contexto.")));
    }
}
