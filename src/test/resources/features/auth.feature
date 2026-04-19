Feature: Authentication and Token Management
  As a user of the application
  I want to be able to log in, refresh my token, and log out
  So that my data and session are secure

  Scenario: Successful login returns access and refresh tokens
    When I authenticate with username "admin" and password "testpass"
    Then the response status code should be 200
    And the response body should contain "accessToken"
    And the response body should contain "refreshToken"

  Scenario: Unsuccessful login returns 401 Unauthorized
    When I authenticate with username "admin" and password "wrongpass"
    Then the response status code should be 401

  Scenario: Successful token refresh
    When I authenticate with username "admin" and password "testpass"
    And I save the response value "refreshToken" as "savedRefreshToken"
    When I refresh the token with "{savedRefreshToken}"
    Then the response status code should be 200
    And the response body should contain "accessToken"
    And the response body should contain "refreshToken"

  Scenario: Try to refresh with invalid token
    When I refresh the token with "invalid.refresh.token"
    Then the response status code should be 400

  Scenario: Successful logout
    Given I log in with user "admin" and password "testpass"
    When I authenticate with username "admin" and password "testpass"
    And I save the response value "refreshToken" as "savedRefreshTokenForLogout"
    When I log out using the token "{savedRefreshTokenForLogout}"
    Then the response status code should be 204
    When I refresh the token with "{savedRefreshTokenForLogout}"
    Then the response status code should be 400

  Scenario: Logout with invalid token returns 204 (idempotent)
    Given I log in with user "admin" and password "testpass"
    When I log out using the token "invalid.refresh.token"
    Then the response status code should be 204

  Scenario: Prevent unauthorized access to logout endpoint
    Given I log out
    When I log out using the token "some.token.value"
    Then the response status code should be 403

  Scenario: Login with blank credentials returns 400 Bad Request
    When I authenticate with username "" and password ""
    Then the response status code should be 400
