package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.PetDTO;
import com.fiap.pettrack.model.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IPetMapper {

    @Mapping(target = "idTutor", source = "tutor.id")
    @Mapping(target = "idClinica", source = "clinica.id")
    PetDTO toDTO(Pet pet);

    @Mapping(target = "tutor.id", source = "idTutor")
    @Mapping(target = "clinica.id", source = "idClinica")
    Pet toEntity(PetDTO petDTO);
}