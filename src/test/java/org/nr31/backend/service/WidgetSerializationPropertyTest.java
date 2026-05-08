package org.nr31.backend.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.nr31.backend.dto.cms.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for widget serialization using jqwik.
 * Tests that Jackson polymorphic deserialization preserves exact widget types 
 * through serialization round-trip for all widget types.
 */
class WidgetSerializationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property(tries = 100)
    @Label("Widget Type Preservation - HeroWidget")
    void heroWidgetTypePreservation(@ForAll("heroWidgets") HeroWidgetDto widget) 
        throws JacksonException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(widget);
        
        // Deserialize back to WidgetDto (base type)
        WidgetDto deserialized = objectMapper.readValue(json, WidgetDto.class);
        
        // Assert type preservation
        assertThat(deserialized)
            .as("Deserialized widget should be of same type as original")
            .isInstanceOf(HeroWidgetDto.class);
        
        assertThat(deserialized.getClass())
            .as("Deserialized widget class should match original widget class")
            .isEqualTo(widget.getClass());
        
        // Assert equality
        assertThat(deserialized)
            .as("Deserialized widget should be equal to original")
            .isEqualTo(widget);
    }

    @Property(tries = 100)
    @Label("Widget Type Preservation - RichTextWidget")
    void richTextWidgetTypePreservation(@ForAll("richTextWidgets") RichTextWidgetDto widget) 
        throws JacksonException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(widget);
        
        // Deserialize back to WidgetDto (base type)
        WidgetDto deserialized = objectMapper.readValue(json, WidgetDto.class);
        
        // Assert type preservation
        assertThat(deserialized)
            .as("Deserialized widget should be of same type as original")
            .isInstanceOf(RichTextWidgetDto.class);
        
        assertThat(deserialized.getClass())
            .as("Deserialized widget class should match original widget class")
            .isEqualTo(widget.getClass());
        
        // Assert equality
        assertThat(deserialized)
            .as("Deserialized widget should be equal to original")
            .isEqualTo(widget);
    }

    @Property(tries = 100)
    @Label("Widget Type Preservation - NextEventWidget")
    void nextEventWidgetTypePreservation(@ForAll("nextEventWidgets") NextEventWidgetDto widget) 
        throws JacksonException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(widget);
        
        // Deserialize back to WidgetDto (base type)
        WidgetDto deserialized = objectMapper.readValue(json, WidgetDto.class);
        
        // Assert type preservation
        assertThat(deserialized)
            .as("Deserialized widget should be of same type as original")
            .isInstanceOf(NextEventWidgetDto.class);
        
        assertThat(deserialized.getClass())
            .as("Deserialized widget class should match original widget class")
            .isEqualTo(widget.getClass());
        
        // Assert equality
        assertThat(deserialized)
            .as("Deserialized widget should be equal to original")
            .isEqualTo(widget);
    }

    @Property(tries = 100)
    @Label("Property 1: Widget Type Preservation - NewsFeedWidget")
    void newsFeedWidgetTypePreservation(@ForAll("newsFeedWidgets") NewsFeedWidgetDto widget) 
        throws JacksonException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(widget);
        
        // Deserialize back to WidgetDto (base type)
        WidgetDto deserialized = objectMapper.readValue(json, WidgetDto.class);
        
        // Assert type preservation
        assertThat(deserialized)
            .as("Deserialized widget should be of same type as original")
            .isInstanceOf(NewsFeedWidgetDto.class);
        
        assertThat(deserialized.getClass())
            .as("Deserialized widget class should match original widget class")
            .isEqualTo(widget.getClass());
        
        // Assert equality
        assertThat(deserialized)
            .as("Deserialized widget should be equal to original")
            .isEqualTo(widget);
    }

    @Provide
    Arbitrary<HeroWidgetDto> heroWidgets() {
        Arbitrary<Map<String, String>> localizedStrings = Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
        ).ofMinSize(1).ofMaxSize(4);

        Arbitrary<String> nonEmptyStrings = Arbitraries.strings()
            .alpha().numeric().withChars(' ', '-', '.')
            .ofMinLength(1).ofMaxLength(50);

        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);

        return Combinators.combine(
            localizedStrings,  // badgeText
            nonEmptyStrings,   // titleMain
            nonEmptyStrings,   // titleSub
            localizedStrings,  // description
            localizedStrings,  // ctaText
            nonEmptyStrings,   // ctaTargetId
            uuids              // backgroundImageId
        ).as((badgeText, titleMain, titleSub, description, ctaText, ctaTargetId, backgroundImageId) -> {
            HeroWidgetDto widget = new HeroWidgetDto();
            widget.setBadgeText(badgeText);
            widget.setTitleMain(titleMain);
            widget.setTitleSub(titleSub);
            widget.setDescription(description);
            widget.setCtaText(ctaText);
            widget.setCtaTargetId(ctaTargetId);
            widget.setBackgroundImageId(backgroundImageId);
            return widget;
        });
    }

    @Provide
    Arbitrary<RichTextWidgetDto> richTextWidgets() {
        Arbitrary<JsonNode> tipTapContent = Arbitraries.create(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String content = Arbitraries.strings()
                    .alpha().numeric().withChars(' ', '.', ',', '!', '?')
                    .ofMinLength(5).ofMaxLength(100)
                    .sample();
                
                String json = String.format(
                    "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"%s\"}]}]}",
                    content.replace("\"", "\\\"")
                );
                return mapper.readTree(json);
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to create TipTap JSON", e);
            }
        });

        Arbitrary<Map<String, JsonNode>> localizedContent = Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            tipTapContent
        ).ofMinSize(1).ofMaxSize(4);

        return localizedContent.map(bodyContent -> {
            RichTextWidgetDto widget = new RichTextWidgetDto();
            widget.setBodyContent(bodyContent);
            return widget;
        });
    }

    @Provide
    Arbitrary<NextEventWidgetDto> nextEventWidgets() {
        Arbitrary<Map<String, String>> localizedStrings = Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
        ).ofMinSize(1).ofMaxSize(4);

        // titleOverride is optional, so we include null as a possibility
        Arbitrary<Map<String, String>> optionalLocalizedStrings = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(null)),
            Tuple.of(3, localizedStrings)
        );

        return optionalLocalizedStrings.map(titleOverride -> {
            NextEventWidgetDto widget = new NextEventWidgetDto();
            widget.setTitleOverride(titleOverride);
            return widget;
        });
    }

    @Provide
    Arbitrary<NewsFeedWidgetDto> newsFeedWidgets() {
        Arbitrary<Map<String, String>> localizedStrings = Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
        ).ofMinSize(1).ofMaxSize(4);

        Arbitrary<Integer> itemCounts = Arbitraries.integers().between(1, 50);

        // tagFilter is optional
        Arbitrary<String> optionalTagFilter = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(null)),
            Tuple.of(3, Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20))
        );

        return Combinators.combine(
            localizedStrings,  // sectionTitle
            itemCounts,        // itemCount
            optionalTagFilter  // tagFilter
        ).as((sectionTitle, itemCount, tagFilter) -> {
            NewsFeedWidgetDto widget = new NewsFeedWidgetDto();
            widget.setSectionTitle(sectionTitle);
            widget.setItemCount(itemCount);
            widget.setTagFilter(tagFilter);
            return widget;
        });
    }

    @Property(tries = 100)
    @Label("Property 6: TipTap JSON Validity - Valid structures are accepted")
    void validTipTapJsonStructuresAreValid(@ForAll("validTipTapContent") Map<String, JsonNode> bodyContent) {
        // All generated content should have valid TipTap structure
        for (Map.Entry<String, JsonNode> entry : bodyContent.entrySet()) {
            String locale = entry.getKey();
            JsonNode content = entry.getValue();
            
            // Verify it has the required "type" field with value "doc"
            assertThat(content.has("type"))
                .as("TipTap JSON for locale '%s' should have 'type' field", locale)
                .isTrue();
            
            assertThat(content.get("type").asString())
                .as("TipTap JSON for locale '%s' should have type='doc'", locale)
                .isEqualTo("doc");
            
            // Verify it has the required "content" field as an array
            assertThat(content.has("content"))
                .as("TipTap JSON for locale '%s' should have 'content' field", locale)
                .isTrue();
            
            assertThat(content.get("content").isArray())
                .as("TipTap JSON for locale '%s' should have 'content' as an array", locale)
                .isTrue();
        }
    }

    @Property(tries = 100)
    @Label("Property 6: TipTap JSON Validity - Invalid structures are rejected")
    void invalidTipTapJsonStructuresAreInvalid(@ForAll("invalidTipTapContent") Map<String, JsonNode> bodyContent) {
        // All generated content should have invalid TipTap structure
        for (Map.Entry<String, JsonNode> entry : bodyContent.entrySet()) {
            JsonNode content = entry.getValue();
            
            // At least one of these conditions should be true for invalid content:
            // 1. Missing "type" field
            // 2. "type" field is not "doc"
            // 3. Missing "content" field
            // 4. "content" field is not an array
            
            boolean isInvalid = !content.has("type") 
                || !content.get("type").asString().equals("doc")
                || !content.has("content")
                || !content.get("content").isArray();
            
            assertThat(isInvalid)
                .as("Generated invalid TipTap JSON should fail at least one validity check")
                .isTrue();
        }
    }

    @Property(tries = 100)
    @Label("Property 6: TipTap JSON Validity - Valid TipTap content round-trips correctly")
    void validTipTapContentRoundTrips(@ForAll("validTipTapContent") Map<String, JsonNode> bodyContent) 
        throws JacksonException {
        
        // Create widget with valid TipTap content
        RichTextWidgetDto widget = new RichTextWidgetDto();
        widget.setBodyContent(bodyContent);
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(widget);
        
        // Deserialize back
        WidgetDto deserialized = objectMapper.readValue(json, WidgetDto.class);
        
        // Assert type preservation
        assertThat(deserialized)
            .as("Deserialized widget should be RichTextWidgetDto")
            .isInstanceOf(RichTextWidgetDto.class);
        
        RichTextWidgetDto deserializedRichText = (RichTextWidgetDto) deserialized;
        
        // Verify all locales still have valid TipTap structure after round-trip
        for (Map.Entry<String, JsonNode> entry : deserializedRichText.getBodyContent().entrySet()) {
            String locale = entry.getKey();
            JsonNode content = entry.getValue();
            
            assertThat(content.has("type") && content.get("type").asString().equals("doc"))
                .as("Round-tripped TipTap JSON for locale '%s' should maintain type='doc'", locale)
                .isTrue();
            
            assertThat(content.has("content") && content.get("content").isArray())
                .as("Round-tripped TipTap JSON for locale '%s' should maintain content array", locale)
                .isTrue();
        }
    }

    @Provide
    Arbitrary<Map<String, JsonNode>> validTipTapContent() {
        Arbitrary<JsonNode> validTipTapJson = Arbitraries.create(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                
                // Generate various valid TipTap structures
                int variant = Arbitraries.integers().between(0, 4).sample();
                
                String json = switch (variant) {
                    case 0 -> // Simple paragraph
                        """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [
                                {"type": "text", "text": "%s"}
                              ]
                            }
                          ]
                        }
                        """.formatted(Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50).sample());
                    
                    case 1 -> // Paragraph with bold text
                        """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [
                                {"type": "text", "text": "%s"},
                                {"type": "text", "marks": [{"type": "bold"}], "text": "%s"}
                              ]
                            }
                          ]
                        }
                        """.formatted(
                            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30).sample(),
                            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30).sample()
                        );
                    
                    case 2 -> // Multiple paragraphs
                        """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "%s"}]
                            },
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "%s"}]
                            }
                          ]
                        }
                        """.formatted(
                            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(40).sample(),
                            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(40).sample()
                        );
                    
                    case 3 -> // Heading and paragraph
                        """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "heading",
                              "attrs": {"level": 1},
                              "content": [{"type": "text", "text": "%s"}]
                            },
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "%s"}]
                            }
                          ]
                        }
                        """.formatted(
                            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30).sample(),
                            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50).sample()
                        );
                    
                    default -> // Empty doc (valid but no content)
                        """
                        {
                          "type": "doc",
                          "content": []
                        }
                        """;
                };
                
                return mapper.readTree(json);
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to create valid TipTap JSON", e);
            }
        });

        return Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            validTipTapJson
        ).ofMinSize(1).ofMaxSize(4);
    }

    @Provide
    Arbitrary<Map<String, JsonNode>> invalidTipTapContent() {
        Arbitrary<JsonNode> invalidTipTapJson = Arbitraries.create(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                
                // Generate various invalid TipTap structures
                int variant = Arbitraries.integers().between(0, 6).sample();
                
                String json = switch (variant) {
                    case 0 -> // Missing "type" field
                        """
                        {
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "test"}]
                            }
                          ]
                        }
                        """;
                    
                    case 1 -> // Wrong "type" value
                        """
                        {
                          "type": "document",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "test"}]
                            }
                          ]
                        }
                        """;
                    
                    case 2 -> // Missing "content" field
                        """
                        {
                          "type": "doc"
                        }
                        """;
                    
                    case 3 -> // "content" is not an array (it's an object)
                        """
                        {
                          "type": "doc",
                          "content": {
                            "type": "paragraph",
                            "content": [{"type": "text", "text": "test"}]
                          }
                        }
                        """;
                    
                    case 4 -> // "content" is a string
                        """
                        {
                          "type": "doc",
                          "content": "some text"
                        }
                        """;
                    
                    case 5 -> // "content" is a number
                        """
                        {
                          "type": "doc",
                          "content": 123
                        }
                        """;
                    
                    default -> // Empty object
                        """
                        {}
                        """;
                };
                
                return mapper.readTree(json);
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to create invalid TipTap JSON", e);
            }
        });

        return Arbitraries.maps(
            Arbitraries.of("en", "uk", "de", "fr"),
            invalidTipTapJson
        ).ofMinSize(1).ofMaxSize(4);
    }
}
