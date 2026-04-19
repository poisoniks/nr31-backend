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

    @When("I update the role {string} with quota {long} bytes")
    public void i_update_the_role_with_quota(String roleName, Long quotaBytes) throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        String currentName = jdbcTemplate.queryForObject("SELECT name FROM roles WHERE id = ?", String.class, roleId);
        String body = "{\"name\": \"" + currentName + "\", \"filesUploadQuotaBytes\": " + quotaBytes + "}";
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/admin/roles/" + roleId, body);
        contextHelper.addValue("response", response);
    }

    @When("I update the role quota for role ID {string} to {long} bytes via admin API")
    public void i_update_the_role_quota_for_role_id_via_admin_api(String roleIdRef, Long quotaBytes) throws Exception {
        String roleId = resolveVariables(roleIdRef);
        String currentName = jdbcTemplate.queryForObject("SELECT name FROM roles WHERE id = ?", String.class, Long.parseLong(roleId));
        String body = "{\"name\": \"" + currentName + "\", \"filesUploadQuotaBytes\": " + quotaBytes + "}";
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

    @When("I update the permission {string} with the following description:")
    public void i_update_permission_description(String permissionName, DataTable dataTable) throws Exception {
        Long permissionId = jdbcTemplate.queryForObject("SELECT id FROM permissions WHERE name = ?", Long.class, permissionName);
        Map<String, String> description = dataTable.asMap(String.class, String.class);
        String descriptionJson = objectMapper.writeValueAsString(description);
        String body = "{\"description\": " + descriptionJson + "}";
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/admin/permissions/" + permissionId, body);
        contextHelper.addValue("response", response);
    }

    @And("the response body should contain updated permission description:")
    public void the_response_body_should_contain_updated_permission_description(DataTable dataTable) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode descriptionNode = root.get("description");
        assertTrue(descriptionNode != null && descriptionNode.isObject(), "description is missing or not an object");

        Map<String, String> expectedDescription = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : expectedDescription.entrySet()) {
            JsonNode val = descriptionNode.get(entry.getKey());
            assertTrue(val != null, "Missing description for key: " + entry.getKey());
            assertEquals(entry.getValue(), val.asText());
        }
    }

    @When("I retrieve all users")
    public void i_retrieve_all_users() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/users", null);
        contextHelper.addValue("response", response);
    }

    @When("I search users by username {string}")
    public void i_search_users_by_username(String username) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/admin/users/search?username=" + username, null);
        contextHelper.addValue("response", response);
    }

    @And("the response body should contain a user with username {string}")
    public void the_response_body_should_contain_a_user_with_username(String expectedUsername) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.isArray() ? root : root.get("content");
        assertTrue(contentNode != null && contentNode.isArray(), "Response content is not an array");

        boolean found = false;
        for (JsonNode node : contentNode) {
            JsonNode usernameNode = node.get("username");
            if (usernameNode != null && usernameNode.asText().equals(expectedUsername)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find user with username: " + expectedUsername);
    }

    @When("I unassign permission {string} from role {string}")
    public void i_unassign_permission_from_role(String permissionName, String roleName) throws Exception {
        Long permissionId = jdbcTemplate.queryForObject("SELECT id FROM permissions WHERE name = ?", Long.class, permissionName);
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/admin/roles/" + roleId + "/permissions/" + permissionId, null);
        contextHelper.addValue("response", response);
    }

    @When("I assign role {string} to user {string}")
    public void i_assign_role_to_user(String roleName, String username) throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/admin/users/" + userId + "/roles/" + roleId, "");
        contextHelper.addValue("response", response);
    }

    @When("I unassign role {string} from user {string}")
    public void i_unassign_role_from_user(String roleName, String username) throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/admin/users/" + userId + "/roles/" + roleId, null);
        contextHelper.addValue("response", response);
    }

    @And("the response body should contain a permission with name {string}")
    public void the_response_body_should_contain_a_permission_with_name(String expectedPermissionName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode permissionsNode = root.get("permissions");
        assertTrue(permissionsNode != null && permissionsNode.isArray(), "permissions list is missing or not an array");

        boolean found = false;
        for (JsonNode node : permissionsNode) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(expectedPermissionName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find permission with name: " + expectedPermissionName);
    }
}
