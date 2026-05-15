package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.CollarLeituraDTO;
import com.fiap.pettrack.model.CollarLeitura;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ICollarLeituraMapper {

    @Mapping(target = "idPet", source = "pet.id")
    CollarLeituraDTO toDTO(CollarLeitura collar);

    @Mapping(target = "pet.id", source = "idPet")
    CollarLeitura toEntity(CollarLeituraDTO collarDTO);
}