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
    When I register with username "newuser", email "newuser@example.com", and password "Password123!"
    Then the response status code should be 201
    And I retrieve the verification token for email "newuser@example.com" from Mailpit

  Scenario: Cannot register with duplicate username or email
    Given I register with username "dupuser", email "dupuser@example.com", and password "Password123!"
    Then the response status code should be 201
    When I register with username "dupuser", email "other@example.com", and password "Password123!"
    Then the response status code should be 409
    And the response body should contain "code" with value "USERNAME_ALREADY_EXISTS"
    When I register with username "otheruser", email "dupuser@example.com", and password "Password123!"
    Then the response status code should be 409
    And the response body should contain "code" with value "EMAIL_ALREADY_EXISTS"

  Scenario: Unverified users blocked by default, can log in after verification
    When I register with username "unverifieduser", email "unverified@example.com", and password "Password123!"
    Then the response status code should be 201
    When I log in with user "unverifieduser" and password "Password123!"
    Then the response status code should be 403
    When I retrieve the verification token for email "unverified@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I log in with user "unverifieduser" and password "Password123!"
    Then the response status code should be 200

  Scenario: Unverified users can log in if feature switch is disabled
    Given I set the feature switch "block_unverified_users" to "false"
    When I register with username "bypasseduser", email "bypass@example.com", and password "Password123!"
    Then the response status code should be 201
    When I log in with user "bypasseduser" and password "Password123!"
    Then the response status code should be 200

  Scenario: Try to verify email with invalid token
    When I verify the email with token "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 400

  Scenario: Successful resending of email verification
    When I register with username "resenduser", email "resend@example.com", and password "Password123!"
    Then the response status code should be 201
    When I retrieve the verification token for email "resend@example.com" from Mailpit
    And I clear Mailpit messages
    And I wait 3 seconds
    When I request to resend the verification email for "resend@example.com"
    Then the response status code should be 200
    When I retrieve the verification token for email "resend@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I log in with user "resenduser" and password "Password123!"
    Then the response status code should be 200

  Scenario: Resend verification email for a non-existent email
    When I request to resend the verification email for "nonexistent@example.com"
    Then the response status code should be 400

  Scenario: Resend verification email for an already verified email
    When I register with username "alreadyverified", email "alreadyverified@example.com", and password "Password123!"
    Then the response status code should be 201
    When I retrieve the verification token for email "alreadyverified@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    When I request to resend the verification email for "alreadyverified@example.com"
    Then the response status code should be 409

  Scenario: Resend verification email with invalid email format
    When I request to resend the verification email for "invalid-email-format"
    Then the response status code should be 400

  Scenario: Resend verification email too quickly returns 429 Too Many Requests
    When I register with username "rateuser", email "rate@example.com", and password "Password123!"
    Then the response status code should be 201
    When I request to resend the verification email for "rate@example.com"
    Then the response status code should be 429
    And the response body should contain "code" with value "TOO_MANY_REQUESTS"
    And the response body should contain "metadata.remainingSeconds"

  Scenario: Successful authenticated password change
    Given I log in with user "admin" and password "testpass"
    When I change the password with current password "testpass" and new password "NewPassword123!"
    Then the response status code should be 204
    When I log in with user "admin" and password "testpass"
    Then the response status code should be 401
    When I log in with user "admin" and password "NewPassword123!"
    Then the response status code should be 200

  Scenario: Authenticated password change fails if current password is incorrect
    Given I log in with user "admin" and password "testpass"
    When I change the password with current password "wrongcurrentpass" and new password "NewPassword123!"
    Then the response status code should be 401
    When I log in with user "admin" and password "testpass"
    Then the response status code should be 200

  Scenario: Authenticated password change fails if new password is same as current password
    Given I log in with user "admin" and password "testpass"
    When I change the password with current password "testpass" and new password "NewPassword123!"
    Then the response status code should be 204
    When I change the password with current password "NewPassword123!" and new password "NewPassword123!"
    Then the response status code should be 409
    And the response body should contain "code" with value "CONFLICT"

  Scenario: Authenticated password change fails if new password validation fails
    Given I log in with user "admin" and password "testpass"
    When I change the password with current password "testpass" and new password "weak"
    Then the response status code should be 400

  Scenario: Authenticated password change invalidates all refresh tokens
    Given I log in with user "admin" and password "testpass"
    And I save the response field "refreshToken" as "adminRefreshToken"
    When I change the password with current password "testpass" and new password "NewPassword123!"
    Then the response status code should be 204
    When I refresh the token with "{adminRefreshToken}"
    Then the response status code should be 400

  Scenario: Successful password reset flow
    When I register with username "resetuser", email "reset@example.com", and password "Password123!"
    Then the response status code should be 201
    When I retrieve the verification token for email "reset@example.com" from Mailpit
    And I verify the email with the retrieved token
    Then the response status code should be 204
    And I clear Mailpit messages
    When I request a password reset for email "reset@example.com"
    Then the response status code should be 200
    When I retrieve the password reset token for email "reset@example.com" from Mailpit
    And I reset the password with the retrieved token and new password "NewPassword123!"
    Then the response status code should be 204
    When I log in with user "resetuser" and password "Password123!"
    Then the response status code should be 401
    When I log in with user "resetuser" and password "NewPassword123!"
    Then the response status code should be 200

  Scenario: Request password reset for non-existent user returns silent success
    When I request a password reset for email "nonexistent@example.com"
    Then the response status code should be 200

  Scenario: Try to reset password with invalid token
    When I reset the password with token "00000000-0000-0000-0000-000000000000" and new password "NewPassword123!"
    Then the response status code should be 400
