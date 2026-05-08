package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.net.http.HttpResponse;

public class AuthSteps extends CommonStepDefs {

    @Given("I log in with user {string} and password {string}")
    public void i_log_in_with_user_and_password(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/login", body);
        contextHelper.addValue("response", response);

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String token = root.get("accessToken").asString();
            contextHelper.addValue("jwt_token", token);
        }
    }

    @When("I refresh the token with {string}")
    public void i_refresh_the_token_with(String tokenRef) throws Exception {
        String token = resolveVariables(tokenRef);
        String body = String.format("{\"refreshToken\":\"%s\"}", token);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/refresh", body);
        contextHelper.addValue("response", response);
    }

    @When("I log out using the token {string}")
    public void i_log_out_using_the_token(String tokenRef) throws Exception {
        String token = resolveVariables(tokenRef);
        String body = String.format("{\"refreshToken\":\"%s\"}", token);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/logout", body);
        contextHelper.addValue("response", response);
    }

    @And("I save the response value {string} as {string}")
    public void i_save_the_response_value_as(String field, String varName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        contextHelper.addValue(varName, root.get(field).asString());
    }
}
