package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.ProtocoloPreventivoDTO;
import com.fiap.pettrack.mapper.IProtocoloPreventivoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloPreventivoPaginacaoService {

    @Autowired
    private ProtocoloPreventivoCachingService protocoloPreventivoCachingService;

    @Autowired
    private IProtocoloPreventivoMapper iProtocoloPreventivoMapper;

    @Transactional(readOnly = true)
    public Page<ProtocoloPreventivoDTO> paginar(PageRequest pageRequest) {
        return protocoloPreventivoCachingService.findAll(pageRequest)
                .map(iProtocoloPreventivoMapper::toDTO);
    }
}