package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilesUploadSteps extends CommonStepDefs {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Given("I find role ID for {string} as {string}")
    public void i_find_role_id_for_as(String name, String varName) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = ?",
                Long.class,
                name
        );
        contextHelper.addValue(varName, id);
    }

    @When("I update the quota for role {string} to {long} bytes")
    public void i_update_the_quota_for_role_to_bytes(String roleIdRef, Long quotaBytes) throws Exception {
        String roleId = resolveVariables(roleIdRef);
        HttpResponse<String> response = makeApiCall("PATCH", "/api/v1/files/quota/role/" + roleId + "?quotaBytes=" + quotaBytes, null);
        contextHelper.addValue("response", response);
    }

    @Then("the role {string} should have quota {long} bytes in the database")
    public void the_role_should_have_quota_bytes_in_the_database(String roleIdRef, Long expectedQuota) {
        String roleId = resolveVariables(roleIdRef);
        Long actualQuota = jdbcTemplate.queryForObject(
                "SELECT files_upload_quota_bytes FROM roles WHERE id = ?",
                Long.class,
                Long.parseLong(roleId)
        );
        assertEquals(expectedQuota, actualQuota);
    }
}
