package org.nr31.backend.cucumber.stepdefs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CmsSteps extends CommonStepDefs {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Given("a page exists with slug {string} and title {string}")
    public void a_page_exists_with_slug_and_title(String slug, String title) {
        contextHelper.addValue("last_page_slug", slug);

        jdbcTemplate.update(
                "INSERT INTO pages (slug, title, version, created_at, updated_at) " +
                        "VALUES (?, ?::jsonb, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT (slug) DO NOTHING",
                slug, String.format("{\"en\": \"%s\"}", title)
        );

        Long pageId = jdbcTemplate.queryForObject(
                "SELECT id FROM pages WHERE slug = ?",
                Long.class,
                slug
        );
        contextHelper.addValue("page_id_" + slug, pageId);
    }

    @And("the page has a draft revision with layout data")
    public void the_page_has_a_draft_revision_with_layout_data() {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        String layoutData = createDefaultLayoutData();

        jdbcTemplate.update(
                "INSERT INTO page_revisions (page_id, layout_data, status, created_at) " +
                        "VALUES (?, ?::jsonb, 'DRAFT', CURRENT_TIMESTAMP)",
                pageId, layoutData
        );
    }

    @And("the page has a published revision with layout data")
    public void the_page_has_a_published_revision_with_layout_data() {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        String layoutData = createDefaultLayoutData();
        contextHelper.addValue("published_layout_" + slug, layoutData);

        jdbcTemplate.update(
                "INSERT INTO page_revisions (page_id, layout_data, status, created_at) " +
                        "VALUES (?, ?::jsonb, 'PUBLISHED', CURRENT_TIMESTAMP)",
                pageId, layoutData
        );
    }

    @And("the page has no draft revision")
    public void the_page_has_no_draft_revision() {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        jdbcTemplate.update(
                "DELETE FROM page_revisions WHERE page_id = ? AND status = 'DRAFT'",
                pageId
        );
    }

    @And("the page version has been incremented to {int}")
    public void the_page_version_has_been_incremented_to(Integer version) {
        String slug = getLastPageSlug();

        jdbcTemplate.update(
                "UPDATE pages SET version = ? WHERE slug = ?",
                version, slug
        );
    }

    @And("slot restrictions allow only {string} and {string} in {string} slots")
    public void slot_restrictions_allow_only_widgets_in_slots(String widget1, String widget2, String slotType) {
        String restrictions = String.format(
                "{\"%s\": [\"%s\", \"%s\"]}",
                slotType, widget1, widget2
        );

        jdbcTemplate.update(
                "UPDATE app_config SET config_value = ?::jsonb WHERE config_key = 'cms_slot_restrictions'",
                restrictions
        );
    }

    @When("I publish the draft for page {string} with version {int}")
    public void i_publish_the_draft_for_page_with_version(String slug, Integer version) throws Exception {
        String body = String.format("{\"version\": %d}", version);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/cms/pages/" + slug + "/publish", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a {string} widget in a {string} slot")
    public void i_update_the_draft_with_invalid_widget(String slug, Integer version, String widgetType, String slotType) throws Exception {
        String layoutData = createLayoutDataWithWidget(slotType, widgetType);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I retrieve the published page {string} as a public user")
    public void i_retrieve_the_published_page_as_a_public_user(String slug) throws Exception {
        // Clear JWT token to simulate public user
        contextHelper.addValue("jwt_token", null);

        HttpResponse<String> response = makeApiCall("GET", "/api/v1/cms/pages/" + slug, null);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I attempt to retrieve the draft for page {string} without authentication")
    public void i_attempt_to_retrieve_the_draft_without_authentication(String slug) throws Exception {
        // Clear JWT token
        contextHelper.addValue("jwt_token", null);

        HttpResponse<String> response = makeApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I attempt to update the draft for page {string} with version {int}")
    public void i_attempt_to_update_the_draft_for_page_with_version(String slug, Integer version) throws Exception {
        String layoutData = createDefaultLayoutData();
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I retrieve the draft for page {string}")
    public void i_retrieve_the_draft_for_page(String slug) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update slot restrictions with the following configuration:")
    public void i_update_slot_restrictions_with_configuration(DataTable dataTable) throws Exception {
        Map<String, String> restrictions = dataTable.asMap(String.class, String.class);

        ObjectNode restrictionsNode = objectMapper.createObjectNode();
        for (Map.Entry<String, String> entry : restrictions.entrySet()) {
            String[] widgets = entry.getValue().split(",");
            ArrayNode widgetArray = objectMapper.createArrayNode();
            for (String widget : widgets) {
                widgetArray.add(widget.trim());
            }
            restrictionsNode.set(entry.getKey(), widgetArray);
        }

        String body = String.format("{\"restrictions\": %s}", restrictionsNode.toString());
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/slot-restrictions", body);
        contextHelper.addValue("response", response);
    }

    @When("I update the draft for page {string} with version {int} and a text widget missing the {string} property")
    public void i_update_the_draft_with_missing_property(String slug, Integer version, String property) throws Exception {
        String layoutData = createLayoutDataWithMissingProperty();
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and new layout data")
    public void i_update_the_draft_with_new_layout_data(String slug, Integer version) throws Exception {
        String layoutData = createDefaultLayoutData();
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I retrieve the current slot restrictions")
    public void i_retrieve_the_current_slot_restrictions() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/cms/slot-restrictions", null);
        contextHelper.addValue("response", response);
    }

    @And("the response body should contain error message mentioning {string} and {string}")
    public void the_response_body_should_contain_error_message_mentioning(String term1, String term2) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String body = response.body();

        assertTrue(body.contains(term1) || body.toLowerCase().contains(term1.toLowerCase()),
                "Expected response to mention: " + term1 + "\nBody: " + body);
        assertTrue(body.contains(term2) || body.toLowerCase().contains(term2.toLowerCase()),
                "Expected response to mention: " + term2 + "\nBody: " + body);
    }

    @And("the response body should contain metadata field {string} with value {string}")
    public void the_response_body_should_contain_metadata_field_with_value(String field, String expectedValue) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode metadata = root.get("metadata");

        assertNotNull(metadata, "metadata field is missing");
        JsonNode fieldNode = metadata.get(field);
        assertNotNull(fieldNode, "metadata." + field + " is missing");
        assertEquals(expectedValue, fieldNode.asText());
    }

    @And("the response body should contain nested field {string} with value {string}")
    public void the_response_body_should_contain_nested_field_with_value(String jsonPath, String expectedValue) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        String[] pathParts = jsonPath.split("\\.");
        JsonNode current = root;
        for (String part : pathParts) {
            if (current != null && current.isArray()) {
                current = current.get(Integer.parseInt(part));
            } else {
                current = current.get(part);
            }
            assertNotNull(current, "Path " + jsonPath + " not found in " + response.body());
        }
        assertEquals(expectedValue, current.asText());
    }

    @And("the response body should contain slot restriction for {string} with widgets {string}")
    public void the_response_body_should_contain_slot_restriction_for_with_widgets(String slotType, String widgets) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode restrictions = root.get("restrictions");

        assertNotNull(restrictions, "restrictions field is missing");
        JsonNode slotRestrictions = restrictions.get(slotType);
        assertNotNull(slotRestrictions, "restrictions." + slotType + " is missing");

        String[] expectedWidgets = widgets.split(",");
        assertEquals(expectedWidgets.length, slotRestrictions.size());

        for (String widget : expectedWidgets) {
            boolean found = false;
            for (JsonNode node : slotRestrictions) {
                if (node.asText().equals(widget.trim())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected widget " + widget + " not found in slot " + slotType);
        }
    }

    @And("the draft layout data should match the published layout data")
    public void the_draft_layout_data_should_match_the_published_layout_data() throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode draftLayoutData = root.get("layoutData");

        String slug = getLastPageSlug();
        String publishedLayout = contextHelper.getValue("published_layout_" + slug);
        JsonNode publishedLayoutData = objectMapper.readTree(publishedLayout);

        assertEquals(publishedLayoutData, draftLayoutData,
                "Draft layout data should match published layout data");
    }

    @And("the response body should contain validation error for {string}")
    public void the_response_body_should_contain_validation_error_for(String field) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String body = response.body();

        assertTrue(body.contains(field) || body.toLowerCase().contains(field.toLowerCase()),
                "Expected validation error for field: " + field + "\nBody: " + body);
    }

    @And("the previous published revision should be archived")
    public void the_previous_published_revision_should_be_archived() {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        Integer archivedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM page_revisions WHERE page_id = ? AND status = 'ARCHIVED'",
                Integer.class,
                pageId
        );

        assertTrue(archivedCount > 0, "Expected at least one archived revision");
    }

    private String getLastPageSlug() {
        String slug = contextHelper.getValue("last_page_slug");
        if (slug == null) {
            throw new IllegalStateException("No page slug found in context");
        }
        return slug;
    }

    private String createDefaultLayoutData() {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "hero");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "text");

            ObjectNode content = objectMapper.createObjectNode();
            content.put("en", "<h1>Test Content</h1>");
            widget.set("content", content);

            widgets.add(widget);

            slot.set("widgets", widgets);
            slots.add(slot);

            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create layout data", e);
        }
    }

    private String createLayoutDataWithWidget(String slotType, String widgetType) {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", slotType);

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", widgetType);

            switch (widgetType) {
                case "text":
                    ObjectNode textContent = objectMapper.createObjectNode();
                    textContent.put("en", "<p>Test content</p>");
                    widget.set("content", textContent);
                    break;
                case "image":
                    widget.put("url", "https://example.com/image.jpg");
                    ObjectNode imageAlt = objectMapper.createObjectNode();
                    imageAlt.put("en", "Test image");
                    widget.set("alt", imageAlt);
                    break;
                case "video":
                    widget.put("url", "https://example.com/video.mp4");
                    break;
                case "embed":
                    ObjectNode embedCode = objectMapper.createObjectNode();
                    embedCode.put("en", "<iframe></iframe>");
                    widget.set("embedCode", embedCode);
                    break;
            }

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);

            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create layout data", e);
        }
    }

    private String createLayoutDataWithMissingProperty() {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "hero");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "text");
            // Intentionally missing "content" property

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);

            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create layout data", e);
        }
    }

    @When("I update the draft for page {string} with version {int} and a HeroWidget containing:")
    public void i_update_the_draft_with_hero_widget(String slug, Integer version, DataTable dataTable)
            throws Exception {
        String layoutData = createLayoutDataWithHeroWidget(dataTable);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a RichTextWidget containing:")
    public void i_update_the_draft_with_richtext_widget(String slug, Integer version, DataTable dataTable)
            throws Exception {
        String layoutData = createLayoutDataWithRichTextWidget(dataTable);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a NextEventWidget containing:")
    public void i_update_the_draft_with_nextevent_widget(String slug, Integer version, DataTable dataTable)
            throws Exception {
        String layoutData = createLayoutDataWithNextEventWidget(dataTable);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a NewsFeedWidget containing:")
    public void i_update_the_draft_with_newsfeed_widget(String slug, Integer version, DataTable dataTable)
            throws Exception {
        String layoutData = createLayoutDataWithNewsFeedWidget(dataTable);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a RichTextWidget with content exceeding {int} bytes")
    public void i_update_the_draft_with_oversized_richtext_widget(String slug, Integer version, Integer maxBytes)
            throws Exception {
        String layoutData = createLayoutDataWithOversizedRichTextWidget(maxBytes);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a NewsFeedWidget with itemCount {int}")
    public void i_update_the_draft_with_newsfeed_widget_itemcount(String slug, Integer version, Integer itemCount)
            throws Exception {
        String layoutData = createLayoutDataWithNewsFeedWidgetItemCount(itemCount);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a HeroWidget with non-existent backgroundImageId {string}")
    public void i_update_the_draft_with_hero_widget_invalid_image(String slug, Integer version, String imageId)
            throws Exception {
        String layoutData = createLayoutDataWithHeroWidgetInvalidImage(imageId);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);

        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @And("the AppConfig key {string} is set to {int}")
    public void the_appconfig_key_is_set_to(String key, Integer value) {
        jdbcTemplate.update(
                "INSERT INTO app_config (config_key, config_value, description) " +
                        "VALUES (?, ?, 'Test config') " +
                        "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value",
                key, value.toString());
    }

    @And("slot restrictions allow only {string} in {string} slots")
    public void slot_restrictions_allow_only_widget_in_slots(String widgetType, String slotType) {
        String restrictions = String.format("{\"%s\": [\"%s\"]}", slotType, widgetType);

        jdbcTemplate.update(
                "UPDATE app_config SET config_value = ?::jsonb WHERE config_key = 'cms_slot_restrictions'",
                restrictions);
    }

    @And("the draft should contain a widget of type {string}")
    public void the_draft_should_contain_a_widget_of_type(String widgetType) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode layoutData = root.get("layoutData");

        assertNotNull(layoutData, "layoutData field is missing");
        JsonNode slots = layoutData.get("slots");
        assertNotNull(slots, "slots field is missing");
        assertTrue(slots.isArray(), "slots is not an array");

        boolean found = false;
        for (JsonNode slot : slots) {
            JsonNode widgets = slot.get("widgets");
            if (widgets != null && widgets.isArray()) {
                for (JsonNode widget : widgets) {
                    JsonNode typeNode = widget.get("type");
                    if (typeNode != null && typeNode.asText().equals(widgetType)) {
                        found = true;
                        break;
                    }
                }
            }
            if (found)
                break;
        }

        assertTrue(found, "Expected to find widget of type: " + widgetType);
    }

    @And("the response body should contain validation error mentioning {string} or {string}")
    public void the_response_body_should_contain_validation_error_mentioning_either(String term1, String term2) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String body = response.body().toLowerCase();

        boolean containsTerm1 = body.contains(term1.toLowerCase());
        boolean containsTerm2 = body.contains(term2.toLowerCase());

        assertTrue(containsTerm1 || containsTerm2,
                "Expected response to mention either '" + term1 + "' or '" + term2 + "'\nBody: " + response.body());
    }

    @And("the page has a draft revision with a HeroWidget")
    public void the_page_has_a_draft_revision_with_a_hero_widget() {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        String layoutData = createSimpleHeroWidgetLayoutData();

        jdbcTemplate.update(
                "INSERT INTO page_revisions (page_id, layout_data, status, created_at) " +
                        "VALUES (?, ?::jsonb, 'DRAFT', CURRENT_TIMESTAMP)",
                pageId, layoutData);
    }

    @And("the page has a published revision with the following widgets:")
    public void the_page_has_a_published_revision_with_widgets(DataTable dataTable) {
        String slug = getLastPageSlug();
        Long pageId = contextHelper.getValue("page_id_" + slug);

        String layoutData = createLayoutDataWithMultipleWidgets(dataTable);

        jdbcTemplate.update(
                "INSERT INTO page_revisions (page_id, layout_data, status, created_at) " +
                        "VALUES (?, ?::jsonb, 'PUBLISHED', CURRENT_TIMESTAMP)",
                pageId, layoutData);
    }

    @And("the published page should contain a widget of type {string}")
    public void the_published_page_should_contain_a_widget_of_type(String widgetType) throws Exception {
        the_draft_should_contain_a_widget_of_type(widgetType);
    }

    // ========== Helper Methods for New Widgets ==========

    private String createLayoutDataWithHeroWidget(DataTable dataTable) {
        try {
            Map<String, String> data = dataTable.asMap(String.class, String.class);

            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "hero");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "hero");

            // Build localized fields
            ObjectNode badgeText = objectMapper.createObjectNode();
            ObjectNode description = objectMapper.createObjectNode();
            ObjectNode ctaText = objectMapper.createObjectNode();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("badgeText.")) {
                    badgeText.put(key.substring(10), value);
                } else if (key.startsWith("description.")) {
                    description.put(key.substring(12), value);
                } else if (key.startsWith("ctaText.")) {
                    ctaText.put(key.substring(8), value);
                } else if (key.equals("titleMain")) {
                    widget.put("titleMain", value);
                } else if (key.equals("titleSub")) {
                    widget.put("titleSub", value);
                } else if (key.equals("ctaTargetId")) {
                    widget.put("ctaTargetId", value);
                } else if (key.equals("backgroundImageId")) {
                    widget.put("backgroundImageId", value);
                }
            }

            widget.set("badgeText", badgeText);
            widget.set("description", description);
            widget.set("ctaText", ctaText);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HeroWidget layout data", e);
        }
    }

    private String createLayoutDataWithRichTextWidget(DataTable dataTable) {
        try {
            Map<String, String> data = dataTable.asMap(String.class, String.class);

            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "content");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "richtext");

            ObjectNode bodyContent = objectMapper.createObjectNode();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getKey().startsWith("bodyContent.")) {
                    String locale = entry.getKey().substring(12);
                    JsonNode contentNode = objectMapper.readTree(entry.getValue());
                    bodyContent.set(locale, contentNode);
                }
            }

            widget.set("bodyContent", bodyContent);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RichTextWidget layout data", e);
        }
    }

    private String createLayoutDataWithNextEventWidget(DataTable dataTable) {
        try {
            Map<String, String> data = dataTable.asMap(String.class, String.class);

            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "sidebar");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "nextevent");

            ObjectNode titleOverride = objectMapper.createObjectNode();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getKey().startsWith("titleOverride.")) {
                    String locale = entry.getKey().substring(14);
                    titleOverride.put(locale, entry.getValue());
                }
            }

            if (!titleOverride.isEmpty()) {
                widget.set("titleOverride", titleOverride);
            }

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create NextEventWidget layout data", e);
        }
    }

    private String createLayoutDataWithNewsFeedWidget(DataTable dataTable) {
        try {
            Map<String, String> data = dataTable.asMap(String.class, String.class);

            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "content");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "newsfeed");

            ObjectNode sectionTitle = objectMapper.createObjectNode();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("sectionTitle.")) {
                    String locale = key.substring(13);
                    sectionTitle.put(locale, value);
                } else if (key.equals("itemCount")) {
                    widget.put("itemCount", Integer.parseInt(value));
                } else if (key.equals("tagFilter")) {
                    widget.put("tagFilter", value);
                }
            }

            widget.set("sectionTitle", sectionTitle);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create NewsFeedWidget layout data", e);
        }
    }

    private String createLayoutDataWithOversizedRichTextWidget(Integer maxBytes) {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "content");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "richtext");

            // Create oversized content
            StringBuilder largeContent = new StringBuilder();
            largeContent.append(
                    "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"");

            // Add enough text to exceed the limit
            int targetSize = maxBytes + 1000;
            while (largeContent.length() < targetSize) {
                largeContent.append("This is a very long text that will exceed the size limit. ");
            }

            largeContent.append("\"}]}]}");

            ObjectNode bodyContent = objectMapper.createObjectNode();
            bodyContent.set("en", objectMapper.readTree(largeContent.toString()));

            widget.set("bodyContent", bodyContent);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create oversized RichTextWidget layout data", e);
        }
    }

    private String createLayoutDataWithNewsFeedWidgetItemCount(Integer itemCount) {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "content");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "newsfeed");

            ObjectNode sectionTitle = objectMapper.createObjectNode();
            sectionTitle.put("en", "Latest News");
            widget.set("sectionTitle", sectionTitle);
            widget.put("itemCount", itemCount);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create NewsFeedWidget layout data", e);
        }
    }

    private String createLayoutDataWithHeroWidgetInvalidImage(String imageId) {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "hero");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "hero");

            ObjectNode badgeText = objectMapper.createObjectNode();
            badgeText.put("en", "Test Badge");
            widget.set("badgeText", badgeText);

            widget.put("titleMain", "Nr.31");
            widget.put("titleSub", "Feldkanonenregiment");

            ObjectNode description = objectMapper.createObjectNode();
            description.put("en", "Test description");
            widget.set("description", description);

            ObjectNode ctaText = objectMapper.createObjectNode();
            ctaText.put("en", "Join Now");
            widget.set("ctaText", ctaText);

            widget.put("ctaTargetId", "test-target");
            widget.put("backgroundImageId", imageId);

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HeroWidget layout data with invalid image", e);
        }
    }

    private String createSimpleHeroWidgetLayoutData() {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            ObjectNode slot = objectMapper.createObjectNode();
            slot.put("slotType", "hero");

            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "hero");

            ObjectNode badgeText = objectMapper.createObjectNode();
            badgeText.put("en", "M&B Bannerlord Regiment");
            widget.set("badgeText", badgeText);

            widget.put("titleMain", "Nr.31");
            widget.put("titleSub", "Feldkanonenregiment");

            ObjectNode description = objectMapper.createObjectNode();
            description.put("en", "Join the elite artillery regiment");
            widget.set("description", description);

            ObjectNode ctaText = objectMapper.createObjectNode();
            ctaText.put("en", "Join Now");
            widget.set("ctaText", ctaText);

            widget.put("ctaTargetId", "how-to-join");
            widget.put("backgroundImageId", "550e8400-e29b-41d4-a716-446655440000");

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create simple HeroWidget layout data", e);
        }
    }

    private String createLayoutDataWithMultipleWidgets(DataTable dataTable) {
        try {
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();

            Map<String, ArrayNode> slotMap = new java.util.HashMap<>();

            for (Map<String, String> row : dataTable.asMaps(String.class, String.class)) {
                String widgetType = row.get("type");
                String slotType = row.get("slot");

                if (!slotMap.containsKey(slotType)) {
                    slotMap.put(slotType, objectMapper.createArrayNode());
                }

                ObjectNode widget = objectMapper.createObjectNode();
                widget.put("type", widgetType);

                // Add minimal required fields for each widget type
                switch (widgetType) {
                    case "hero":
                        ObjectNode badgeText = objectMapper.createObjectNode();
                        badgeText.put("en", "Test Badge");
                        widget.set("badgeText", badgeText);
                        widget.put("titleMain", "Nr.31");
                        widget.put("titleSub", "Feldkanonenregiment");
                        ObjectNode heroDesc = objectMapper.createObjectNode();
                        heroDesc.put("en", "Test description");
                        widget.set("description", heroDesc);
                        ObjectNode ctaText = objectMapper.createObjectNode();
                        ctaText.put("en", "Join Now");
                        widget.set("ctaText", ctaText);
                        widget.put("ctaTargetId", "test-target");
                        widget.put("backgroundImageId", "550e8400-e29b-41d4-a716-446655440000");
                        break;
                    case "richtext":
                        ObjectNode bodyContent = objectMapper.createObjectNode();
                        bodyContent.set("en", objectMapper.readTree(
                                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Test content\"}]}]}"));
                        widget.set("bodyContent", bodyContent);
                        break;
                    case "nextevent":
                        // NextEventWidget has no required fields
                        break;
                    case "newsfeed":
                        ObjectNode sectionTitle = objectMapper.createObjectNode();
                        sectionTitle.put("en", "Latest News");
                        widget.set("sectionTitle", sectionTitle);
                        widget.put("itemCount", 3);
                        break;
                }

                slotMap.get(slotType).add(widget);
            }

            // Build slots array
            for (Map.Entry<String, ArrayNode> entry : slotMap.entrySet()) {
                ObjectNode slot = objectMapper.createObjectNode();
                slot.put("slotType", entry.getKey());
                slot.set("widgets", entry.getValue());
                slots.add(slot);
            }

            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create layout data with multiple widgets", e);
        }
    }

}
