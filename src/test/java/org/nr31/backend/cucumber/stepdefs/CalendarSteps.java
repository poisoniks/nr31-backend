package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import org.nr31.backend.cucumber.ScenarioContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalendarSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Before
    public void setup() {
        jdbcTemplate.execute("TRUNCATE TABLE events CASCADE");
        contextHelper.initialize();
    }

    @After
    public void tearDown() {
        contextHelper.release();
    }

    @Given("I log in with user {string} and password {string}")
    public void i_log_in_with_user_and_password(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Login failed: " + response.body());

        JsonNode root = objectMapper.readTree(response.body());
        String token = root.get("accessToken").asText();
        contextHelper.addValue("jwt_token", token);
    }

    @Given("I log out")
    public void i_log_out() {
        contextHelper.addValue("jwt_token", null);
    }

    @When("I create an event with the following details:")
    public void i_create_an_event_with_the_following_details(DataTable dataTable) throws Exception {
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/events", body);
        contextHelper.addValue("response", response);
    }

    @When("I update the event {string} with the following details:")
    public void i_update_the_event_with_the_following_details(String eventIdRef, DataTable dataTable) throws Exception {
        String eventId = resolveVariables(eventIdRef);
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/calendar/events/" + eventId, body);
        contextHelper.addValue("response", response);
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(Integer expectedStatusCode) {
        HttpResponse<String> response = contextHelper.getValue("response");
        assertEquals(expectedStatusCode, response.statusCode(), "Body was: " + response.body());
    }

    @Then("the response body should contain {string} with value {string}")
    public void the_response_body_should_contain_with_value(String jsonPath, String expectedValue) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        String[] pathParts = jsonPath.split("\\.");
        JsonNode current = root;
        for (String part : pathParts) {
            current = current.get(part);
            assertNotNull(current, "Path " + jsonPath + " not found in " + response.body());
        }
        assertEquals(expectedValue, current.asText());
    }

    @Then("the response body should indicate {string} is true")
    public void the_response_body_should_indicate_is_true(String fieldName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has(fieldName));
        assertTrue(root.get(fieldName).asBoolean());
    }

    @When("I retrieve events with the following parameters:")
    public void i_retrieve_events_with_the_following_parameters(DataTable dataTable) throws Exception {
        Map<String, String> params = dataTable.asMap(String.class, String.class);
        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpResponse<String> response = makeApiCall("GET", "/api/v1/calendar/events?" + queryString, null);
        contextHelper.addValue("response", response);
    }

    @When("I delete the event {string} with parameters:")
    public void i_delete_the_event_with_parameters(String eventIdRef, DataTable dataTable) throws Exception {
        String eventId = resolveVariables(eventIdRef);
        Map<String, String> params = dataTable.asMap(String.class, String.class);
        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/calendar/events/" + eventId + "?" + queryString,
                null);
        contextHelper.addValue("response", response);
    }

    @Then("the response body should be a list of calendar events")
    public void the_response_body_should_be_a_list_of_calendar_events() throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
    }

    @Then("each event in the list should have a valid {string} and {string} date")
    public void each_event_in_the_list_should_have_a_valid_and_date(String field1, String field2) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        for (JsonNode node : root) {
            assertTrue(node.has(field1));
            assertTrue(node.has(field2));
        }
    }

    @Then("I should not be able to retrieve event {string}")
    public void i_should_not_be_able_to_retrieve_event(String idStr) throws Exception {
        String expectedId = resolveVariables(idStr);

        // Since there is no GET /{id} endpoint, verify by querying the range
        HttpResponse<String> response = makeApiCall("GET",
                "/api/v1/calendar/events?from=2025-01-01&to=2027-12-31&timezone=UTC", null);

        assertEquals(200, response.statusCode(), "Expected 200 OK when retrieving events to verify deletion");
        assertFalse(response.body().contains("\"id\":\"" + expectedId + "\""),
                "Expected event " + expectedId + " to be deleted, but it was found in the response.");
    }

    @Then("all events in series {string} should be deleted")
    public void all_events_in_series_should_be_deleted(String idStr) throws Exception {
        i_should_not_be_able_to_retrieve_event(idStr);
    }

    @Then("events in series {string} after {string} should not exist")
    public void events_in_series_after_should_not_exist(String seriesIdRef, String afterDate) throws Exception {
        String seriesId = resolveVariables(seriesIdRef);
        HttpResponse<String> response = makeApiCall("GET",
                "/api/v1/calendar/events?from=" + afterDate + "&to=2027-12-31&timezone=UTC", null);
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        for (JsonNode node : root) {
            String actId = node.get("id").asText();
            String actSeriesId = actId.split("_")[0];
            assertNotEquals(seriesId, actSeriesId, "Found event that should have been deleted: " + actId);
        }
    }

    @Then("the event start time should be adjusted to local time in the response")
    public void the_event_start_time_should_be_adjusted_to_local_time_in_the_response() throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        assertFalse(root.isEmpty(), "No events returned");

        boolean timeAdjusted = false;
        for (JsonNode node : root) {
            if (node.get("title").get("en").asText().equals("Timezone check event")) {
                String localStart = node.get("start").asText();
                // UTC: 10:00 -> Kyiv: 12:00
                assertTrue(localStart.contains("12:00"), "Expected adjusted time to be 12:00 but was " + localStart);
                timeAdjusted = true;
            }
        }
        assertTrue(timeAdjusted, "Could not find the timezone check event to verify.");
    }

    @Then("the list should contain an event with title {string}")
    public void the_list_should_contain_an_event_with_title(String expectedTitle) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());

        boolean found = false;
        for (JsonNode node : root) {
            if (expectedTitle.equals(node.get("title").get("en").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find event with title: " + expectedTitle);
    }

    @Then("the response body should match the new start time {string}")
    public void the_response_body_should_match_the_new_start_time(String expectedTime) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertEquals(expectedTime, root.get("start").asText());
    }

    @When("I retrieve the nearest event to {string}")
    public void i_retrieve_the_nearest_event(String date) throws Exception {
        HttpResponse<String> res = makeApiCall("GET", "/api/v1/calendar/events/nearest?date=" + date, "");
        contextHelper.addValue("response", res);
    }

    private String dataTableToJson(DataTable dataTable) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        Map<String, String> map = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            ObjectNode current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    current.set(parts[i], objectMapper.createObjectNode());
                }
                current = (ObjectNode) current.get(parts[i]);
            }
            String key = parts[parts.length - 1];
            String value = entry.getValue();

            if (value == null || value.equals("null")) {
                current.set(key, objectMapper.nullNode());
            } else if ((value.startsWith("[") && value.endsWith("]"))
                    || (value.startsWith("{") && value.endsWith("}"))) {
                current.set(key, objectMapper.readTree(value));
            } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                current.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("-?\\d+")) {
                current.put(key, Integer.parseInt(value));
            } else {
                current.put(key, value);
            }
        }
        return root.toString();
    }

    private String resolveVariables(String text) {
        if (text == null)
            return null;
        String resolved = text;
        Matcher m = Pattern.compile("\\{([^}]+)}").matcher(text);
        while (m.find()) {
            String key = m.group(1);
            Object value = contextHelper.getValue(key);
            if (value != null) {
                resolved = resolved.replace("{" + key + "}", value.toString());
            }
        }
        return resolved;
    }

    private HttpResponse<String> makeApiCall(String method, String url, String body) throws Exception {
        String resolvedUrl = resolveVariables(url);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + resolvedUrl))
                .header("Content-Type", "application/json");

        String token = contextHelper.getValue("jwt_token");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        if (body != null && !body.isEmpty()) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String fieldName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has(fieldName), "Response does not contain " + fieldName + "\nBody: " + response.body());
    }

    @Then("subsequent GET requests for any date in the series should show title {string}")
    public void subsequent_get_requests_for_any_date_in_the_series_should_show_title(String expectedTitle)
            throws Exception {
        HttpResponse<String> response = makeApiCall("GET",
                "/api/v1/calendar/events?from=2026-10-27&to=2026-10-31&timezone=UTC", null);
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        for (JsonNode node : root) {
            assertEquals(expectedTitle, node.get("title").get("en").asText());
        }
    }

    @And("I save the created event {string} as {string}")
    public void iSaveTheCreatedEventAs(String field, String varName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        contextHelper.addValue(varName, root.get(field).asText());
    }

    @And("the response list should contain exactly the following state for the series:")
    public void theResponseListShouldContainExactlyTheFollowingStateForTheSeries(
            io.cucumber.datatable.DataTable dataTable) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());

        List<Map<String, String>> expectedPresentList = dataTable.asMaps().stream()
                .filter(row -> "Present".equals(row.get("Status")))
                .toList();

        assertEquals(expectedPresentList.size(), root.size(),
                "The number of events in response does not match the 'Present' expected events.");

        for (Map<String, String> expectedRow : expectedPresentList) {
            boolean found = false;
            for (JsonNode node : root) {
                String title = node.get("title").get("en").asText();
                String start = node.get("start").asText();
                String server = node.get("serverName").asText();

                if (title.equals(expectedRow.get("Expected Title")) &&
                        start.equals(expectedRow.get("Expected Start Time (Kyiv Time)")) &&
                        server.equals(expectedRow.get("Expected Server"))) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Could not find an event matching: " + expectedRow);
        }
    }
}
