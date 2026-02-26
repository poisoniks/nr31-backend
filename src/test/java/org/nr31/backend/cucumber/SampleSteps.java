package org.nr31.backend.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleSteps {

    @Autowired
    private ApplicationContext applicationContext;

    private boolean isEnvironmentReady = false;
    private boolean actionResult = false;

    @Given("the application environment is ready")
    public void theApplicationEnvironmentIsReady() {
        assertNotNull(applicationContext, "Spring configuration failed to load context");
        isEnvironmentReady = true;
    }

    @When("a sample cucumber action is performed")
    public void aSampleCucumberActionIsPerformed() {
        if (isEnvironmentReady) {
            actionResult = true;
        }
    }

    @Then("the result of the test should be successful")
    public void theResultOfTheTestShouldBeSuccessful() {
        assertTrue(actionResult, "The sample action did not execute successfully.");
    }
}
