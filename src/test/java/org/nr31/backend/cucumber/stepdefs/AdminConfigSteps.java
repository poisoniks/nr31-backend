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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminConfigSteps extends CommonStepDefs {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        JsonNode contentNode = root.isArray() ? root : root.get("content");
        assertTrue(contentNode != null && contentNode.isArray(), "Response content array missing or invalid");

        boolean found = false;
        for (JsonNode node : contentNode) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(configName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find an item with name: " + configName);
    }

    @And("the response body should contain name {string}")
    public void the_response_body_should_contain_name(String expectedName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        assertTrue(response.body().contains("\"name\":\"" + expectedName + "\""), 
                "Expected response to contain name: " + expectedName + "\nBody: " + response.body());
    }

    @And("the updated config value should be {string}")
    public void the_updated_config_value_should_be(String expectedValue) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertEquals(expectedValue, root.get("configValue").asText());
    }

    @When("I assign permission {string} to role {string}")
    public void i_assign_permission_to_role(String permissionName, String roleName) throws Exception {
        Long permissionId = jdbcTemplate.queryForObject("SELECT id FROM permissions WHERE name = ?", Long.class, permissionName);
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/admin/roles/" + roleId + "/permissions/" + permissionId, "");
        contextHelper.addValue("response", response);
    }

    @When("I retrieve all roles")
    public void i_retrieve_all_roles() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/roles", null);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the role {string}")
    public void i_retrieve_the_role(String roleName) throws Exception {
        try {
            Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
            HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/roles/" + roleId, null);
            contextHelper.addValue("response", response);
        } catch (EmptyResultDataAccessException e) {
            HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/roles/99999", null);
            contextHelper.addValue("response", response);
        }
    }

    @When("I create a new role with name {string} and localized name:")
    public void i_create_a_new_role(String roleName, DataTable dataTable) throws Exception {
        Map<String, String> localizedName = dataTable.asMap(String.class, String.class);
        String localizedNameJson = objectMapper.writeValueAsString(localizedName);
        String body = "{\"name\": \"" + roleName + "\", \"localizedName\": " + localizedNameJson + "}";
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/admin/roles", body);
        contextHelper.addValue("response", response);
    }

    @When("I update the role {string} to have name {string} and localized name:")
    public void i_update_the_role(String oldName, String newName, DataTable dataTable) throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, oldName);
        Map<String, String> localizedName = dataTable.asMap(String.class, String.class);
        String localizedNameJson = objectMapper.writeValueAsString(localizedName);
        String body = "{\"name\": \"" + newName + "\", \"localizedName\": " + localizedNameJson + "}";
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/admin/roles/" + roleId, body);
        contextHelper.addValue("response", response);
    }

    @And("the response body should contain localized name:")
    public void the_response_body_should_contain_localized_name(DataTable dataTable) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode localizedNameNode = root.get("localizedName");
        assertTrue(localizedNameNode != null && localizedNameNode.isObject(), "localizedName is missing or not an object");

        Map<String, String> expectedLocalizedName = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : expectedLocalizedName.entrySet()) {
            JsonNode val = localizedNameNode.get(entry.getKey());
            assertTrue(val != null, "Missing localized name for key: " + entry.getKey());
            assertEquals(entry.getValue(), val.asText());
        }
    }

    @When("I delete the role {string}")
    public void i_delete_the_role(String roleName) throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/admin/roles/" + roleId, null);
        contextHelper.addValue("response", response);
    }
}
