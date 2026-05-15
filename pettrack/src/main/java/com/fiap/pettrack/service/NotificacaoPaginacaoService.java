package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.NotificacaoDTO;
import com.fiap.pettrack.mapper.INotificacaoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoPaginacaoService {

    @Autowired
    private NotificacaoCachingService notificacaoCachingService;

    @Autowired
    private INotificacaoMapper iNotificacaoMapper;

    @Transactional(readOnly = true)
    public Page<NotificacaoDTO> paginar(PageRequest pageRequest) {
        return notificacaoCachingService.findAll(pageRequest)
                .map(iNotificacaoMapper::toDTO);
    }
}