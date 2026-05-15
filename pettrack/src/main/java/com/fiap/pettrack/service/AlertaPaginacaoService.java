package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.AlertaDTO;
import com.fiap.pettrack.mapper.IAlertaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaPaginacaoService {

    @Autowired
    private AlertaCachingService alertaCachingService;

    @Autowired
    private IAlertaMapper iAlertaMapper;

    @Transactional(readOnly = true)
    public Page<AlertaDTO> paginar(PageRequest pageRequest) {
        return alertaCachingService.findAll(pageRequest)
                .map(iAlertaMapper::toDTO);
    }
}
