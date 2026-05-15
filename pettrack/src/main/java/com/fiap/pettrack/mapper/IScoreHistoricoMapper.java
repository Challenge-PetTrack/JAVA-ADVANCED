package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.ScoreHistoricoDTO;
import com.fiap.pettrack.model.ScoreHistorico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IScoreHistoricoMapper {

    @Mapping(target = "idPet", source = "pet.id")
    ScoreHistoricoDTO toDTO(ScoreHistorico score);

    @Mapping(target = "pet.id", source = "idPet")
    ScoreHistorico toEntity(ScoreHistoricoDTO scoreDTO);
}