package org.nr31.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.cms.*;
import org.nr31.backend.exception.AppConfigValidationException;
import org.nr31.backend.service.impl.ValidationServiceImpl;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlotRestrictionValidationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppConfigService appConfigService = mock(AppConfigService.class);
    private final ValidationService validationService = new ValidationServiceImpl(appConfigService, objectMapper);

    /**
     * For any layout data containing a widget placed in a slot, if the widget type 
     * is not in the allowed widget types for that slot type according to the slot 
     * restrictions configuration, the system SHALL reject the layout data with 
     * HTTP 400 Bad Request and include both the slot type and widget type in the 
     * error message.
     */
    @Property(tries = 100)
    @Label("Property 4: Slot Restriction Validation - Invalid placements are rejected")
    void invalidWidgetPlacementsAreRejected(
        @ForAll("invalidWidgetSlotCombinations") LayoutDataDto layoutData
    ) throws Exception {
        // Setup slot restrictions for this test
        Map<String, List<String>> restrictions = Map.of(
            "hero", List.of("text", "image"),
            "sidebar", List.of("text"),
            "content", List.of("text", "image", "video", "embed")
        );
        
        String restrictionsJson = objectMapper.writeValueAsString(restrictions);
        AppConfigDto configDto = AppConfigDto.builder()
            .name("cms_slot_restrictions")
            .configValue(restrictionsJson)
            .build();
        
        when(appConfigService.getConfig("cms_slot_restrictions")).thenReturn(configDto);
        // When: Validating layout with invalid widget-slot combination
        // Then: Should throw AppConfigValidationException
        assertThatThrownBy(() -> validationService.validateLayout(layoutData))
            .isInstanceOf(AppConfigValidationException.class)
            .satisfies(exception -> {
                AppConfigValidationException validationException = (AppConfigValidationException) exception;
                Map<String, String> errors = validationException.getErrors();
                
                // Verify error details contain slot type and widget type information
                assertThat(errors).isNotEmpty();
                
                // Check that at least one error message contains both slot type and widget type
                boolean hasSlotAndWidgetInfo = errors.values().stream()
                    .anyMatch(errorMsg -> 
                        errorMsg.contains("Widget type") && 
                        errorMsg.contains("is not allowed in slot type")
                    );
                
                assertThat(hasSlotAndWidgetInfo)
                    .as("Error message should include both slot type and widget type")
                    .isTrue();
            });
    }

    @Provide
    Arbitrary<LayoutDataDto> invalidWidgetSlotCombinations() {
        // Define slot types and their restrictions
        Map<String, List<String>> slotRestrictions = Map.of(
            "hero", List.of("text", "image"),
            "sidebar", List.of("text"),
            "content", List.of("text", "image", "video", "embed")
        );
        
        // Generate combinations where widget type is NOT in the allowed list
        return Combinators.combine(
            Arbitraries.of("hero", "sidebar", "content"),
            Arbitraries.of("text", "image", "video", "embed"),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50)
        ).as((slotType, widgetType, randomContent) -> {
            List<String> allowedTypes = slotRestrictions.get(slotType);
            // Only return combinations where widget is NOT allowed in slot
            if (allowedTypes != null && !allowedTypes.contains(widgetType)) {
                return createLayoutWithWidget(slotType, widgetType, randomContent);
            }
            // If widget is allowed, return a different invalid combination
            // This ensures we always return an invalid combination
            return createLayoutWithWidget("sidebar", "video", randomContent);
        }).filter(layout -> {
            // Verify this is actually an invalid combination
            SlotDto slot = layout.getSlots().get(0);
            WidgetDto widget = slot.getWidgets().get(0);
            String widgetType = getWidgetType(widget);
            List<String> allowed = slotRestrictions.get(slot.getSlotType());
            return allowed != null && !allowed.contains(widgetType);
        });
    }

    private LayoutDataDto createLayoutWithWidget(String slotType, String widgetType, String randomContent) {
        WidgetDto widget = switch (widgetType) {
            case "text" -> createTextWidget(randomContent);
            case "image" -> createImageWidget(randomContent);
            case "video" -> createVideoWidget(randomContent);
            case "embed" -> createEmbedWidget(randomContent);
            default -> throw new IllegalArgumentException("Unknown widget type: " + widgetType);
        };

        SlotDto slot = new SlotDto(slotType, List.of(widget));
        return new LayoutDataDto(List.of(slot));
    }

    private TextWidgetDto createTextWidget(String content) {
        TextWidgetDto widget = new TextWidgetDto();
        widget.setContent("<p>" + content + "</p>");
        return widget;
    }

    private ImageWidgetDto createImageWidget(String filename) {
        ImageWidgetDto widget = new ImageWidgetDto();
        widget.setUrl("https://example.com/" + filename);
        widget.setAlt("Sample image");
        return widget;
    }

    private VideoWidgetDto createVideoWidget(String filename) {
        VideoWidgetDto widget = new VideoWidgetDto();
        widget.setUrl("https://example.com/" + filename);
        return widget;
    }

    private EmbedWidgetDto createEmbedWidget(String url) {
        EmbedWidgetDto widget = new EmbedWidgetDto();
        widget.setEmbedCode("<iframe src='" + url + "'></iframe>");
        return widget;
    }

    /**
     * Determines the widget type from the WidgetDto instance using instanceof checks.
     * Maps concrete widget classes to their @JsonTypeName values.
     */
    private String getWidgetType(WidgetDto widget) {
        if (widget instanceof TextWidgetDto) {
            return "text";
        } else if (widget instanceof ImageWidgetDto) {
            return "image";
        } else if (widget instanceof VideoWidgetDto) {
            return "video";
        } else if (widget instanceof EmbedWidgetDto) {
            return "embed";
        }
        throw new IllegalArgumentException("Unknown widget type: " + widget.getClass().getName());
    }
}
