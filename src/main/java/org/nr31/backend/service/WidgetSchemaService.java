package org.nr31.backend.service;

import tools.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Set;

/**
 * Service for generating and retrieving JSON schemas for CMS widgets.
 */
public interface WidgetSchemaService {

    /**
     * Retrieves the JSON schema for a specific widget type.
     *
     * @param type the widget type (e.g., "hero", "richtext")
     * @return the JSON schema as a JsonNode
     * @throws org.nr31.backend.exception.ElementNotFoundException if the type is unknown
     */
    JsonNode getSchema(String type);

    /**
     * Retrieves JSON schemas for all known widget types.
     *
     * @return a map of widget type to its JSON schema
     */
    Map<String, JsonNode> getAllSchemas();

    /**
     * Retrieves the set of all known widget types.
     *
     * @return a set of widget type names
     */
    Set<String> getWidgetTypes();
}
