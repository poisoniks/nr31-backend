@schema
Feature: CMS Widget Schema
  As a frontend developer
  I want to retrieve JSON Schemas for widget types
  So that I can render dynamic admin forms

  Scenario: Retrieve all widget schemas
    Given I log in with user "admin" and password "testpass"
    When I request all widget schemas
    Then the response status code should be 200
    And the response body should contain "hero"
    And the response body should contain "richtext"
    And the response body should contain "nextevent"
    And the response body should contain "newsfeed"
    And the response body should contain "youtube"
    And the response body should contain "discord"

  Scenario Outline: Retrieve schema for specific widget type
    Given I log in with user "admin" and password "testpass"
    When I request the widget schema for type "<type>"
    Then the response status code should be 200
    And the response body should contain "properties"

    Examples:
      | type      |
      | hero      |
      | richtext  |
      | nextevent |
      | newsfeed  |
      | youtube   |
      | discord   |

  Scenario: Widget schema requires authentication
    When I request the widget schema for type "hero" without authentication
    Then the response status code should be 403

  Scenario: Widget schema requires cms:write permission
    Given I log in with user "user" and password "testpass"
    When I request the widget schema for type "hero"
    Then the response status code should be 403
