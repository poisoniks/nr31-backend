package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminConfigSteps extends CommonStepDefs {

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

    @When("I retrieve all configs with the following parameters:")
    public void i_retrieve_all_configs_with_the_following_parameters(DataTable dataTable) throws Exception {
        Map<String, String> params = dataTable.asMap(String.class, String.class);
        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/config?" + queryString, null);
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

}
