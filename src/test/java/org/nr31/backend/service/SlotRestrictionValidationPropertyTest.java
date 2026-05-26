package org.nr31.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.dto.cms.*;
import org.nr31.backend.exception.AppConfigValidationException;
import org.nr31.backend.service.impl.ValidationServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    ) {
        // Setup slot restrictions for this test
        Map<String, List<String>> restrictions = Map.of(
            "hero-slot", List.of("hero"),
            "sidebar", List.of("nextevent", "newsfeed"),
            "content", List.of("richtext", "nextevent", "newsfeed")
        );
        JsonNode restrictionsNode = objectMapper.valueToTree(restrictions);
        AppConfigDto configDto = AppConfigDto.builder()
            .name("cms_slot_restrictions")
            .configValue(restrictionsNode)
            .build();
        
        when(appConfigService.getConfig(AppConfigKey.CMS_SLOT_RESTRICTIONS)).thenReturn(configDto);
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
            "hero-slot", List.of("hero"),
            "sidebar", List.of("nextevent", "newsfeed"),
            "content", List.of("richtext", "nextevent", "newsfeed")
        );
        
        // Generate combinations where widget type is NOT in the allowed list
        return Combinators.combine(
            Arbitraries.of("hero-slot", "sidebar", "content"),
            Arbitraries.of("hero", "richtext", "nextevent", "newsfeed"),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50)
        ).as((slotType, widgetType, randomContent) -> {
            List<String> allowedTypes = slotRestrictions.get(slotType);
            // Only return combinations where widget is NOT allowed in slot
            if (allowedTypes != null && !allowedTypes.contains(widgetType)) {
                return createLayoutWithWidget(slotType, widgetType, randomContent);
            }
            // If widget is allowed, return a different invalid combination
            // This ensures we always return an invalid combination
            return createLayoutWithWidget("sidebar", "hero", randomContent);
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
            case "hero" -> createHeroWidget(randomContent);
            case "richtext" -> createRichTextWidget(randomContent);
            case "nextevent" -> createNextEventWidget(randomContent);
            case "newsfeed" -> createNewsFeedWidget(randomContent);
            default -> throw new IllegalArgumentException("Unknown widget type: " + widgetType);
        };

        SlotDto slot = new SlotDto(slotType, List.of(widget));
        return new LayoutDataDto(List.of(slot));
    }

    private HeroWidgetDto createHeroWidget(String content) {
        HeroWidgetDto widget = new HeroWidgetDto();
        widget.setBadgeText(Map.of("en", content));
        widget.setTitleMain("Nr.31");
        widget.setTitleSub("FKR");
        widget.setDescription(Map.of("en", "Description " + content));
        widget.setCtaText(Map.of("en", "Join"));
        widget.setCtaTargetId("join-section");
        widget.setBackgroundImageId(UUID.randomUUID());
        return widget;
    }

    private RichTextWidgetDto createRichTextWidget(String content) {
        RichTextWidgetDto widget = new RichTextWidgetDto();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonContent = mapper.readTree(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + content + "\"}]}]}"
            );
            widget.setBodyContent(Map.of("en", jsonContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RichTextWidget", e);
        }
        return widget;
    }

    private NextEventWidgetDto createNextEventWidget(String content) {
        NextEventWidgetDto widget = new NextEventWidgetDto();
        widget.setTitleOverride(Map.of("en", "Event " + content));
        return widget;
    }

    private NewsFeedWidgetDto createNewsFeedWidget(String content) {
        NewsFeedWidgetDto widget = new NewsFeedWidgetDto();
        widget.setSectionTitle(Map.of("en", "News " + content));
        widget.setItemCount(3);
        widget.setTagFilter(null);
        return widget;
    }

    /**
     * Determines the widget type from the WidgetDto instance using instanceof checks.
     * Maps concrete widget classes to their @JsonTypeName values.
     */
    private String getWidgetType(WidgetDto widget) {
        if (widget instanceof HeroWidgetDto) {
            return "hero";
        } else if (widget instanceof RichTextWidgetDto) {
            return "richtext";
        } else if (widget instanceof NextEventWidgetDto) {
            return "nextevent";
        } else if (widget instanceof NewsFeedWidgetDto) {
            return "newsfeed";
        }
        throw new IllegalArgumentException("Unknown widget type: " + widget.getClass().getName());
    }
}
