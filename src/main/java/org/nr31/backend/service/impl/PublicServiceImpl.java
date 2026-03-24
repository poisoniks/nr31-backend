package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.SupportedLocaleDTO;
import org.nr31.backend.model.SupportedLocale;
import org.nr31.backend.repository.SupportedLocaleRepository;
import org.nr31.backend.service.PublicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final SupportedLocaleRepository supportedLocaleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupportedLocaleDTO> getSupportedLocales() {
        return supportedLocaleRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    private SupportedLocaleDTO convertToDTO(SupportedLocale locale) {
        return SupportedLocaleDTO.builder()
                .id(locale.getId())
                .code(locale.getCode())
                .description(locale.getDescription())
                .build();
    }
}
