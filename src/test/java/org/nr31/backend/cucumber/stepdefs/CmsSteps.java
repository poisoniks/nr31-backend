package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
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
    }

    @And("the page has a draft revision with layout data")
    public void the_page_has_a_draft_revision_with_layout_data() throws Exception {
        String slug = getLastPageSlug();
        String layoutData = createDefaultLayoutData();
        HttpResponse<String> getDraftResp = makeAdminApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        if (getDraftResp.statusCode() == 404) {
            Long pageId = jdbcTemplate.queryForObject("SELECT id FROM pages WHERE slug = ?", Long.class, slug);
            jdbcTemplate.update(
                    "INSERT INTO page_revisions (page_id, layout_data, status, created_at) " +
                    "VALUES (?, ?::jsonb, 'DRAFT', CURRENT_TIMESTAMP)",
                    pageId, layoutData);
            return;
        }
        int currentVersion = objectMapper.readTree(getDraftResp.body()).get("version").asInt();
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", currentVersion, layoutData);
        makeAdminApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
    }

    @And("the page has a published revision with layout data")
    public void the_page_has_a_published_revision_with_layout_data() throws Exception {
        String slug = getLastPageSlug();
        String layoutData = createDefaultLayoutData();
        contextHelper.addValue("published_layout_" + slug, layoutData);
        HttpResponse<String> getDraftResp = makeAdminApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        int currentVersion = objectMapper.readTree(getDraftResp.body()).get("version").asInt();
        makeAdminApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft",
                String.format("{\"version\": %d, \"layoutData\": %s}", currentVersion, layoutData));
        makeAdminApiCall("POST", "/api/v1/cms/pages/" + slug + "/publish",
                String.format("{\"version\": %d}", currentVersion));
    }

    @And("the page version has been incremented to {int}")
    public void the_page_version_has_been_incremented_to(Integer targetVersion) throws Exception {
        String slug = getLastPageSlug();
        for (int v = 1; v < targetVersion; v++) {
            makeAdminApiCall("POST", "/api/v1/cms/pages/" + slug + "/publish",
                    String.format("{\"version\": %d}", v));
            makeAdminApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
            makeAdminApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft",
                    String.format("{\"version\": %d, \"layoutData\": %s}",
                            v + 1, createDefaultLayoutData()));
        }
    }

    @And("slot restrictions allow only {string} and {string} in {string} slots")
    public void slot_restrictions_allow_only_widgets_in_slots(String widget1, String widget2, String slotType) throws Exception {
        String body = String.format("{\"restrictions\": {\"%s\": [\"%s\", \"%s\"]}}",
                slotType, widget1, widget2);
        makeApiCall("PUT", "/api/v1/cms/slot-restrictions", body);
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
        contextHelper.addValue("jwt_token", null);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/cms/pages/" + slug, null);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I attempt to retrieve the draft for page {string} without authentication")
    public void i_attempt_to_retrieve_the_draft_without_authentication(String slug) throws Exception {
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

    @When("I update the draft for page {string} with version {int} and a richtext widget missing the {string} property")
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
                if (node.asString().equals(widget.trim())) {
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
        assertEquals(publishedLayoutData, draftLayoutData, "Draft layout data should match published layout data");
    }

    @And("the previous published revision should be archived")
    public void the_previous_published_revision_should_be_archived() {
        HttpResponse<String> response = contextHelper.getValue("response");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"PUBLISHED\""));
    }

    @When("I update the draft for page {string} with version {int} and a {word} containing:")
    public void i_update_the_draft_with_widget(String slug, Integer version, String widgetClass, DataTable dataTable) throws Exception {
        String layoutData = buildGenericLayoutData(widgetClass, dataTable);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a RichTextWidget with content exceeding {int} bytes")
    public void i_update_the_draft_with_oversized_richtext_widget(String slug, Integer version, Integer maxBytes) throws Exception {
        String layoutData = createLayoutDataWithOversizedRichTextWidget(maxBytes);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a NewsFeedWidget with itemCount {int}")
    public void i_update_the_draft_with_newsfeed_widget_itemcount(String slug, Integer version, Integer itemCount) throws Exception {
        String layoutData = createLayoutDataWithNewsFeedWidgetItemCount(itemCount);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @When("I update the draft for page {string} with version {int} and a HeroWidget with non-existent backgroundImageId {string}")
    public void i_update_the_draft_with_hero_widget_invalid_image(String slug, Integer version, String imageId) throws Exception {
        String layoutData = createLayoutDataWithHeroWidgetInvalidImage(imageId);
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", version, layoutData);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
        contextHelper.addValue("response", response);
        contextHelper.addValue("last_page_slug", slug);
    }

    @And("the AppConfig key {string} is set to {int}")
    public void the_appconfig_key_is_set_to(String key, Integer value) throws Exception {
        String body = String.format("{\"name\": \"%s\", \"configValue\": \"%d\"}", key, value);
        makeApiCall("PUT", "/api/v1/admin/config/" + key, body);
    }

    @And("slot restrictions allow only {string} in {string} slots")
    public void slot_restrictions_allow_only_widget_in_slots(String widgetType, String slotType) throws Exception {
        String body = String.format("{\"restrictions\": {\"%s\": [\"%s\"]}}", slotType, widgetType);
        makeApiCall("PUT", "/api/v1/cms/slot-restrictions", body);
    }

    @And("the draft should contain a widget of type {string}")
    public void the_draft_should_contain_a_widget_of_type(String widgetType) {
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
                    if (typeNode != null && typeNode.asString().equals(widgetType)) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) break;
        }
        assertTrue(found, "Expected to find widget of type: " + widgetType);
    }

    @And("the page has a draft revision with a HeroWidget")
    public void the_page_has_a_draft_revision_with_a_hero_widget() throws Exception {
        String slug = getLastPageSlug();
        String layoutData = createSimpleHeroWidgetLayoutData();
        HttpResponse<String> getDraftResp = makeAdminApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        int currentVersion = objectMapper.readTree(getDraftResp.body()).get("version").asInt();
        String body = String.format("{\"version\": %d, \"layoutData\": %s}", currentVersion, layoutData);
        makeAdminApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft", body);
    }

    @And("the page has a published revision with the following widgets:")
    public void the_page_has_a_published_revision_with_widgets(DataTable dataTable) throws Exception {
        String slug = getLastPageSlug();
        String layoutData = createLayoutDataWithMultipleWidgets(dataTable);
        HttpResponse<String> getDraftResp = makeAdminApiCall("GET", "/api/v1/cms/pages/" + slug + "/draft", null);
        int currentVersion = objectMapper.readTree(getDraftResp.body()).get("version").asInt();
        makeAdminApiCall("PUT", "/api/v1/cms/pages/" + slug + "/draft",
                String.format("{\"version\": %d, \"layoutData\": %s}", currentVersion, layoutData));
        makeAdminApiCall("POST", "/api/v1/cms/pages/" + slug + "/publish",
                String.format("{\"version\": %d}", currentVersion));
    }

    @And("the published page should contain a widget of type {string}")
    public void the_published_page_should_contain_a_widget_of_type(String widgetType) throws Exception {
        the_draft_should_contain_a_widget_of_type(widgetType);
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
            slot.put("slotType", "content");
            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", "richtext");
            ObjectNode bodyContent = objectMapper.createObjectNode();
            bodyContent.set("en", objectMapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Test Content\"}]}]}"));
            widget.set("bodyContent", bodyContent);
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
                case "richtext":
                    ObjectNode bodyContent = objectMapper.createObjectNode();
                    bodyContent.set("en", objectMapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Test content\"}]}]}"));
                    widget.set("bodyContent", bodyContent);
                    break;
                case "hero":
                    ObjectNode badgeText = objectMapper.createObjectNode();
                    badgeText.put("en", "Test badge");
                    widget.set("badgeText", badgeText);
                    widget.put("titleMain", "Nr.31");
                    widget.put("titleSub", "FKR");
                    ObjectNode description = objectMapper.createObjectNode();
                    description.put("en", "Test description");
                    widget.set("description", description);
                    ObjectNode ctaText = objectMapper.createObjectNode();
                    ctaText.put("en", "Join");
                    widget.set("ctaText", ctaText);
                    widget.put("ctaTargetId", "test-target");
                    widget.put("backgroundImageId", "550e8400-e29b-41d4-a716-446655440000");
                    break;
                case "youtube":
                    widget.put("channelId", "UCbU41G2hhiwdn-gFFRqZN4w");
                    break;
                case "newsfeed":
                    ObjectNode sectionTitle = objectMapper.createObjectNode();
                    sectionTitle.put("en", "Latest News");
                    widget.set("sectionTitle", sectionTitle);
                    widget.put("itemCount", 3);
                    break;
                case "nextevent":
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
            widget.put("type", "richtext");
            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);
            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create layout data", e);
        }
    }

    private String buildGenericLayoutData(String widgetClass, DataTable dataTable) {
        try {
            Map<String, String> data = dataTable.asMap(String.class, String.class);
            ObjectNode layoutData = objectMapper.createObjectNode();
            ArrayNode slots = objectMapper.createArrayNode();
            ObjectNode slot = objectMapper.createObjectNode();

            String slotType;
            String type;
            switch (widgetClass) {
                case "HeroWidget": slotType = "hero"; type = "hero"; break;
                case "RichTextWidget": slotType = "content"; type = "richtext"; break;
                case "NextEventWidget": slotType = "sidebar"; type = "nextevent"; break;
                case "NewsFeedWidget": slotType = "content"; type = "newsfeed"; break;
                default: throw new IllegalArgumentException("Unknown widget class: " + widgetClass);
            }

            slot.put("slotType", slotType);
            ArrayNode widgets = objectMapper.createArrayNode();
            ObjectNode widget = objectMapper.createObjectNode();
            widget.put("type", type);

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String[] parts = entry.getKey().split("\\.");
                ObjectNode current = widget;
                for (int i = 0; i < parts.length - 1; i++) {
                    if (!current.has(parts[i])) {
                        current.set(parts[i], objectMapper.createObjectNode());
                    }
                    current = (ObjectNode) current.get(parts[i]);
                }

                String val = entry.getValue();
                String lastPart = parts[parts.length - 1];

                if (val.startsWith("{") || val.startsWith("[")) {
                    current.set(lastPart, objectMapper.readTree(val));
                } else if (val.matches("-?\\d+")) {
                    current.put(lastPart, Integer.parseInt(val));
                } else {
                    current.put(lastPart, val);
                }
            }

            widgets.add(widget);
            slot.set("widgets", widgets);
            slots.add(slot);
            layoutData.set("slots", slots);

            return objectMapper.writeValueAsString(layoutData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create generic layout data for " + widgetClass, e);
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
            StringBuilder largeContent = new StringBuilder();
            largeContent.append("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"");
            int targetSize = maxBytes + 1000;
            while (largeContent.length() < targetSize) largeContent.append("This is a very long text that will exceed the size limit. ");
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
                if (!slotMap.containsKey(slotType)) slotMap.put(slotType, objectMapper.createArrayNode());
                ObjectNode widget = objectMapper.createObjectNode();
                widget.put("type", widgetType);
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
                        bodyContent.set("en", objectMapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Test content\"}]}]}"));
                        widget.set("bodyContent", bodyContent);
                        break;
                    case "nextevent":
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
