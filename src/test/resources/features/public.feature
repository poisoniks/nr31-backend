Feature: Public API
  As a public user
  I want to access regimental information
  So that I can join or stay informed

  Scenario: Retrieve all supported locales
    When I request the list of supported locales
    Then the response status code should be 200
    And the response body should be a list of supported locales
    And the list should contain a locale with code "en"

  Scenario: Retrieve all allowed MIME types
    When I request the list of allowed MIME types
    Then the response status code should be 200
    And the response body should contain MIME type "application/pdf"
    And the response body should contain MIME type "application/zip"
    And the response body should contain MIME type "text/plain"
    And the response body should contain MIME type "image/png"
