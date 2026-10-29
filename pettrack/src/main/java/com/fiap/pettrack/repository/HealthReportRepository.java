package com.fiap.pettrack.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.dto.HealthReportHistoryDTO.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HealthReportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public HealthReportRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void salvar(HealthReportDTO report, HealthContextDTO contexto, Long usuario, OffsetDateTime salvoEm) {
        var params = new MapSqlParameterSource().addValue("id", report.idGeracao().toString())
                .addValue("pet", report.idPet()).addValue("usuario", usuario)
                .addValue("gerado", report.geradoEm(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("contextoEm", report.contextoGeradoEm(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("salvo", salvoEm, Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("provedor", report.provedor()).addValue("modelo", report.modelo())
                .addValue("prompt", report.versaoPrompt()).addValue("simulado", report.simulado() ? "S" : "N")
                .addValue("nivel", report.relatorio().nivelAtencao().name())
                .addValue("relatorio", json(report), Types.CLOB).addValue("contexto", json(contexto), Types.CLOB);
        jdbc.update("""
                INSERT INTO TB_HEALTH_REPORT (ID_GERACAO, ID_PET, ID_SOLICITANTE, DT_GERACAO,
                    DT_CONTEXTO, DT_SALVO, DS_PROVEDOR, DS_MODELO, DS_VERSAO_PROMPT,
                    ST_SIMULADO, DS_NIVEL_ATENCAO, JS_RELATORIO, JS_CONTEXTO)
                VALUES (:id,:pet,:usuario,:gerado,:contextoEm,:salvo,:provedor,:modelo,:prompt,
                    :simulado,:nivel,:relatorio,:contexto)
                """, params);
    }

    public Optional<Detalhe> buscar(Long pet, UUID id) {
        return jdbc.query("SELECT * FROM TB_HEALTH_REPORT WHERE ID_PET=:pet AND ID_GERACAO=:id",
                new MapSqlParameterSource("pet", pet).addValue("id", id.toString()),
                (rs, row) -> detalhe(rs)).stream().findFirst();
    }

    public Optional<Detalhe> ultimo(Long pet) {
        return jdbc.query("""
                SELECT * FROM TB_HEALTH_REPORT WHERE ID_PET=:pet
                ORDER BY DT_SALVO DESC, ID_GERACAO DESC FETCH FIRST 1 ROWS ONLY
                """, new MapSqlParameterSource("pet", pet), (rs, row) -> detalhe(rs)).stream().findFirst();
    }

    public Pagina listar(Long pet, int pagina, int tamanho) {
        var params = new MapSqlParameterSource("pet", pet)
                .addValue("offset", (long) pagina * tamanho).addValue("tamanho", tamanho);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM TB_HEALTH_REPORT WHERE ID_PET=:pet", params, Long.class);
        var itens = jdbc.query("""
                SELECT ID_GERACAO, ID_PET, DT_GERACAO, DT_SALVO, DS_PROVEDOR, DS_MODELO, ST_SIMULADO, DS_NIVEL_ATENCAO
                FROM TB_HEALTH_REPORT WHERE ID_PET=:pet
                ORDER BY DT_SALVO DESC, ID_GERACAO DESC OFFSET :offset ROWS FETCH NEXT :tamanho ROWS ONLY
                """, params, (rs, row) -> new Resumo(UUID.fromString(rs.getString("ID_GERACAO")),
                rs.getLong("ID_PET"), data(rs,"DT_GERACAO"), data(rs,"DT_SALVO"),
                rs.getString("DS_PROVEDOR"), rs.getString("DS_MODELO"), "S".equals(rs.getString("ST_SIMULADO")),
                HealthReportDTO.NivelAtencao.valueOf(rs.getString("DS_NIVEL_ATENCAO"))));
        return new Pagina(itens, pagina, tamanho, total);
    }

    private Detalhe detalhe(ResultSet rs) throws SQLException {
        try {
            return new Detalhe(rs.getLong("ID_SOLICITANTE"), data(rs,"DT_SALVO"),
                    mapper.readValue(rs.getString("JS_RELATORIO"), HealthReportDTO.class),
                    mapper.readValue(rs.getString("JS_CONTEXTO"), HealthContextDTO.class));
        } catch (JsonProcessingException ex) {
            throw new SQLException("Registro de Health Report invalido");
        }
    }

    private OffsetDateTime data(ResultSet rs, String campo) throws SQLException {
        return rs.getObject(campo, OffsetDateTime.class);
    }

    private String json(Object objeto) {
        try { return mapper.writeValueAsString(objeto); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Falha ao serializar Health Report"); }
    }
}
