package com.fiap.pettrack.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/** Contrato de dados para o Health Report. Nao contem entidades JPA nem dados pessoais do tutor. */
public record HealthContextDTO(
        String versao, OffsetDateTime geradoEm, String fusoHorario,
        PerfilPet pet, Periodo periodoIot, IotResumo iotRecente,
        BcsResumo bcs, Historico historico, Clima clima, List<String> avisos) {

    public record PerfilPet(Long id, String nome, String especie, String raca, Double idadeAnos, Double pesoKg) {}
    /** Inicio inclusivo e fim exclusivo, no fuso indicado no contexto. */
    public record Periodo(LocalDateTime inicioInclusivo, LocalDateTime fimExclusivo) {}
    public record IotResumo(long totalLeituras, long temperaturasValidas, long atividadesValidas,
                            Double temperaturaMedia, Double temperaturaMaxima, Double atividadeMedia,
                            LocalDateTime ultimaLeitura, List<AlertaResumo> alertas) {}
    public record AlertaResumo(Long id, String tipo, String descricao, Double valorReferencia,
                               String resolvido, LocalDateTime data) {}
    public record AvaliacaoBcs(Long id, Integer score, String observacao, LocalDateTime data) {}
    /** Tendencia compara as duas ultimas avaliacoes validas, sem inferencia do LLM. */
    public record BcsResumo(AvaliacaoBcs ultima, AvaliacaoBcs anterior, String tendencia) {}
    public record EventoResumo(Long id, String tipo, LocalDateTime data, String diagnostico, String observacao) {}
    public record MedicamentoResumo(Long id, String nome, String dosagem, String frequencia,
                                    LocalDate inicio, LocalDate fim) {}
    public record VacinaResumo(Long id, String nome, LocalDate proximaAplicacao, String statusCadastro) {}
    public record Historico(LocalDate inicioEventos, LocalDateTime ultimaConsulta,
                            List<EventoResumo> eventosRecentes, List<MedicamentoResumo> medicamentosAtivos,
                            LocalDate limiteVacinas, List<VacinaResumo> vacinasVencidasOuVencendo) {}
    public record Clima(String status, String fonte, String localidade, OffsetDateTime observadoEm,
                        Double temperaturaCelsius, Double umidadePercentual, String condicao) {
        public static Clima indisponivel(String status, String localidade) {
            return new Clima(status, "OpenWeatherMap", localidade, null, null, null, null);
        }
    }
}
