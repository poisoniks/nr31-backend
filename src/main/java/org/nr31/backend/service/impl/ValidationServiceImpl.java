package org.nr31.backend.service.impl;

import org.nr31.backend.dto.cms.DiscordWidgetDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.cms.HeroWidgetDto;
import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.NewsFeedWidgetDto;
import org.nr31.backend.dto.cms.NextEventWidgetDto;
import org.nr31.backend.dto.cms.RichTextWidgetDto;
import org.nr31.backend.dto.cms.YoutubeWidgetDto;
import org.nr31.backend.dto.cms.SlotDto;
import org.nr31.backend.dto.cms.SlotRestrictionsDto;
import org.nr31.backend.dto.cms.UpdateSlotRestrictionsRequest;
import org.nr31.backend.dto.cms.WidgetDto;
import org.nr31.backend.exception.AppConfigValidationException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.ValidationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {

    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper;

    private static final String SLOT_RESTRICTIONS_CONFIG_KEY = "cms_slot_restrictions";

    @Override
    public void validateLayout(LayoutDataDto layoutData) {
        SlotRestrictionsDto restrictions = getSlotRestrictions();
        Map<String, List<String>> restrictionsMap = restrictions.getRestrictions();

        if (layoutData.getSlots() == null) {
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();

        for (int slotIndex = 0; slotIndex < layoutData.getSlots().size(); slotIndex++) {
            SlotDto slot = layoutData.getSlots().get(slotIndex);
            String slotType = slot.getSlotType();
            List<String> allowedWidgetTypes = restrictionsMap.get(slotType);

            if (slot.getWidgets() == null) {
                continue;
            }

            for (int widgetIndex = 0; widgetIndex < slot.getWidgets().size(); widgetIndex++) {
                WidgetDto widget = slot.getWidgets().get(widgetIndex);
                String widgetType = getWidgetType(widget);

                if (allowedWidgetTypes != null && !allowedWidgetTypes.contains(widgetType)) {
                    String fieldPath = String.format("slots[%d].widgets[%d]", slotIndex, widgetIndex);
                    String errorMessage = String.format(
                        "Widget type '%s' is not allowed in slot type '%s'",
                        widgetType,
                        slotType
                    );
                    errors.put(fieldPath, errorMessage);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AppConfigValidationException("Layout validation failed", errors);
        }
    }

    @Override
    @Cacheable("slotRestrictions")
    public SlotRestrictionsDto getSlotRestrictions() {
        try {
            AppConfigDto config = appConfigService.getConfig(SLOT_RESTRICTIONS_CONFIG_KEY);
            Map<String, List<String>> restrictions = objectMapper.readValue(
                config.getConfigValue(),
                objectMapper.getTypeFactory().constructMapType(
                    Map.class,
                    objectMapper.getTypeFactory().constructType(String.class),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                )
            );
            return new SlotRestrictionsDto(restrictions);
        } catch (ElementNotFoundException e) {
            return new SlotRestrictionsDto(Collections.emptyMap());
        } catch (JacksonException e) {
            throw new AppConfigValidationException(
                "Failed to parse slot restrictions configuration",
                Map.of("configValue", "Invalid JSON format")
            );
        }
    }

    @Override
    @CacheEvict(value = "slotRestrictions", allEntries = true)
    public void updateSlotRestrictions(UpdateSlotRestrictionsRequest request) {
        Map<String, List<String>> restrictions = request.getRestrictions();

        Map<String, String> errors = new LinkedHashMap<>();

        if (restrictions == null) {
            errors.put("restrictions", "Restrictions must not be null");
            throw new AppConfigValidationException("Slot restrictions validation failed", errors);
        }

        for (Map.Entry<String, List<String>> entry : restrictions.entrySet()) {
            String slotType = entry.getKey();
            List<String> widgetTypes = entry.getValue();

            if (slotType == null || slotType.isBlank()) {
                errors.put("restrictions." + slotType, "Slot type must be a valid string");
            }

            if (widgetTypes == null) {
                errors.put("restrictions." + slotType, "Widget types array must not be null");
            } else {
                for (int i = 0; i < widgetTypes.size(); i++) {
                    String widgetType = widgetTypes.get(i);
                    if (widgetType == null || widgetType.isBlank()) {
                        errors.put(
                            String.format("restrictions.%s[%d]", slotType, i),
                            "Widget type must be a valid string"
                        );
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AppConfigValidationException("Slot restrictions validation failed", errors);
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(restrictions);
            AppConfigDto configDto = AppConfigDto.builder()
                .name(SLOT_RESTRICTIONS_CONFIG_KEY)
                .configValue(jsonValue)
                .description(Map.of("en", "CMS slot restrictions configuration"))
                .build();

            appConfigService.updateConfig(SLOT_RESTRICTIONS_CONFIG_KEY, configDto);
        } catch (JacksonException e) {
            throw new AppConfigValidationException(
                "Failed to serialize slot restrictions",
                Map.of("restrictions", "Serialization error")
            );
        }
    }

    private String getWidgetType(WidgetDto widget) {
        if (widget instanceof HeroWidgetDto) {
            return "hero";
        } else if (widget instanceof RichTextWidgetDto) {
            return "richtext";
        } else if (widget instanceof NextEventWidgetDto) {
            return "nextevent";
        } else if (widget instanceof NewsFeedWidgetDto) {
            return "newsfeed";
        } else if (widget instanceof YoutubeWidgetDto) {
            return "youtube";
        } else if (widget instanceof DiscordWidgetDto) {
            return "discord";
        }
        throw new IllegalArgumentException("Unknown widget type: " + widget.getClass().getSimpleName());
    }
}
