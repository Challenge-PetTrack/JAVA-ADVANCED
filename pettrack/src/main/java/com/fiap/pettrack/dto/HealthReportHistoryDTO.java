package com.fiap.pettrack.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class HealthReportHistoryDTO {
    private HealthReportHistoryDTO() {}

    public record Detalhe(Long solicitadoPor, OffsetDateTime salvoEm,
                          HealthReportDTO relatorio, HealthContextDTO contexto) {}

    public record Resumo(UUID idGeracao, Long idPet, OffsetDateTime geradoEm,
                         OffsetDateTime salvoEm, String provedor, String modelo,
                         boolean simulado, HealthReportDTO.NivelAtencao nivelAtencao) {}

    public record Pagina(List<Resumo> itens, int pagina, int tamanho, long total) {}
}
