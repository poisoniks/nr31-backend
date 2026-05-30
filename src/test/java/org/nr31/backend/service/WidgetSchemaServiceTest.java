package org.nr31.backend.service;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.SupportedLocale;
import org.nr31.backend.repository.SupportedLocaleRepository;
import org.nr31.backend.service.impl.WidgetSchemaServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WidgetSchemaServiceTest {

    private WidgetSchemaService widgetSchemaService;

    @BeforeEach
    void setUp() {
        SupportedLocaleRepository supportedLocaleRepository = mock(SupportedLocaleRepository.class);

        when(supportedLocaleRepository.findAll()).thenReturn(List.of(
                SupportedLocale.builder().id(1L).code("en").description("English").build(),
                SupportedLocale.builder().id(2L).code("uk").description("Ukrainian").build()
        ));
        
        widgetSchemaService = new WidgetSchemaServiceImpl(supportedLocaleRepository);
    }

    @Test
    void shouldReturnSchemaForEveryKnownType() {
        Set<String> types = widgetSchemaService.getWidgetTypes();
        assertFalse(types.isEmpty());
        assertTrue(types.contains("hero"));
        assertTrue(types.contains("richtext"));

        for (String type : types) {
            JsonNode schema = widgetSchemaService.getSchema(type);
            assertNotNull(schema, "Schema for " + type + " should not be null");
            System.out.println("Schema for " + type + ": " + schema.toPrettyString());
            assertEquals("object", schema.get("type").asString());
            assertTrue(schema.has("properties"), "Schema for " + type + " should have properties");
        }
    }

    @Test
    void shouldThrowForUnknownType() {
        assertThrows(ElementNotFoundException.class, () -> widgetSchemaService.getSchema("nonexistent"));
    }

    @Test
    void shouldIncludeRequiredFieldsForHero() {
        JsonNode schema = widgetSchemaService.getSchema("hero");
        JsonNode required = schema.get("required");
        assertNotNull(required);
        assertTrue(required.isArray());
        
        Set<String> requiredFields = new java.util.HashSet<>();
        required.forEach(node -> requiredFields.add(node.asString()));
        
        assertTrue(requiredFields.contains("badgeText"));
        assertTrue(requiredFields.contains("titleMain"));
        assertTrue(requiredFields.contains("titleSub"));
        assertTrue(requiredFields.contains("description"));
        assertTrue(requiredFields.contains("ctaText"));
        assertTrue(requiredFields.contains("ctaTargetId"));
        assertTrue(requiredFields.contains("backgroundImageId"));
    }

    @Test
    void shouldIncludePatternForYoutubeChannelId() {
        JsonNode schema = widgetSchemaService.getSchema("youtube");
        JsonNode channelId = schema.get("properties").get("channelId");
        assertNotNull(channelId);
        assertEquals("^UC[\\w-]{22}$", channelId.get("pattern").asString());
    }

    @Test
    void shouldIncludeMinForNewsFeedItemCount() {
        JsonNode schema = widgetSchemaService.getSchema("newsfeed");
        JsonNode itemCount = schema.get("properties").get("itemCount");
        assertNotNull(itemCount);
        assertEquals(1, itemCount.get("minimum").asInt());
    }

    @Test
    void shouldCacheSchemaAcrossCalls() {
        JsonNode schema1 = widgetSchemaService.getSchema("hero");
        JsonNode schema2 = widgetSchemaService.getSchema("hero");
        assertSame(schema1, schema2);
    }

    @Test
    void shouldReturnAllSchemas() {
        Map<String, JsonNode> allSchemas = widgetSchemaService.getAllSchemas();
        assertEquals(widgetSchemaService.getWidgetTypes().size(), allSchemas.size());
        assertTrue(allSchemas.containsKey("hero"));
        assertTrue(allSchemas.containsKey("richtext"));
    }

    @Test
    void shouldAddLocalizedMetadataToLocalizedFields() {
        JsonNode schema = widgetSchemaService.getSchema("hero");
        JsonNode properties = schema.get("properties");

        // Check badgeText has localization metadata
        JsonNode badgeText = properties.get("badgeText");
        assertNotNull(badgeText);
        assertTrue(badgeText.has("x-localized"), "badgeText should have x-localized");
        assertTrue(badgeText.get("x-localized").asBoolean(), "x-localized should be true");
        assertTrue(badgeText.has("x-widget"), "badgeText should have x-widget");
        assertEquals("localized-text-input", badgeText.get("x-widget").asString());
        assertTrue(badgeText.has("x-supported-locales"), "badgeText should have x-supported-locales");
        
        JsonNode supportedLocales = badgeText.get("x-supported-locales");
        assertTrue(supportedLocales.isArray());
        assertEquals(2, supportedLocales.size());
        assertEquals("en", supportedLocales.get(0).asString());
        assertEquals("uk", supportedLocales.get(1).asString());
        
        // Check description has localization metadata
        JsonNode description = properties.get("description");
        assertNotNull(description);
        assertTrue(description.has("x-localized"));
        assertTrue(description.get("x-localized").asBoolean());
        
        // Check ctaText has localization metadata
        JsonNode ctaText = properties.get("ctaText");
        assertNotNull(ctaText);
        assertTrue(ctaText.has("x-localized"));
        assertTrue(ctaText.get("x-localized").asBoolean());
        
        // Check non-localized fields don't have metadata
        JsonNode titleMain = properties.get("titleMain");
        assertNotNull(titleMain);
        assertFalse(titleMain.has("x-localized"), "titleMain should not have x-localized");
    }
}
