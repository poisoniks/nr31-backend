package org.nr31.backend.service;

import org.nr31.backend.dto.SupportedLocaleDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicService {
    Page<SupportedLocaleDTO> getSupportedLocales(Pageable pageable);
}
