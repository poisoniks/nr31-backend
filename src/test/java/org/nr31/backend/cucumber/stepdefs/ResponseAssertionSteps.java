package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

public class ResponseAssertionSteps extends CommonStepDefs {

    @Before(order = 1)
    public void setup() {
        contextHelper.initialize();
    }

    @After
    public void tearDown() {
        contextHelper.release();
    }

    @Given("I log out")
    public void i_log_out() {
        contextHelper.addValue("jwt_token", null);
    }

    @Given("I find role ID for {string} as {string}")
    public void i_find_role_id_for_as(String name, String varName) throws Exception {
        HttpResponse<String> resp = makeApiCall("GET", "/api/v1/admin/roles?size=200", null);
        JsonNode content = objectMapper.readTree(resp.body()).get("content");
        for (JsonNode node : content) {
            if (name.equals(node.get("name").asString())) {
                contextHelper.addValue(varName, node.get("id").asLong());
                return;
            }
        }
        throw new IllegalStateException("Role not found via API: " + name);
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(Integer expectedStatusCode) {
        HttpResponse<String> response = contextHelper.getValue("response");
        assertEquals(expectedStatusCode, response.statusCode(), "Body was: " + response.body());
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String jsonPath) {
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
        assertTrue(current != null && !current.isMissingNode(), "Response does not contain " + jsonPath + "\nBody: " + response.body());
    }

    @Then("the response body should contain {string} with value {string}")
    public void the_response_body_should_contain_with_value(String jsonPath, String expectedValue) {
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
        assertEquals(resolveVariables(expectedValue), current.asString());
    }

    @Then("the response body should indicate {string} is true")
    public void the_response_body_should_indicate_is_true(String fieldName) {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has(fieldName));
        assertTrue(root.get(fieldName).asBoolean());
    }

    @Then("the response body should contain array {string} with length {int}")
    public void the_response_body_should_contain_array_with_length(String jsonPath, int expectedLength) {
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
        assertTrue(current.isArray(), "Path " + jsonPath + " is not an array");
        assertEquals(expectedLength, current.size());
    }

    @Then("the response list should have size {int}")
    public void the_response_list_should_have_size(int expectedSize) {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        assertEquals(expectedSize, root.size());
    }

    @Then("the response body should contain {string} with value containing {string}")
    public void the_response_body_should_contain_field_with_value_containing(String field, String substring) {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertNotNull(root.get(field), "Field '" + field + "' not found in: " + response.body());
        String value = root.get(field).asString();
        assertTrue(value.toLowerCase().contains(substring.toLowerCase()),
                "Expected field '" + field + "' to contain '" + substring + "' but was: " + value);
    }

    @Then("the response should have header {string} starting with {string}")
    public void the_response_should_have_header_starting_with(String headerName, String prefix) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String value = response.headers().firstValue(headerName.toLowerCase()).orElse(null);
        assertNotNull(value, "Header '" + headerName + "' not found in response");
        assertTrue(value.startsWith(prefix),
                "Expected header '" + headerName + "' to start with '" + prefix + "' but was '" + value + "'");
    }

    @Then("the response should have header {string} with value {string}")
    public void the_response_should_have_header_with_value(String headerName, String expectedValue) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String value = response.headers().firstValue(headerName.toLowerCase()).orElse(null);
        assertNotNull(value, "Header '" + headerName + "' not found in response");
        assertEquals(expectedValue, value);
    }

    @And("the response body should contain metadata field {string} with value {string}")
    public void the_response_body_should_contain_metadata_field_with_value(String field, String expectedValue) {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode metadata = root.get("metadata");

        assertNotNull(metadata, "metadata field is missing");
        JsonNode fieldNode = metadata.get(field);
        assertNotNull(fieldNode, "metadata." + field + " is missing");
        assertEquals(expectedValue, fieldNode.asString());
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

    @And("the response body should contain validation error for {string}")
    public void the_response_body_should_contain_validation_error_for(String field) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String body = response.body();

        assertTrue(body.contains(field) || body.toLowerCase().contains(field.toLowerCase()),
                "Expected validation error for field: " + field + "\nBody: " + body);
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

    @And("I save the response field {string} as {string}")
    public void i_save_the_response_field_as(String field, String varName) {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        contextHelper.addValue(varName, root.get(field).asString());
    }
}
