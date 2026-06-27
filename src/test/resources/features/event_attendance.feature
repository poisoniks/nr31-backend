Feature: Event Attendance Tracking

  Scenario: Admin can record and retrieve event attendance
    Given I log in with user "admin" and password "testpass"
    When I record attendance for the event "1000" on "2026-06-01T10:00:00Z" with members:
      | 1000 |
    Then the response status code should be 204
    When I request attendance for the event "1000" on "2026-06-01T10:00:00Z"
    Then the response status code should be 200
    And the response list should have size 1
    
    When I request monthly attendance for member "1000" for year 2026 and month 6
    Then the response status code should be 200
    And the response body should contain "totalScore" with value "1"
