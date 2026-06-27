package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.nr31.backend.cucumber.ScenarioContextHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventAttendanceSteps extends CommonStepDefs {

    @Autowired
    private ScenarioContextHelper contextHelper;

    @When("I record attendance for the event {string} on {string} with members:")
    public void i_record_attendance_for_the_event_on_with_members(String eventIdRef, String occurrenceDate, DataTable dataTable) throws Exception {
        String eventId = resolveVariables(eventIdRef);
        List<Long> memberIds = dataTable.asList(Long.class);
        
        String body = String.format("{\"occurrenceDate\": \"%s\", \"memberIds\": %s}", occurrenceDate, memberIds.toString());
        
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/calendar/events/" + eventId + "/attendance", body);
        contextHelper.addValue("response", response);
    }

    @When("I request attendance for the event {string} on {string}")
    public void i_request_attendance_for_the_event_on(String eventIdRef, String occurrenceDate) throws Exception {
        String eventId = resolveVariables(eventIdRef);
        String queryString = "occurrenceDate=" + URLEncoder.encode(occurrenceDate, StandardCharsets.UTF_8);
        
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/calendar/events/" + eventId + "/attendance?" + queryString, null);
        contextHelper.addValue("response", response);
    }

    @When("I request monthly attendance for member {string} for year {int} and month {int}")
    public void i_request_monthly_attendance_for_member_for_year_and_month(String memberIdRef, int year, int month) throws Exception {
        String memberId = resolveVariables(memberIdRef);
        String queryString = "year=" + year + "&month=" + month;
        
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/roster/members/" + memberId + "/attendance?" + queryString, null);
        contextHelper.addValue("response", response);
    }
}
