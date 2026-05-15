package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.AlertaDTO;
import com.fiap.pettrack.model.Alerta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IAlertaMapper {

    @Mapping(target = "idPet", source = "pet.id")
    AlertaDTO toDTO(Alerta alerta);

    @Mapping(target = "pet.id", source = "idPet")
    Alerta toEntity(AlertaDTO alertaDTO);
}