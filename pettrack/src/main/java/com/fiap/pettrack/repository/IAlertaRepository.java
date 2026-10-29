package com.fiap.pettrack.repository;

import com.fiap.pettrack.model.Alerta;
import com.fiap.pettrack.model.enums.TipoAlertaEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IAlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByTipoAlerta(TipoAlertaEnum tipoAlerta);

    List<Alerta> findByResolvido(com.fiap.pettrack.model.enums.SimNaoEnum resolvido);

    @Query("SELECT a FROM Alerta a WHERE " +
            "a.pet.id = :idPet AND a.resolvido = 'N'")
    List<Alerta> buscarPendentesPorPet(Long idPet);
}
