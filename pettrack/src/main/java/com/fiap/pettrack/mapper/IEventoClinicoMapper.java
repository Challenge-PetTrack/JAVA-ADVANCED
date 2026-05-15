package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.EventoClinicoDTO;
import com.fiap.pettrack.model.EventoClinico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IEventoClinicoMapper {

    @Mapping(target = "idPet", source = "pet.id")
    @Mapping(target = "idClinica", source = "clinica.id")
    EventoClinicoDTO toDTO(EventoClinico evento);

    @Mapping(target = "pet.id", source = "idPet")
    @Mapping(target = "clinica.id", source = "idClinica")
    EventoClinico toEntity(EventoClinicoDTO eventoDTO);
}