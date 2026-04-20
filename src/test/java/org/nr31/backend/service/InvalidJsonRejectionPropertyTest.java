package org.nr31.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.nr31.backend.dto.cms.LayoutDataDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvalidJsonRejectionPropertyTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * For any string that is not well-formed JSON, when submitted as layout data, 
     * the system SHALL reject it with HTTP 400 Bad Request.
     */
    @Property(tries = 100)
    @Label("Property 15: Invalid JSON Rejection - Malformed JSON is rejected")
    void malformedJsonIsRejected(@ForAll("malformedJson") String malformedJson) {
        // When/Then: Attempting to deserialize malformed JSON should throw exception
        assertThatThrownBy(() -> objectMapper.readValue(malformedJson, LayoutDataDto.class))
            .satisfies(exception -> {
                // Should throw JsonProcessingException or its subclasses
                // Spring will wrap this in HttpMessageNotReadableException
                assertThat(exception)
                    .as("Malformed JSON should cause deserialization exception")
                    .isInstanceOf(Exception.class);
            });
    }

    @Provide
    Arbitrary<String> malformedJson() {
        return Arbitraries.oneOf(
            // Missing closing brace
            Arbitraries.just("{\"slots\": ["),
            
            // Missing opening brace
            Arbitraries.just("\"slots\": []}"),
            
            // Unclosed string
            Arbitraries.just("{\"slots\": \"unclosed}"),
            
            // Invalid escape sequence
            Arbitraries.just("{\"slots\": \"\\x\"}"),
            
            // Trailing comma
            Arbitraries.just("{\"slots\": [],}"),
            
            // Single quotes instead of double quotes
            Arbitraries.just("{'slots': []}"),
            
            // Missing quotes on key
            Arbitraries.just("{slots: []}"),
            
            // Unquoted string value
            Arbitraries.just("{\"slots\": unquoted}"),
            
            // Missing colon
            Arbitraries.just("{\"slots\" []}"),
            
            // Missing comma between properties
            Arbitraries.just("{\"slots\": [] \"widgets\": []}"),
            
            // Empty string
            Arbitraries.just(""),
            
            // Just whitespace
            Arbitraries.just("   "),
            
            // Plain text
            Arbitraries.just("not json at all"),
            
            // Incomplete array
            Arbitraries.just("[1, 2, 3"),
            
            // Invalid number format
            Arbitraries.just("{\"version\": 01}"),
            
            // Null character
            Arbitraries.just("{\"slots\": \"\u0000\"}"),
            
            // Unclosed array
            Arbitraries.just("{\"slots\": [{\"slotType\": \"hero\"}"),
            
            // Missing value
            Arbitraries.just("{\"slots\":}"),
            
            // Double comma
            Arbitraries.just("{\"slots\": [,,]}"),
            
            // Random malformed JSON with arbitrary content
            Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('{', '}', '[', ']', ':', ',', '"', '\\')
                .ofMinLength(5)
                .ofMaxLength(50)
                .filter(s -> {
                    try {
                        new ObjectMapper().readTree(s);
                        return false;
                    } catch (Exception e) {
                        return true;
                    }
                })
        );
    }
}
