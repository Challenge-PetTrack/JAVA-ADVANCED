package com.fiap.pettrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Resultado transitorio de geracao. Persistencia pertence a prioridade 3. */
public record HealthReportDTO(
        @NotNull UUID idGeracao, @NotNull @Positive Long idPet,
        @NotNull OffsetDateTime geradoEm, @NotNull OffsetDateTime contextoGeradoEm,
        @NotBlank @Pattern(regexp = "openai|mock") String provedor,
        @NotBlank @Size(max = 200) String modelo,
        @NotBlank @Pattern(regexp = "health-report-v1") String versaoPrompt,
        @NotNull Boolean simulado, @NotNull @AssertFalse Boolean persistido,
        @NotNull @Valid Relatorio relatorio) {

    public enum NivelAtencao { BAIXO, MODERADO, ALTO, INDETERMINADO }

    public record Motivo(
            @NotBlank @Size(max = 2000) String descricao,
            @NotEmpty @Size(max = 5) List<@Pattern(regexp = "pet|iotRecente|bcs|historico|clima") String> fontes) {}

    public record Relatorio(
            @NotBlank @Size(max = 2000) String resumo,
            @NotNull NivelAtencao nivelAtencao,
            @NotEmpty @Size(max = 10) List<@NotNull @Valid Motivo> motivos,
            @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 2000) String> recomendacoes,
            @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 4000) String> limitacoes) {}
}
