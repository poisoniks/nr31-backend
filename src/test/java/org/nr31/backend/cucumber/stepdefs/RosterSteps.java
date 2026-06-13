package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.net.http.HttpResponse;

public class RosterSteps extends CommonStepDefs {

    @When("I create an event type with the following details:")
    public void i_create_an_event_type_with_the_following_details(DataTable dataTable) throws Exception {
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/event-types", body);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the event type {string}")
    public void i_retrieve_the_event_type(String idRef) throws Exception {
        String id = resolveVariables(idRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/calendar/event-types/" + id, null);
        contextHelper.addValue("response", response);
    }

    @When("I update the event type {string} with the following details:")
    public void i_update_the_event_type_with_the_following_details(String idRef, DataTable dataTable) throws Exception {
        String id = resolveVariables(idRef);
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/calendar/event-types/" + id, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete the event type {string}")
    public void i_delete_the_event_type(String idRef) throws Exception {
        String id = resolveVariables(idRef);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/calendar/event-types/" + id, null);
        contextHelper.addValue("response", response);
    }

    @When("I create a unit type with the following details:")
    public void i_create_a_unit_type_with_the_following_details(DataTable dataTable) throws Exception {
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/roster/unit-types", body);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the unit type {string}")
    public void i_retrieve_the_unit_type(String idRef) throws Exception {
        String id = resolveVariables(idRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/roster/unit-types/" + id, null);
        contextHelper.addValue("response", response);
    }

    @When("I update the unit type {string} with the following details:")
    public void i_update_the_unit_type_with_the_following_details(String idRef, DataTable dataTable) throws Exception {
        String id = resolveVariables(idRef);
        String body = dataTableToJson(dataTable);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/roster/unit-types/" + id, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete the unit type {string}")
    public void i_delete_the_unit_type(String idRef) throws Exception {
        String id = resolveVariables(idRef);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/roster/unit-types/" + id, null);
        contextHelper.addValue("response", response);
    }
}
