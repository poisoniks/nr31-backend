package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.en.When;
import java.net.http.HttpResponse;

public class UserSteps extends CommonStepDefs {

    @When("I retrieve my user profile")
    public void i_retrieve_my_user_profile() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/users/me", null);
        contextHelper.addValue("response", response);
    }
}
