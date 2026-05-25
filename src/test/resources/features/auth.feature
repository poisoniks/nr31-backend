Feature: Authentication and Token Management
  As a user of the application
  I want to be able to log in, refresh my token, and log out
  So that my data and session are secure

  Scenario: Successful login returns access and refresh tokens
    When I log in with user "admin" and password "testpass"
    Then the response status code should be 200
    And the response body should contain "accessToken"
    And the response body should contain "refreshToken"

  Scenario: Unsuccessful login returns 401 Unauthorized
    When I log in with user "admin" and password "wrongpass"
    Then the response status code should be 401

  Scenario: Successful token refresh
    When I log in with user "admin" and password "testpass"
    And I save the response field "refreshToken" as "savedRefreshToken"
    When I refresh the token with "{savedRefreshToken}"
    Then the response status code should be 200
    And the response body should contain "accessToken"
    And the response body should contain "refreshToken"

  Scenario: Try to refresh with invalid token
    When I refresh the token with "invalid.refresh.token"
    Then the response status code should be 400

  Scenario: Successful logout
    Given I log in with user "admin" and password "testpass"
    And I save the response field "refreshToken" as "savedRefreshTokenForLogout"
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
    When I log in with user "" and password ""
    Then the response status code should be 400

  Scenario: Successful user registration
    When I register with username "newuser", email "newuser@example.com", and password "password123"
    Then the response status code should be 201
    And I retrieve the verification token for email "newuser@example.com" from Mailpit

  Scenario: Cannot register with duplicate username or email
    Given I register with username "dupuser", email "dupuser@example.com", and password "password123"
    Then the response status code should be 201
    When I register with username "dupuser", email "other@example.com", and password "password123"
    Then the response status code should be 409
    When I register with username "otheruser", email "dupuser@example.com", and password "password123"
    Then the response status code should be 409

  Scenario: Unverified users blocked by default, can log in after verification
    When I register with username "unverifieduser", email "unverified@example.com", and password "password123"
    Then the response status code should be 201
    When I log in with user "unverifieduser" and password "password123"
    Then the response status code should be 403
    When I retrieve the verification token for email "unverified@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I log in with user "unverifieduser" and password "password123"
    Then the response status code should be 200

  Scenario: Unverified users can log in if feature switch is disabled
    Given I set the feature switch "block_unverified_users" to "false"
    When I register with username "bypasseduser", email "bypass@example.com", and password "password123"
    Then the response status code should be 201
    When I log in with user "bypasseduser" and password "password123"
    Then the response status code should be 200

  Scenario: Try to verify email with invalid token
    When I verify the email with token "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 400

  Scenario: Successful resending of email verification
    When I register with username "resenduser", email "resend@example.com", and password "password123"
    Then the response status code should be 201
    When I retrieve the verification token for email "resend@example.com" from Mailpit
    And I clear Mailpit messages
    When I request to resend the verification email for "resend@example.com"
    Then the response status code should be 200
    When I retrieve the verification token for email "resend@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I log in with user "resenduser" and password "password123"
    Then the response status code should be 200

  Scenario: Resend verification email for a non-existent email
    When I request to resend the verification email for "nonexistent@example.com"
    Then the response status code should be 400

  Scenario: Resend verification email for an already verified email
    When I register with username "alreadyverified", email "alreadyverified@example.com", and password "password123"
    Then the response status code should be 201
    When I retrieve the verification token for email "alreadyverified@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I request to resend the verification email for "alreadyverified@example.com"
    Then the response status code should be 409

  Scenario: Resend verification email with invalid email format
    When I request to resend the verification email for "invalid-email-format"
    Then the response status code should be 400
