Feature: Roster Management
  As an admin user
  I want to manage event types and unit types

  Scenario: Create, retrieve, update, and delete an event type
    Given I log in with user "admin" and password "testpass"
    When I create an event type with the following details:
      | name.en    | Skirmish |
    Then the response status code should be 201
    And I save the response field "id" as "eventTypeId"

    When I retrieve the event type "{eventTypeId}"
    Then the response status code should be 200
    And the response body should contain "name.en" with value "Skirmish"

    When I update the event type "{eventTypeId}" with the following details:
      | name.en    | Skirmish Updated |
    Then the response status code should be 200

    When I retrieve the event type "{eventTypeId}"
    Then the response status code should be 200
    And the response body should contain "name.en" with value "Skirmish Updated"

    When I delete the event type "{eventTypeId}"
    Then the response status code should be 204

    When I retrieve the event type "{eventTypeId}"
    Then the response status code should be 404

  Scenario: Create, retrieve, update, and delete a unit type
    Given I log in with user "admin" and password "testpass"
    When I create a unit type with the following details:
      | name.en        | 31st Regiment |
      | description.en | Line Infantry |
    Then the response status code should be 201
    And I save the response field "id" as "unitTypeId"

    When I retrieve the unit type "{unitTypeId}"
    Then the response status code should be 200
    And the response body should contain "name.en" with value "31st Regiment"

    When I update the unit type "{unitTypeId}" with the following details:
      | name.en        | 31st Regiment Updated |
      | description.en | Updated Description   |
    Then the response status code should be 200

    When I retrieve the unit type "{unitTypeId}"
    Then the response status code should be 200
    And the response body should contain "name.en" with value "31st Regiment Updated"

    When I delete the unit type "{unitTypeId}"
    Then the response status code should be 204

    When I retrieve the unit type "{unitTypeId}"
    Then the response status code should be 404

  Scenario: Error cases for unit types and event types
    Given I log in with user "admin" and password "testpass"

    When I update the unit type "99999" with the following details:
      | name.en | Non-existent |
    Then the response status code should be 404

    When I delete the unit type "99999"
    Then the response status code should be 404

    When I update the event type "99999" with the following details:
      | name.en | Non-existent |
    Then the response status code should be 404

    When I delete the event type "99999"
    Then the response status code should be 404

