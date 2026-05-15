package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.ProtocoloPreventivoDTO;
import com.fiap.pettrack.model.ProtocoloPreventivo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IProtocoloPreventivoMapper {

    @Mapping(target = "idPet", source = "pet.id")
    ProtocoloPreventivoDTO toDTO(ProtocoloPreventivo protocolo);

    @Mapping(target = "pet.id", source = "idPet")
    ProtocoloPreventivo toEntity(ProtocoloPreventivoDTO protocoloDTO);
}