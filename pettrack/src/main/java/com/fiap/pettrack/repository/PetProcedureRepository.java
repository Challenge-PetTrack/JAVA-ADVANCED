package com.fiap.pettrack.repository;

import com.fiap.pettrack.model.Pet;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

@Repository
public class PetProcedureRepository {
    private final JdbcTemplate jdbc;

    public PetProcedureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long inserir(Pet pet) {
        Long id = jdbc.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement call = connection.prepareCall(
                    "{call PKG_PETTRACK_CADASTRO.PRC_INSERT_PET(?,?,?,?,?,?,?,?,?)}")) {
                call.setString(1, pet.getNome());
                call.setString(2, pet.getEspecie());
                call.setString(3, pet.getRaca());
                call.setString(4, pet.getSexo() == null ? null : pet.getSexo().name());
                call.setObject(5, pet.getIdade(), Types.NUMERIC);
                call.setObject(6, pet.getPeso(), Types.NUMERIC);
                call.setLong(7, pet.getTutor().getId());
                call.setLong(8, pet.getClinica().getId());
                call.registerOutParameter(9, Types.NUMERIC);
                call.execute();
                long generated = call.getLong(9);
                if (call.wasNull()) throw new SQLException("A procedure nao retornou o ID do pet.");
                return generated;
            }
        });
        if (id == null) throw new IllegalStateException("A procedure nao retornou o ID do pet.");
        return id;
    }

    public String exportarJson(String especie) {
        return jdbc.execute((ConnectionCallback<String>) connection -> {
            try (CallableStatement call = connection.prepareCall(
                    "{call PKG_PETTRACK_CONSULTA.PRC_LISTAR_PETS_JSON(?,?)}")) {
                call.setString(1, especie);
                call.registerOutParameter(2, Types.CLOB);
                call.execute();
                return lerClob(call.getClob(2));
            }
        });
    }

    public String relatorioAtividades() {
        return jdbc.execute((ConnectionCallback<String>) connection -> {
            try (CallableStatement call = connection.prepareCall(
                    "{call PKG_PETTRACK_CONSULTA.PRC_RELATORIO_SUBTOTAIS_MANUAL(?)}")) {
                call.registerOutParameter(1, Types.CLOB);
                call.execute();
                return lerClob(call.getClob(1));
            }
        });
    }

    private String lerClob(Clob clob) throws SQLException {
        if (clob == null) throw new SQLException("A procedure nao retornou o resultado.");
        try (Reader reader = clob.getCharacterStream(); StringWriter writer = new StringWriter()) {
            reader.transferTo(writer);
            return writer.toString();
        } catch (IOException e) {
            throw new SQLException("Falha ao ler o resultado da procedure.", e);
        } finally {
            clob.free();
        }
    }
}
