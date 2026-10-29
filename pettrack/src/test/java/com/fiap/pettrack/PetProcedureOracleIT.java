package com.fiap.pettrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.pettrack.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "PETTRACK_ORACLE_TESTS", matches = "true")
@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class PetProcedureOracleIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockBean DataInitializer initializer;

    @Test @WithMockUser(roles="ADMIN")
    void apiInserePetPorProcedureEAuditoriaNaMesmaTransacao() throws Exception {
        long total = totalPets();
        String nome = "SPRINT4_API_" + System.nanoTime();
        mvc.perform(post("/pet/novo").contentType("application/json").content(payload(nome, tutor(), clinica())))
                .andExpect(status().isCreated());
        assertEquals(total+1, totalPets());
        Long id = jdbc.queryForObject("SELECT ID_PET FROM TB_PET WHERE NM_PET=?", Long.class, nome);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM TB_AUDITORIA_PETTRACK WHERE TP_OPERACAO='INSERT' AND DS_VALORES_NOVOS LIKE ?",
                Integer.class, "ID="+id+" | Nome="+nome+"%"));
    }

    @Test @WithMockUser(roles="ADMIN")
    void formularioExistenteInserePorProcedure() throws Exception {
        String nome = "SPRINT4_WEB_" + System.nanoTime();
        mvc.perform(post("/admin/pets/salvar").with(csrf()).param("nome", nome)
                .param("especie", "Cao").param("sexo", "M").param("peso", "10.5")
                .param("tutor.id", ""+tutor()).param("clinica.id", ""+clinica()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/pets"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM TB_PET WHERE NM_PET=?", Integer.class, nome));
    }

    @Test @WithMockUser(roles="ADMIN")
    void fkInvalidaRetornaErroESemInsercao() throws Exception {
        long total = totalPets();
        mvc.perform(post("/pet/novo").contentType("application/json")
                .content(payload("SPRINT4_FK_INVALIDA", -999999L, clinica())))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(total, totalPets());
    }

    @Test @WithMockUser(roles="ADMIN")
    void exportaDadosReaisComIdsRelacionadosEGravaDatasetSeSolicitado() throws Exception {
        String result = mvc.perform(get("/pet/exportar-json")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"pets.json\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode docs = json.readTree(result);
        assertEquals(totalPets(), docs.size());
        for (JsonNode doc : docs) {
            Map<String,Object> row = jdbc.queryForMap("SELECT ID_TUTOR, ID_CLINICA FROM TB_PET WHERE ID_PET=?", doc.path("idPet").asLong());
            assertEquals(((Number)row.get("ID_TUTOR")).longValue(), doc.path("tutor").path("id").asLong());
            assertEquals(((Number)row.get("ID_CLINICA")).longValue(), doc.path("clinica").path("id").asLong());
        }
        String output = System.getProperty("pettrack.dataset");
        if (output != null) {
            Path file = Path.of(output); Files.createDirectories(file.getParent());
            Files.writeString(file, result, StandardCharsets.UTF_8);
        }
    }

    @Test @WithMockUser(roles="ADMIN")
    void relatorioDaProcedureContemTotalConferidoComBanco() throws Exception {
        String result = mvc.perform(get("/pet/relatorio-atividades")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Sub Total"));
        assertTrue(result.contains("Total Geral"));
        String linha = result.lines().filter(s -> s.contains("Total Geral")).findFirst().orElseThrow();
        double valor = Double.parseDouble(linha.replace("Total Geral", "").trim().replace(',', '.'));
        double esperado = jdbc.queryForObject("SELECT SUM(NVL(L.NR_ATIVIDADE,0)) FROM TB_COLLAR_LEITURA L JOIN TB_PET P ON P.ID_PET=L.ID_PET JOIN TB_CLINICA C ON C.ID_CLINICA=P.ID_CLINICA", Double.class);
        assertEquals(esperado, valor, .001);
    }

    @Test @WithMockUser(roles="TUTOR")
    void tutorNaoExportaDatasetNemRelatorio() throws Exception {
        mvc.perform(get("/pet/exportar-json")).andExpect(status().isForbidden());
        mvc.perform(get("/pet/relatorio-atividades")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="ADMIN")
    void exportacaoTrataFiltroInvalidoESemResultados() throws Exception {
        mvc.perform(get("/pet/exportar-json").param("especie","x")).andExpect(status().isBadRequest());
        mvc.perform(get("/pet/exportar-json").param("especie","ESPECIE_INEXISTENTE_SPRINT4"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void serializacaoOracleEscapaCaracteresEIndependeDeNls() throws Exception {
        jdbc.execute("ALTER SESSION SET NLS_NUMERIC_CHARACTERS=',.'");
        try {
            String result = jdbc.queryForObject("SELECT PKG_PETTRACK_CONSULTA.FN_MONTAR_JSON_PET(?,?,?,?,?,?,?,?,?) FROM DUAL",
                    String.class, 1, "Pet \"A\"\\teste\n", "Ca\to", "Raça\rTeste", 12.345, "Tutor\b", "Clinica", tutor(), clinica());
            JsonNode doc = json.readTree(result);
            assertEquals("Pet \"A\"\\teste\n", doc.path("nome").asText());
            assertEquals("Ca\to", doc.path("especie").asText());
            assertEquals(12.345, doc.path("pesoKg").asDouble(), .0001);
        } finally { jdbc.execute("ALTER SESSION SET NLS_NUMERIC_CHARACTERS='.,'"); }
    }

    @Test void funcaoAdesaoEmpacotadaPreservaCalculoExistente() {
        Long id = jdbc.queryForObject("SELECT MIN(ID_MEDICAMENTO) FROM TB_ADESAO_MEDICAMENTO", Long.class);
        assertNotNull(id);
        Double original = jdbc.queryForObject("SELECT FN_CALCULAR_TAXA_ADESAO(?) FROM DUAL", Double.class, id);
        Double empacotado = jdbc.queryForObject("SELECT PKG_PETTRACK_CONSULTA.FN_CALCULAR_TAXA_ADESAO(?) FROM DUAL", Double.class, id);
        assertEquals(original, empacotado);
    }

    private long totalPets() { return jdbc.queryForObject("SELECT COUNT(*) FROM TB_PET", Long.class); }
    private long tutor() { return jdbc.queryForObject("SELECT MIN(ID_TUTOR) FROM TB_TUTOR", Long.class); }
    private long clinica() { return jdbc.queryForObject("SELECT MIN(ID_CLINICA) FROM TB_CLINICA", Long.class); }
    private String payload(String nome, long tutor, long clinica) throws Exception {
        return json.writeValueAsString(Map.of("nome",nome,"especie","Cao","sexo","M","peso",10.5,
                "tutor",Map.of("id",tutor),"clinica",Map.of("id",clinica)));
    }
}
