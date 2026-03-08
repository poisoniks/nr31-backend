Feature: App Config Management
  As an authenticated admin user
  I want to read and update application configurations
  So that I can customize system behavior without code deployment

  Scenario: Get existing application config by name successfully
    Given I log in with user "admin" and password "testpass"
    When I retrieve the config "test_config_1"
    Then the response status code should be 200
    And the response body should contain "configValue"
    And the response body should contain "name" with value "test_config_1"

  Scenario: Get all application configs successfully with pagination
    Given I log in with user "admin" and password "testpass"
    When I retrieve all configs
    Then the response status code should be 200
    And the response body should contain "content"
    And the response list of configs should contain an item with name "test_config_1"
    And the response list of configs should contain an item with name "test_config_2"

  Scenario: Update Application Config value
    Given I log in with user "admin" and password "testpass"
    When I update the config "test_config_2" with the following payload:
      | name            | test_config_2                     |
      | description.en  | Test Config 2                     |
      | configValue     | {"timeout": 9000}                 |
      | configSchema    | {"type": "object", "properties": {"timeout": {"type": "integer"}}, "required": ["timeout"]} |
    Then the response status code should be 200
    And the updated config value should be "{\"timeout\":9000}"

  Scenario: Prevent updates to Application Config with invalid JSON validation schema
    Given I log in with user "admin" and password "testpass"
    When I update the config "test_config_3" with the following payload:
      | name            | test_config_3     |
      | description.en  | Test Config 3     |
      | configValue     | {"retries": "five"} |
      | configSchema    | {"type": "object", "properties": {"retries": {"type": "integer"}}, "required": ["retries"]} |
    Then the response status code should be 400

  Scenario: Prevent unauthorized user from gaining config details
    Given I log in with user "user" and password "testpass"
    When I retrieve the config "test_config_1"
    Then the response status code should be 403

  Scenario: Get application config missing name returns 404 Not Found
    Given I log in with user "admin" and password "testpass"
    When I retrieve the config "unknown_config_nonexistent"
    Then the response status code should be 404
