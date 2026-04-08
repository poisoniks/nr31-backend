package org.nr31.backend.cucumber.stepdefs;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublicSteps extends CommonStepDefs {

    @When("I request the list of supported locales")
    public void i_request_the_list_of_supported_locales() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/public/locales", null);
        contextHelper.addValue("response", response);
    }

    @Then("the response body should be a list of supported locales")
    public void the_response_body_should_be_a_list_of_supported_locales() throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.isArray() ? root : root.get("content");
        assertTrue(contentNode != null && contentNode.isArray(), "Expected result to be an array or page");
    }

    @Then("the list should contain a locale with code {string}")
    public void the_list_should_contain_a_locale_with_code(String expectedCode) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.isArray() ? root : root.get("content");
        boolean found = false;
        for (JsonNode node : contentNode) {
            if (expectedCode.equals(node.get("code").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find locale with code: " + expectedCode);
    }
}
