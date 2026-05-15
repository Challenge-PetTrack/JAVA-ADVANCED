package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.NotificacaoDTO;
import com.fiap.pettrack.model.Notificacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface INotificacaoMapper {

    @Mapping(target = "idTutor", source = "tutor.id")
    @Mapping(target = "idPet", source = "pet.id")
    NotificacaoDTO toDTO(Notificacao notificacao);

    @Mapping(target = "tutor.id", source = "idTutor")
    @Mapping(target = "pet.id", source = "idPet")
    Notificacao toEntity(NotificacaoDTO notificacaoDTO);
}