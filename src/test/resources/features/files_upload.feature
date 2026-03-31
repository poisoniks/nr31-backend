Feature: Files upload quota and role management

  Scenario: Admin can change file upload quota for a role
    Given I log in with user "admin" and password "testpass"
    And I find role ID for "ROLE_ADMIN" as "roleId"
    When I update the quota for role "{roleId}" to 10485760 bytes
    Then the response status code should be 204
    And the role "{roleId}" should have quota 10485760 bytes in the database

  Scenario: Non-existent role results in 404
    Given I log in with user "admin" and password "testpass"
    When I update the quota for role "999" to 10485760 bytes
    Then the response status code should be 404
