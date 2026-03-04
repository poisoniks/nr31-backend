package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.After;
import io.cucumber.java.Before;
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

import java.util.Map;
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

    @When("I send a POST request to {string} with the following body:")
    public void i_send_a_post_request_to_with_the_following_body(String url, String body) throws Exception {
        HttpResponse<String> response = makeApiCall("POST", url, body);
        contextHelper.addValue("response", response);
    }

    @Given("I save the created event ID as {string}")
    public void i_save_the_created_event_id_as(String key) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("id"));
        String id = root.get("id").asText();
        contextHelper.addValue(key, id);
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(Integer expectedStatusCode) {
        HttpResponse<String> response = contextHelper.getValue("response");
        assertEquals(expectedStatusCode, response.statusCode(), "Body was: " + response.body());
    }

    @Then("the response body should contain the created event ID")
    public void the_response_body_should_contain_the_created_event_id() throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("id"));
        assertNotNull(root.get("id").asText());
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

    @When("I send a GET request to {string} with parameters:")
    public void i_send_a_get_request_to_with_parameters(String url, DataTable dataTable) throws Exception {
        Map<String, String> params = dataTable.asMap(String.class, String.class);
        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String fullUrl = url + (url.contains("?") ? "&" : "?") + queryString;

        HttpResponse<String> response = makeApiCall("GET", fullUrl, null);
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

    @When("I send a PUT request to {string} with the following body:")
    public void i_send_a_put_request_to_with_the_following_body(String url, String body) throws Exception {
        HttpResponse<String> response = makeApiCall("PUT", url, body);
        contextHelper.addValue("response", response);
    }

    @When("I send a DELETE request to {string} with parameters:")
    public void i_send_a_delete_request_to_with_parameters(String url, DataTable dataTable) throws Exception {
        Map<String, String> params = dataTable.asMap(String.class, String.class);
        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String fullUrl = url + (url.contains("?") ? "&" : "?") + queryString;

        HttpResponse<String> response = makeApiCall("DELETE", fullUrl, null);
        contextHelper.addValue("response", response);
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

    @Given("I have created a recurring event series starting {string} with ID {string}")
    public void i_have_created_a_recurring_event_series_starting_with_id(String expectedStartTime, String idVarName)
            throws Exception {
        String body = String.format("""
                {
                  "title": { "en": "Future Update Base Series" },
                  "start": "%s",
                  "end": "%s",
                  "type": 1,
                  "serverName": "Main Server",
                  "recurrence": { "frequency": "DAILY", "interval": 1, "count": 20 }
                }""", expectedStartTime, expectedStartTime.replace("10:00:00Z", "10:30:00Z"));

        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/events", body);
        assertEquals(201, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        String eventId = root.get("id").asText();
        contextHelper.addValue(idVarName, eventId);
    }

    @Given("I have created a recurring event series with title {string} and ID {string}")
    public void i_have_created_a_recurring_event_series_with_title_and_id(String title, String idVarName)
            throws Exception {
        String body = String.format("""
                {
                  "title": { "en": "%s" },
                  "start": "2026-10-27T10:00:00Z",
                  "end": "2026-10-27T12:00:00Z",
                  "type": 1,
                  "serverName": "Main Server",
                  "recurrence": { "frequency": "DAILY", "interval": 1, "count": 5 }
                }""", title);

        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/events", body);
        assertEquals(201, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        String eventId = root.get("id").asText();
        contextHelper.addValue(idVarName, eventId);
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String fieldName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has(fieldName), "Response does not contain " + fieldName + "\nBody: " + response.body());
    }

    @Given("I have created a recurring event series with ID {string}")
    public void i_have_created_a_recurring_event_series_with_id(String idVarName) throws Exception {
        i_have_created_a_recurring_event_series_with_title_and_id("Generic Series", idVarName);
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

    @Given("an event exists at {string}")
    public void an_event_exists_at(String startTime) throws Exception {
        String body = String.format("""
                {
                  "title": { "en": "Timezone check event" },
                  "start": "%s",
                  "end": "%s",
                  "type": 1,
                  "serverName": "Main Server"
                }""", startTime, startTime.replace("10:00:00Z", "11:00:00Z"));

        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/events", body);
        assertEquals(201, response.statusCode());
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

    private String resolveVariables(String text) {
        if (text == null)
            return null;
        String resolved = text;
        if (resolved.contains("{eventId}")) {
            String eventId = contextHelper.getValue("eventId");
            if (eventId != null) {
                resolved = resolved.replace("{eventId}", eventId);
            }
        }
        if (resolved.contains("{seriesId}")) {
            String seriesId = contextHelper.getValue("seriesId");
            if (seriesId != null) {
                resolved = resolved.replace("{seriesId}", seriesId);
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
}
