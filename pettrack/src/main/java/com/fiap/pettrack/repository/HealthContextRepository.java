package com.fiap.pettrack.repository;

import com.fiap.pettrack.dto.HealthContextDTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HealthContextRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public IotResumo buscarIot(Long id, LocalDateTime inicio, LocalDateTime fim) {
        var params = periodo(id, inicio, fim);
        var alertas = jdbc.query("""
                SELECT ID_ALERTA, TP_ALERTA, DS_DESCRICAO, NR_VALOR_REF, ST_RESOLVIDO, DT_ALERTA
                FROM TB_ALERTA WHERE ID_PET = :id AND DT_ALERTA >= :inicio AND DT_ALERTA < :fim
                ORDER BY DT_ALERTA DESC, ID_ALERTA DESC
                """, params, (r, n) -> new AlertaResumo(r.getLong("ID_ALERTA"), r.getString("TP_ALERTA"),
                r.getString("DS_DESCRICAO"), numero(r, "NR_VALOR_REF"), r.getString("ST_RESOLVIDO"),
                dataHora(r, "DT_ALERTA")));
        return jdbc.queryForObject("""
                SELECT COUNT(*) TOTAL,
                    COUNT(CASE WHEN NR_TEMPERATURA BETWEEN 30 AND 45 THEN 1 END) TEMPS,
                    COUNT(CASE WHEN NR_ATIVIDADE >= 0 THEN 1 END) ATVS,
                    AVG(CASE WHEN NR_TEMPERATURA BETWEEN 30 AND 45 THEN NR_TEMPERATURA END) TEMP_MEDIA,
                    MAX(CASE WHEN NR_TEMPERATURA BETWEEN 30 AND 45 THEN NR_TEMPERATURA END) TEMP_MAX,
                    AVG(CASE WHEN NR_ATIVIDADE >= 0 THEN NR_ATIVIDADE END) ATV_MEDIA,
                    MAX(DT_LEITURA) ULTIMA
                FROM TB_COLLAR_LEITURA
                WHERE ID_PET = :id AND DT_LEITURA >= :inicio AND DT_LEITURA < :fim
                """, params, (r, n) -> new IotResumo(r.getLong("TOTAL"), r.getLong("TEMPS"), r.getLong("ATVS"),
                numero(r, "TEMP_MEDIA"), numero(r, "TEMP_MAX"), numero(r, "ATV_MEDIA"),
                dataHora(r, "ULTIMA"), List.copyOf(alertas)));
    }

    public List<AvaliacaoBcs> buscarUltimosBcs(Long id, LocalDateTime fim) {
        return jdbc.query("""
                SELECT ID_BCS, NR_BCS, DS_OBSERVACAO, DT_ANALISE FROM TB_BCS_HISTORICO
                WHERE ID_PET = :id AND DT_ANALISE < :fim
                    AND NR_BCS BETWEEN 1 AND 9 AND NR_BCS = FLOOR(NR_BCS)
                ORDER BY DT_ANALISE DESC, ID_BCS DESC FETCH FIRST 2 ROWS ONLY
                """, periodo(id, fim, fim), (r, n) -> new AvaliacaoBcs(r.getLong("ID_BCS"), r.getInt("NR_BCS"),
                r.getString("DS_OBSERVACAO"), dataHora(r, "DT_ANALISE")));
    }

    public Historico buscarHistorico(Long id, LocalDateTime fim) {
        LocalDate hoje = fim.toLocalDate();
        LocalDate inicio = hoje.minusDays(90);
        LocalDate limite = hoje.plusDays(30);
        var params = periodo(id, inicio.atStartOfDay(), fim)
                .addValue("hoje", Timestamp.valueOf(hoje.atStartOfDay()))
                .addValue("amanha", Timestamp.valueOf(hoje.plusDays(1).atStartOfDay()))
                .addValue("limite", Timestamp.valueOf(limite.plusDays(1).atStartOfDay()));
        var eventos = jdbc.query("""
                SELECT ID_EVENTO, TP_EVENTO, DT_EVENTO, DS_DIAGNOSTICO, DS_OBSERVACAO
                FROM TB_EVENTO_CLINICO
                WHERE ID_PET = :id AND DT_EVENTO >= :inicio AND DT_EVENTO < :fim
                ORDER BY DT_EVENTO DESC, ID_EVENTO DESC
                """, params, (r, n) -> new EventoResumo(r.getLong("ID_EVENTO"), r.getString("TP_EVENTO"),
                dataHora(r, "DT_EVENTO"), r.getString("DS_DIAGNOSTICO"), r.getString("DS_OBSERVACAO")));
        LocalDateTime consulta = jdbc.queryForObject("""
                SELECT MAX(DT_EVENTO) ULTIMA FROM TB_EVENTO_CLINICO
                WHERE ID_PET = :id AND TP_EVENTO = 'CONSULTA' AND DT_EVENTO < :fim
                """, params, (r, n) -> dataHora(r, "ULTIMA"));
        var medicamentos = jdbc.query("""
                SELECT m.ID_MEDICAMENTO, m.NM_MEDICAMENTO, m.DS_DOSAGEM, m.DS_FREQUENCIA, m.DT_INICIO, m.DT_FIM
                FROM TB_MEDICAMENTO m JOIN TB_EVENTO_CLINICO e ON e.ID_EVENTO = m.ID_EVENTO
                WHERE e.ID_PET = :id AND e.DT_EVENTO < :fim AND m.DT_INICIO < :amanha
                    AND (m.DT_FIM IS NULL OR (m.DT_FIM >= :hoje AND m.DT_FIM >= m.DT_INICIO))
                ORDER BY m.DT_INICIO DESC, m.ID_MEDICAMENTO DESC
                """, params, (r, n) -> new MedicamentoResumo(r.getLong("ID_MEDICAMENTO"), r.getString("NM_MEDICAMENTO"),
                r.getString("DS_DOSAGEM"), r.getString("DS_FREQUENCIA"), data(r, "DT_INICIO"), data(r, "DT_FIM")));
        var vacinas = jdbc.query("""
                SELECT ID_PROTOCOLO, NM_PROTOCOLO, DT_PROXIMA, ST_STATUS FROM TB_PROTOCOLO_PREVENTIVO
                WHERE ID_PET = :id AND TP_PROTOCOLO = 'VACINA' AND DT_PROXIMA < :limite
                    AND (DT_APLICACAO IS NULL OR DT_APLICACAO < :amanha)
                ORDER BY DT_PROXIMA, ID_PROTOCOLO
                """, params, (r, n) -> new VacinaResumo(r.getLong("ID_PROTOCOLO"), r.getString("NM_PROTOCOLO"),
                data(r, "DT_PROXIMA"), r.getString("ST_STATUS")));
        return new Historico(inicio, consulta, List.copyOf(eventos), List.copyOf(medicamentos), limite, List.copyOf(vacinas));
    }

    private MapSqlParameterSource periodo(Long id, LocalDateTime inicio, LocalDateTime fim) {
        return new MapSqlParameterSource("id", id).addValue("inicio", Timestamp.valueOf(inicio))
                .addValue("fim", Timestamp.valueOf(fim));
    }

    private static Double numero(ResultSet r, String coluna) throws SQLException {
        Number valor = (Number) r.getObject(coluna);
        return valor == null ? null : valor.doubleValue();
    }

    private static LocalDateTime dataHora(ResultSet r, String coluna) throws SQLException {
        Timestamp valor = r.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private static LocalDate data(ResultSet r, String coluna) throws SQLException {
        LocalDateTime valor = dataHora(r, coluna);
        return valor == null ? null : valor.toLocalDate();
    }
}
