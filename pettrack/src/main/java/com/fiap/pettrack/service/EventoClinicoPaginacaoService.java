package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.EventoClinicoDTO;
import com.fiap.pettrack.mapper.IEventoClinicoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoClinicoPaginacaoService {

    @Autowired
    private EventoClinicoCachingService eventoClinicoCachingService;

    @Autowired
    private IEventoClinicoMapper iEventoClinicoMapper;

    @Transactional(readOnly = true)
    public Page<EventoClinicoDTO> paginar(PageRequest pageRequest) {
        return eventoClinicoCachingService.findAll(pageRequest)
                .map(iEventoClinicoMapper::toDTO);
    }
}