package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.nr31.backend.cucumber.ScenarioContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminConfigSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private ScenarioContextHelper contextHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @When("I retrieve the config {string}")
    public void i_retrieve_the_config(String configName) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/config/" + configName, null);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve all configs")
    public void i_retrieve_all_configs() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/config", null);
        contextHelper.addValue("response", response);
    }

    @When("I update the config {string} with the following payload:")
    public void i_update_the_config_with_payload(String configName, DataTable dataTable) throws Exception {
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/admin/config/" + configName, body);
        contextHelper.addValue("response", response);
    }

    @And("the response list of configs should contain an item with name {string}")
    public void response_list_should_contain_an_item_with_name(String configName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());

        JsonNode contentNode = root.get("content");
        assertTrue(contentNode.isArray(), "Response content array missing or invalid");

        boolean found = false;
        for (JsonNode node : contentNode) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(configName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find an item with config name: " + configName);
    }

    @And("the updated config value should be {string}")
    public void the_updated_config_value_should_be(String expectedValue) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertEquals(expectedValue, root.get("configValue").asText());
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
                current.put(key, value);
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

    private HttpResponse<String> makeApiCall(String method, String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + url))
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
