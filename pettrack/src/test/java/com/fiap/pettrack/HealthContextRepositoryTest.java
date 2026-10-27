package com.fiap.pettrack;

import com.fiap.pettrack.repository.HealthContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HealthContextRepositoryTest {
    private final LocalDateTime fim = LocalDateTime.of(2026, 10, 26, 12, 0);
    private JdbcTemplate jdbc;
    private HealthContextRepository repository;
    private SingleConnectionDataSource ds;

    @BeforeEach void setup() {
        ds = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=Oracle", "sa", "", true);
        // Mesmo DDL estrutural da aplicacao; nao executa packages, migracoes ou seeds em Oracle.
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__criar_tabelas_pettrack.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds);
        repository = new HealthContextRepository(new NamedParameterJdbcTemplate(ds));
        jdbc.update("INSERT INTO TB_TUTOR (ID_TUTOR,NM_TUTOR,DS_EMAIL) VALUES (1,'Tutor','teste@example.invalid')");
        jdbc.update("INSERT INTO TB_CLINICA (ID_CLINICA,NM_CLINICA,NR_CNPJ) VALUES (1,'Clinica','00000000000000')");
        for (int id = 1; id <= 2; id++) {
            jdbc.update("INSERT INTO TB_PET (ID_PET,NM_PET,DS_ESPECIE,ID_TUTOR,ID_CLINICA) VALUES (?,'Pet','Cao',1,1)", id);
        }
    }

    @AfterEach void fecharBancoIsolado() {
        ds.destroy();
    }

    @Test void janelaDe24HorasPreservaHoraExcluiFuturoEOutroPet() {
        leitura(1, 1, fim.minusHours(24), 38.0, 0.0);
        leitura(2, 1, fim.minusHours(1), 40.0, 20.0);
        leitura(3, 1, fim.minusHours(24).minusSeconds(1), 45.0, 100.0);
        leitura(4, 1, fim, 45.0, 100.0);
        leitura(5, 2, fim.minusHours(1), 45.0, 100.0);
        var result = repository.buscarIot(1L, fim.minusHours(24), fim);
        assertEquals(2, result.totalLeituras());
        assertEquals(39.0, result.temperaturaMedia());
        assertEquals(40.0, result.temperaturaMaxima());
        assertEquals(10.0, result.atividadeMedia());
        assertEquals(fim.minusHours(1), result.ultimaLeitura());
    }

    @Test void semLeiturasNaoInventaMediasZero() {
        var result = repository.buscarIot(1L, fim.minusHours(24), fim);
        assertEquals(0, result.totalLeituras());
        assertNull(result.temperaturaMedia());
        assertNull(result.temperaturaMaxima());
        assertNull(result.atividadeMedia());
        assertNull(result.ultimaLeitura());
    }

    @Test void atividadeAusenteOuNegativaNaoEntraNaMediaMasZeroValidoEntra() {
        leitura(1, 1, fim.minusHours(1), 38.0, null);
        leitura(2, 1, fim.minusHours(1), 39.0, -10.0);
        leitura(3, 1, fim.minusHours(1), 40.0, 0.0);
        var result = repository.buscarIot(1L, fim.minusHours(24), fim);
        assertEquals(3, result.totalLeituras());
        assertEquals(1, result.atividadesValidas());
        assertEquals(0.0, result.atividadeMedia());
    }

    @Test void alertasRespeitamPetEJanelaIncluindoResolvidos() {
        alerta(1, 1, fim.minusHours(24), "N");
        alerta(2, 1, fim.minusHours(1), "S");
        alerta(3, 1, fim, "N");
        alerta(4, 2, fim.minusHours(1), "N");
        var result = repository.buscarIot(1L, fim.minusHours(24), fim);
        assertEquals(2, result.alertas().size());
        assertEquals("S", result.alertas().get(0).resolvido());
    }

    @Test void bcsUsaHorarioEDesempatePorIdDescartaNuloEFuturo() {
        bcs(1, 1, fim.minusHours(4), 4);
        bcs(2, 1, fim.minusHours(2), 5);
        bcs(3, 1, fim.minusHours(2), 6);
        bcs(4, 1, fim.minusHours(1), null);
        bcs(5, 1, fim, 9);
        bcs(6, 2, fim.minusHours(1), 9);
        var result = repository.buscarUltimosBcs(1L, fim);
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        assertEquals(fim.minusHours(2), result.get(0).data());
    }

    @Test void historicoFiltraEventosEMedicamentosPeloPetEData() {
        evento(1, 1, fim.minusDays(100), "CONSULTA");
        evento(2, 1, fim.minusDays(1), "EXAME");
        evento(3, 1, fim.plusDays(1), "CONSULTA");
        evento(4, 2, fim.minusDays(1), "CONSULTA");
        medicamento(1, 1, fim.minusDays(10), null);
        medicamento(2, 2, fim.minusDays(2), fim.toLocalDate().atStartOfDay());
        medicamento(3, 2, fim.plusDays(1), null);
        medicamento(4, 2, fim.minusDays(5), fim.minusDays(1));
        medicamento(5, 4, fim.minusDays(5), null);
        var result = repository.buscarHistorico(1L, fim);
        assertEquals(fim.minusDays(100), result.ultimaConsulta());
        assertEquals(1, result.eventosRecentes().size());
        assertEquals(2L, result.eventosRecentes().get(0).id());
        assertEquals(2, result.medicamentosAtivos().size());
        assertEquals(2L, result.medicamentosAtivos().get(0).id());
    }

    @Test void vacinasIncluemVencidasEAte30DiasSemConfundirOutrosProtocolos() {
        vacina(1, 1, "VACINA", fim.minusDays(2));
        vacina(2, 1, "VACINA", fim.plusDays(30));
        vacina(3, 1, "VACINA", fim.plusDays(31));
        vacina(4, 2, "VACINA", fim.plusDays(1));
        vacina(5, 1, "CHECKUP", fim.plusDays(1));
        vacina(6, 1, "VACINA", null);
        var result = repository.buscarHistorico(1L, fim);
        assertEquals(2, result.vacinasVencidasOuVencendo().size());
        assertEquals(1L, result.vacinasVencidasOuVencendo().get(0).id());
        assertEquals(2L, result.vacinasVencidasOuVencendo().get(1).id());
    }

    private void leitura(int id, int pet, LocalDateTime data, Double temp, Double atv) {
        jdbc.update("INSERT INTO TB_COLLAR_LEITURA (ID_LEITURA,ID_PET,DT_LEITURA,NR_TEMPERATURA,NR_ATIVIDADE) VALUES (?,?,?,?,?)",
                id, pet, Timestamp.valueOf(data), temp, atv);
    }
    private void alerta(int id, int pet, LocalDateTime data, String resolvido) {
        jdbc.update("INSERT INTO TB_ALERTA (ID_ALERTA,ID_PET,DT_ALERTA,TP_ALERTA,ST_RESOLVIDO) VALUES (?,?,?,'FEBRE',?)",
                id, pet, Timestamp.valueOf(data), resolvido);
    }
    private void bcs(int id, int pet, LocalDateTime data, Integer score) {
        jdbc.update("INSERT INTO TB_BCS_HISTORICO (ID_BCS,ID_PET,DT_ANALISE,NR_BCS) VALUES (?,?,?,?)",
                id, pet, Timestamp.valueOf(data), score);
    }
    private void evento(int id, int pet, LocalDateTime data, String tipo) {
        jdbc.update("INSERT INTO TB_EVENTO_CLINICO (ID_EVENTO,ID_PET,ID_CLINICA,DT_EVENTO,TP_EVENTO) VALUES (?,?,1,?,?)",
                id, pet, Timestamp.valueOf(data), tipo);
    }
    private void medicamento(int id, int evento, LocalDateTime inicio, LocalDateTime fimMed) {
        jdbc.update("INSERT INTO TB_MEDICAMENTO (ID_MEDICAMENTO,ID_EVENTO,NM_MEDICAMENTO,DS_DOSAGEM,DS_FREQUENCIA,DT_INICIO,DT_FIM) VALUES (?,?,'Medicamento','Dose registrada','Frequencia registrada',?,?)",
                id, evento, Timestamp.valueOf(inicio), fimMed == null ? null : Timestamp.valueOf(fimMed));
    }
    private void vacina(int id, int pet, String tipo, LocalDateTime proxima) {
        jdbc.update("INSERT INTO TB_PROTOCOLO_PREVENTIVO (ID_PROTOCOLO,ID_PET,NM_PROTOCOLO,TP_PROTOCOLO,DT_PROXIMA,ST_STATUS) VALUES (?,?,'Protocolo',?,?,'PENDENTE')",
                id, pet, tipo, proxima == null ? null : Timestamp.valueOf(proxima));
    }
}
