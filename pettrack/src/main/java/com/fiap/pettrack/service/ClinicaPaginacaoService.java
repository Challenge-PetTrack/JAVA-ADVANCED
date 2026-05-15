package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.ClinicaDTO;
import com.fiap.pettrack.mapper.IClinicaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicaPaginacaoService {

    @Autowired
    private ClinicaCachingService clinicaCachingService;

    @Autowired
    private IClinicaMapper iClinicaMapper;

    @Transactional(readOnly = true)
    public Page<ClinicaDTO> paginar(PageRequest pageRequest) {
        return clinicaCachingService.findAll(pageRequest)
                .map(iClinicaMapper::toDTO);
    }
}