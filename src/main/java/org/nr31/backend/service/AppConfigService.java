package org.nr31.backend.service;

import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppConfigService {
    AppConfigDto getConfig(String name);

    AppConfigDto getConfig(AppConfigKey key);

    Page<AppConfigDto> getAllConfigs(Pageable pageable);

    AppConfigDto updateConfig(String name, AppConfigDto appConfigDto);
}
