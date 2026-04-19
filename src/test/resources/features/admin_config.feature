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

  Scenario: Prevent updates with missing required configValue field
    Given I log in with user "admin" and password "testpass"
    When I update the config "test_config_1" with the following payload:
      | name            | test_config_1 |
      | description.en  | Missing       |
    Then the response status code should be 400

  Scenario: Update application config missing name returns 404 Not Found
    Given I log in with user "admin" and password "testpass"
    When I update the config "unknown_config_nonexistent" with the following payload:
      | name            | unknown_config_nonexistent |
      | configValue     | {"enabled": true}          |
    Then the response status code should be 404

  Scenario: Tricky scenario - Reject updates with mismatched JSON inner types based on strict DB schema
    Given I log in with user "admin" and password "testpass"
    When I update the config "disabled_endpoints" with the following payload:
      | name            | disabled_endpoints |
      | configValue     | [1, 2, 3]          |
    Then the response status code should be 400
    And the response body should contain "details"

  Scenario: Check pagination attributes in response from get all configs
    Given I log in with user "admin" and password "testpass"
    When I retrieve all configs
    Then the response status code should be 200
    And the response body should contain "page.size" with value "20"
    And the response body should contain "page.number" with value "0"
    And the response body should contain "page.totalElements" with value "6"
    And the response body should contain "page.totalPages" with value "1"

  Scenario: Check pagination attributes in response from get all configs with query parameters
    Given I log in with user "admin" and password "testpass"
    When I retrieve all configs with the following parameters:
      | page | 0             |
      | size | 10            |
      | sort | configKey,asc |
    Then the response status code should be 200
    And the response body should contain "page.size" with value "10"
    And the response body should contain "page.number" with value "0"
    And the response body should contain "page.totalElements" with value "6"
    And the response body should contain "page.totalPages" with value "1"

  Scenario: Assign permission to role successfully
    Given I log in with user "admin" and password "testpass"
    When I assign permission "cache:clear" to role "ROLE_ADMIN"
    Then the response status code should be 204

  Scenario: Prevent unauthorized user from assigning permission
    Given I log in with user "user" and password "testpass"
    When I assign permission "cache:clear" to role "ROLE_ADMIN"
    Then the response status code should be 403

  Scenario: List all roles successfully
    Given I log in with user "admin" and password "testpass"
    When I retrieve all roles
    Then the response status code should be 200
    And the response list of configs should contain an item with name "ROLE_ADMIN"
    And the response list of configs should contain an item with name "ROLE_USER"

  Scenario: Get role by name successfully
    Given I log in with user "admin" and password "testpass"
    When I retrieve the role "ROLE_ADMIN"
    Then the response status code should be 200
    And the response body should contain name "ROLE_ADMIN"

  Scenario: Create, update and delete role successfully
    Given I log in with user "admin" and password "testpass"
    When I create a new role with name "ROLE_NEW_TEST" and localized name:
      | en | New Test Role |
      | uk | Нова тестова роль |
    Then the response status code should be 201
    And the response body should contain "name" with value "ROLE_NEW_TEST"
    And the response body should contain localized name:
      | en | New Test Role |
      | uk | Нова тестова роль |

    When I update the role "ROLE_NEW_TEST" to have name "ROLE_UPDATED_TEST" and localized name:
      | en | Updated Test Role |
    Then the response status code should be 200
    And the response body should contain "name" with value "ROLE_UPDATED_TEST"
    And the response body should contain localized name:
      | en | Updated Test Role |

    When I delete the role "ROLE_UPDATED_TEST"
    Then the response status code should be 204

    When I retrieve the role "ROLE_UPDATED_TEST"
    Then the response status code should be 404

  Scenario: Prevent unauthorized user from managing roles
    Given I log in with user "user" and password "testpass"
    When I retrieve all roles
    Then the response status code should be 403

    When I create a new role with name "ROLE_HACKER" and localized name:
      | en | Hacker |
    Then the response status code should be 403

  Scenario: Prevent creation of role with existing name
    Given I log in with user "admin" and password "testpass"
    When I create a new role with name "ROLE_ADMIN" and localized name:
      | en | Admin |
    Then the response status code should be 400

  Scenario: Prevent renaming role to an existing name
    Given I log in with user "admin" and password "testpass"
    When I create a new role with name "ROLE_TEMP" and localized name:
      | en | Temp Role |
    Then the response status code should be 201
    When I update the role "ROLE_TEMP" to have name "ROLE_ADMIN" and localized name:
      | en | Admin |
    Then the response status code should be 400

  Scenario: Update permission successfully
    Given I log in with user "admin" and password "testpass"
    When I update the permission "cache:clear" with the following description:
      | en | Can clear application cache updated |
      | uk | Може очистити кеш додатку |
    Then the response status code should be 200
    And the response body should contain updated permission description:
      | en | Can clear application cache updated |
      | uk | Може очистити кеш додатку |

  Scenario: List all users successfully
    Given I log in with user "admin" and password "testpass"
    When I retrieve all users
    Then the response status code should be 200
    And the response body should contain a user with username "admin"
    And the response body should contain a user with username "user"

  Scenario: Search users by username fuzzy match
    Given I log in with user "admin" and password "testpass"
    When I search users by username "adm"
    Then the response status code should be 200
    And the response body should contain a user with username "admin"

  Scenario: Prevent unauthorized user from listing users
    Given I log in with user "user" and password "testpass"
    When I retrieve all users
    Then the response status code should be 403

  Scenario: Unassign permission from role successfully
    Given I log in with user "admin" and password "testpass"
    When I assign permission "discord:manage" to role "ROLE_USER"
    Then the response status code should be 204
    When I unassign permission "discord:manage" from role "ROLE_USER"
    Then the response status code should be 204

  Scenario: Unassign role from user successfully
    Given I log in with user "admin" and password "testpass"
    When I assign role "ROLE_ADMIN" to user "user"
    Then the response status code should be 204
    When I unassign role "ROLE_ADMIN" from user "user"
    Then the response status code should be 204

  Scenario: Verify assigned permissions are present in RoleDTO
    Given I log in with user "admin" and password "testpass"
    When I assign permission "access:manage" to role "ROLE_ADMIN"
    And I retrieve the role "ROLE_ADMIN"
    Then the response status code should be 200
    And the response body should contain a permission with name "access:manage"

  Scenario: Verify system protection for SUPER_ADMIN role
    Given I log in with user "admin" and password "testpass"
    When I assign role "SUPER_ADMIN" to user "admin"
    Then the response status code should be 204
    When I unassign role "SUPER_ADMIN" from user "admin"
    Then the response status code should be 500

    When I assign role "SUPER_ADMIN" to user "user"
    Then the response status code should be 204
    When I unassign role "SUPER_ADMIN" from user "admin"
    Then the response status code should be 204

    When I update the role "SUPER_ADMIN" to have name "NORMAL_ADMIN" and localized name:
      | en | Normal Admin |
    Then the response status code should be 400
    When I delete the role "SUPER_ADMIN"
    Then the response status code should be 400
    When I unassign permission "access:manage" from role "SUPER_ADMIN"
    Then the response status code should be 400