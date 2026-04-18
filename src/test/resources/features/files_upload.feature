Feature: File Management
  Tests for file upload, download, delete, CAS deduplication, and quota enforcement

  Background:
    Given I log in with user "admin" and password "testpass"

  Scenario: Upload attachment returns 201 with file metadata
    When I upload a PNG file "icon.png" as "attachment"
    Then the response status code should be 201
    And the response body should contain "id"
    And the response body should contain "originalName"
    And the response body should contain "url"
    And the response body should contain "size"

  Scenario: Upload library file returns 201
    When I upload a PNG file "banner.png" as "library"
    Then the response status code should be 201
    And the response body should contain "id"

  Scenario: Get file returns X-Accel-Redirect header
    When I upload a PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId"
    When I get file "{fileId}"
    Then the response status code should be 200
    And the response should have header "X-Accel-Redirect" starting with "/internal-files/"
    And the response should have header "Content-Type" with value "image/png"

  Scenario: Delete file returns 204 and GET returns 404
    When I upload a PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId"
    When I delete file "{fileId}"
    Then the response status code should be 204
    When I get file "{fileId}"
    Then the response status code should be 404

  Scenario: CAS deduplication - same content uploaded twice shares the same hash
    When I upload a PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId1"
    When I upload the same PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId2"
    Then files "{fileId1}" and "{fileId2}" should have different UUIDs
    And files "{fileId1}" and "{fileId2}" should have the same stored hash

  Scenario: Upload different allowed types all succeed
    When I upload a JPEG file "photo.jpg" as "attachment"
    Then the response status code should be 201
    When I upload a WEBP file "image.webp" as "attachment"
    Then the response status code should be 201

  Scenario: Upload empty file returns 400
    When I upload an empty file as "attachment"
    Then the response status code should be 400

  Scenario: Upload unsupported file type returns 400
    When I upload a text file "readme.txt" as "attachment"
    Then the response status code should be 400

  Scenario: Upload without authentication returns 403
    Given I log out
    When I upload a PNG file "icon.png" as "attachment"
    Then the response status code should be 403

  Scenario: Get non-existent file returns 404
    When I get file "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: Delete non-existent file returns 404
    When I delete file "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: Upload exceeding quota returns 400
    Given I find role ID for "ROLE_ADMIN" as "adminRoleId"
    When I update the quota for role "{adminRoleId}" to 10 bytes
    Then the response status code should be 204
    When I upload a PNG file "icon.png" as "attachment"
    Then the response status code should be 400
    And the response body should contain "message" with value containing "quota"

  Scenario: Delete one of two CAS-deduplicated files, other remains accessible
    When I upload a PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId1"
    When I upload the same PNG file "icon.png" as "attachment"
    And I save the created event "id" as "fileId2"
    When I delete file "{fileId1}"
    Then the response status code should be 204
    When I get file "{fileId2}"
    Then the response status code should be 200

  Scenario: User without library permission cannot upload library files
    Given I log in with user "user" and password "testpass"
    When I upload a PNG file "icon.png" as "library"
    Then the response status code should be 403
