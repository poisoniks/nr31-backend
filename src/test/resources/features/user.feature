Feature: User Profile Retrieval
  As an authenticated user of the application
  I want to retrieve my profile information
  So that I can see my account details

  Scenario: Successful retrieval of current user profile
    Given I log in with user "admin" and password "testpass"
    When I retrieve my user profile
    Then the response status code should be 200
    And the response body should contain "username" with value "admin"
    And the response body should contain "email" with value "admin@example.com"
    And the response body should contain "emailVerified" with value "true"
    And the response body should contain "roles"

  Scenario: Retrieval of current user profile fails when not authenticated
    Given I log out
    When I retrieve my user profile
    Then the response status code should be 403

