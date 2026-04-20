package org.nr31.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.nr31.backend.dto.cms.UpdateSlotRestrictionsRequest;
import org.nr31.backend.exception.AppConfigValidationException;
import org.nr31.backend.service.impl.ValidationServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SlotRestrictionsStructurePropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppConfigService appConfigService = mock(AppConfigService.class);
    private final ValidationService validationService = new ValidationServiceImpl(appConfigService, objectMapper);

    /**
     * For any slot restrictions update where the structure does not conform to the 
     * required format (map of string keys to string array values), the system SHALL 
     * reject the update with HTTP 400 Bad Request.
     */
    @Property(tries = 100)
    @Label("Property 16: Slot Restrictions Structure Validation - Invalid structures are rejected")
    void invalidSlotRestrictionsStructuresAreRejected(
        @ForAll("invalidSlotRestrictions") UpdateSlotRestrictionsRequest request
    ) {
        // When: Attempting to update slot restrictions with invalid structure
        // Then: Should throw AppConfigValidationException
        assertThatThrownBy(() -> validationService.updateSlotRestrictions(request))
            .isInstanceOf(AppConfigValidationException.class)
            .satisfies(exception -> {
                AppConfigValidationException validationException = (AppConfigValidationException) exception;
                Map<String, String> errors = validationException.getErrors();
                
                // Verify error details are present
                assertThat(errors)
                    .as("Validation errors should be present for invalid structure")
                    .isNotEmpty();
                
                // Verify error message indicates validation failure
                assertThat(validationException.getMessage())
                    .as("Exception message should indicate validation failure")
                    .contains("validation failed");
            });
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> invalidSlotRestrictions() {
        return Arbitraries.oneOf(
            restrictionsWithNullSlotType(),
            restrictionsWithBlankSlotType(),
            restrictionsWithNullWidgetTypeArray(),
            restrictionsWithNullWidgetTypeInArray(),
            restrictionsWithBlankWidgetTypeInArray()
        );
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> restrictionsWithNullSlotType() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
            .list().ofMinSize(1).ofMaxSize(3)
            .map(widgetTypes -> {
                Map<String, List<String>> restrictions = new HashMap<>();
                restrictions.put(null, widgetTypes);
                return new UpdateSlotRestrictionsRequest(restrictions);
            });
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> restrictionsWithBlankSlotType() {
        return Combinators.combine(
            Arbitraries.of("", "   ", "\t", "\n"),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .list().ofMinSize(1).ofMaxSize(3)
        ).as((blankSlotType, widgetTypes) -> {
            Map<String, List<String>> restrictions = new HashMap<>();
            restrictions.put(blankSlotType, widgetTypes);
            return new UpdateSlotRestrictionsRequest(restrictions);
        });
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> restrictionsWithNullWidgetTypeArray() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
            .map(slotType -> {
                Map<String, List<String>> restrictions = new HashMap<>();
                restrictions.put(slotType, null);
                return new UpdateSlotRestrictionsRequest(restrictions);
            });
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> restrictionsWithNullWidgetTypeInArray() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
            .map(slotType -> {
                Map<String, List<String>> restrictions = new HashMap<>();
                List<String> widgetTypes = new ArrayList<>();
                
                // Add one null widget type (guaranteed to be invalid)
                widgetTypes.add(null);
                // Add some valid widget types
                widgetTypes.add("widget1");
                widgetTypes.add("widget2");
                
                restrictions.put(slotType, widgetTypes);
                return new UpdateSlotRestrictionsRequest(restrictions);
            });
    }

    @Provide
    Arbitrary<UpdateSlotRestrictionsRequest> restrictionsWithBlankWidgetTypeInArray() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.of("", "   ", "\t", "\n")
        ).as((slotType, blankWidgetType) -> {
            Map<String, List<String>> restrictions = new HashMap<>();
            List<String> widgetTypes = new ArrayList<>();
            
            // Add one blank widget type (guaranteed to be invalid)
            widgetTypes.add(blankWidgetType);
            // Add some valid widget types
            widgetTypes.add("widget1");
            widgetTypes.add("widget2");
            
            restrictions.put(slotType, widgetTypes);
            return new UpdateSlotRestrictionsRequest(restrictions);
        });
    }
}
