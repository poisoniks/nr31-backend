Feature: CMS Page Management
  As an administrator
  I want to manage page content with draft and published revisions
  So that I can create and publish dynamic web pages

  Scenario: Publish a draft page (happy path)
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "test-page" and title "Test Page"
    And the page has a draft revision with layout data
    When I publish the draft for page "test-page" with version 1
    Then the response status code should be 200
    And the response body should contain "version" with value "2"
    And the response body should contain "status" with value "PUBLISHED"

  Scenario: Version conflict during publication
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "conflict-page" and title "Conflict Page"
    And the page has a draft revision with layout data
    And the page version has been incremented to 3
    When I publish the draft for page "conflict-page" with version 1
    Then the response status code should be 409
    And the response body should contain "code" with value "CONFLICT"
    And the response body should contain metadata field "currentVersion" with value "3"

  Scenario: Slot restriction violation
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "restricted-page" and title "Restricted Page"
    And the page has a draft revision with layout data
    And slot restrictions allow only "text" and "image" in "sidebar" slots
    When I update the draft for page "restricted-page" with version 1 and a "video" widget in a "sidebar" slot
    Then the response status code should be 400
    And the response body should contain error message mentioning "video" and "sidebar"

  Scenario: Retrieve published page as public user
    Given a page exists with slug "public-page" and title "Public Page"
    And the page has a published revision with layout data
    When I retrieve the published page "public-page" as a public user
    Then the response status code should be 200
    And the response body should contain "slug" with value "public-page"
    And the response body should contain nested field "title.en" with value "Public Page"
    And the response body should contain "status" with value "PUBLISHED"
    And the response body should contain "layoutData"

  Scenario: Unauthorized access to admin endpoints
    Given a page exists with slug "admin-page" and title "Admin Page"
    And the page has a draft revision with layout data
    When I attempt to retrieve the draft for page "admin-page" without authentication
    Then the response status code should be 403

  Scenario: Insufficient permissions for CMS operations
    Given I log in with user "user" and password "testpass"
    And a page exists with slug "permission-page" and title "Permission Page"
    And the page has a draft revision with layout data
    When I attempt to update the draft for page "permission-page" with version 1
    Then the response status code should be 403

  Scenario: Non-existent page retrieval
    Given I log in with user "admin" and password "testpass"
    When I retrieve the draft for page "non-existent-page"
    Then the response status code should be 404
    And the response body should contain "code" with value "ELEMENT_NOT_FOUND"
    And the response body should contain metadata field "slug" with value "non-existent-page"

  Scenario: Update slot restrictions
    Given I log in with user "admin" and password "testpass"
    When I update slot restrictions with the following configuration:
      | hero    | text,image,video |
      | sidebar | text,image       |
      | content | text,image,video,embed |
    Then the response status code should be 200
    And the response body should contain slot restriction for "hero" with widgets "text,image,video"
    And the response body should contain slot restriction for "sidebar" with widgets "text,image"

  Scenario: Draft creation from published
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "copy-page" and title "Copy Page"
    And the page has a published revision with layout data
    And the page has no draft revision
    When I retrieve the draft for page "copy-page"
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the draft layout data should match the published layout data

  Scenario: Invalid layout data with missing required property
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "invalid-page" and title "Invalid Page"
    And the page has a draft revision with layout data
    When I update the draft for page "invalid-page" with version 1 and a text widget missing the "content" property
    Then the response status code should be 400
    And the response body should contain validation error for "content"

  Scenario: Retrieve draft page as admin
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "draft-page" and title "Draft Page"
    And the page has a draft revision with layout data
    When I retrieve the draft for page "draft-page"
    Then the response status code should be 200
    And the response body should contain "slug" with value "draft-page"
    And the response body should contain "status" with value "DRAFT"
    And the response body should contain "version" with value "1"

  Scenario: Update draft page with valid layout data
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "update-page" and title "Update Page"
    And the page has a draft revision with layout data
    When I update the draft for page "update-page" with version 1 and new layout data
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the response body should contain "version" with value "1"

  Scenario: Archive previous published revision on publication
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "archive-page" and title "Archive Page"
    And the page has a published revision with layout data
    And the page has a draft revision with layout data
    When I publish the draft for page "archive-page" with version 1
    Then the response status code should be 200
    And the previous published revision should be archived

  Scenario: Public user cannot access draft or archived pages
    Given a page exists with slug "draft-only-page" and title "Draft Only Page"
    And the page has a draft revision with layout data
    When I retrieve the published page "draft-only-page" as a public user
    Then the response status code should be 404

  Scenario: Get slot restrictions as admin
    Given I log in with user "admin" and password "testpass"
    When I retrieve the current slot restrictions
    Then the response status code should be 200
    And the response body should contain "restrictions"
