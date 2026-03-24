package org.nr31.backend.service;

import org.nr31.backend.dto.SupportedLocaleDTO;

import java.util.List;

public interface PublicService {
    List<SupportedLocaleDTO> getSupportedLocales();
}
