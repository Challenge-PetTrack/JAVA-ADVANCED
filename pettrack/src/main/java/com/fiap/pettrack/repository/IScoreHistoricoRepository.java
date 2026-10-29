package com.fiap.pettrack.repository;

import com.fiap.pettrack.model.ScoreHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IScoreHistoricoRepository extends JpaRepository<ScoreHistorico, Long> {

    @Query("SELECT s FROM ScoreHistorico s WHERE " +
            "s.pet.id = :idPet ORDER BY s.dataRegistro DESC")
    List<ScoreHistorico> buscarHistoricoPorPet(Long idPet);

    @Query(nativeQuery = true, value =
            "SELECT AVG(NR_SCORE) FROM TB_SCORE_HISTORICO " +
                    "WHERE ID_PET = :idPet")
    Double buscarMediaScorePorPet(Long idPet);
}
