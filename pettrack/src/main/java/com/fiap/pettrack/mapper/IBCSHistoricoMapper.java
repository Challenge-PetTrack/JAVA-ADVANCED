package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.BCSHistoricoDTO;
import com.fiap.pettrack.model.BCSHistorico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IBCSHistoricoMapper {

    @Mapping(target = "idPet", source = "pet.id")
    BCSHistoricoDTO toDTO(BCSHistorico bcs);

    @Mapping(target = "pet.id", source = "idPet")
    BCSHistorico toEntity(BCSHistoricoDTO bcsDTO);
}