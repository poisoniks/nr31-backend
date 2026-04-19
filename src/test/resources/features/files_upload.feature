Feature: File Management
  Tests for file upload, download, delete, CAS deduplication, quota enforcement,
  and the Media Library folder/file API

  Background:
    Given I log in with user "admin" and password "testpass"

  # ---------------------------------------------------------------------------
  # Files operations
  # ---------------------------------------------------------------------------

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

  # ---------------------------------------------------------------------------
  # Media Library — Folder Operations
  # ---------------------------------------------------------------------------

  Scenario: Create a root-level folder returns 201 with folder metadata
    When I create a library folder with name "Banners" and no parent
    Then the response status code should be 201
    And the response body should contain "id"
    And the response body should contain "name"
    And the response body should contain "createdAt"
    And the response body should contain "name" with value "Banners"

  Scenario: Create a nested folder returns 201 with correct parentId
    When I create a library folder with name "2026" and no parent
    And I save the created event "id" as "parentFolderId"
    When I create a library folder with name "Q1" under parent "{parentFolderId}"
    Then the response status code should be 201
    And the response body should contain "parentId" with value "{parentFolderId}"

  Scenario: Rename a folder via PATCH returns 200 with updated name
    When I create a library folder with name "OldName" and no parent
    And I save the created event "id" as "folderId"
    When I patch library folder "{folderId}" with name "NewName" and no parent
    Then the response status code should be 200
    And the response body should contain "name" with value "NewName"
    And the response body should contain "id" with value "{folderId}"

  Scenario: Move a folder to a different parent via PATCH returns 200
    When I create a library folder with name "Root" and no parent
    And I save the created event "id" as "rootId"
    When I create a library folder with name "Child" and no parent
    And I save the created event "id" as "childId"
    When I patch library folder "{childId}" with name "Child" under parent "{rootId}"
    Then the response status code should be 200
    And the response body should contain "parentId" with value "{rootId}"

  Scenario: Delete an empty folder returns 204
    When I create a library folder with name "Temporary" and no parent
    And I save the created event "id" as "folderId"
    When I delete library folder "{folderId}"
    Then the response status code should be 204

  Scenario: Delete a folder that contains a sub-folder returns 409
    When I create a library folder with name "Parent" and no parent
    And I save the created event "id" as "parentFolderId"
    When I create a library folder with name "Child" under parent "{parentFolderId}"
    When I delete library folder "{parentFolderId}"
    Then the response status code should be 409

  Scenario: Delete a folder that contains a file returns 409
    When I create a library folder with name "Occupied" and no parent
    And I save the created event "id" as "folderId"
    When I upload a PNG file "banner.png" to library folder "{folderId}"
    When I delete library folder "{folderId}"
    Then the response status code should be 409

  Scenario: Create a folder with a blank name returns 400
    When I create a library folder with name "" and no parent
    Then the response status code should be 400

  Scenario: Create a folder without authentication returns 403
    Given I log out
    When I create a library folder with name "Secret" and no parent
    Then the response status code should be 403

  Scenario: Create a folder under a non-existent parent returns 404
    When I create a library folder with name "Orphan" under parent "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: PATCH a non-existent folder returns 404
    When I patch library folder "00000000-0000-0000-0000-000000000000" with name "Ghost" and no parent
    Then the response status code should be 404

  Scenario: DELETE a non-existent folder returns 404
    When I delete library folder "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: User without delete permission cannot delete a folder
    Given I log in with user "user" and password "testpass"
    When I delete library folder "00000000-0000-0000-0000-000000000001"
    Then the response status code should be 403

  # ---------------------------------------------------------------------------
  # Media Library — File Operations
  # ---------------------------------------------------------------------------

  Scenario: Upload a library file without a folder places it at root level
    When I upload a PNG file "banner.png" to library root
    Then the response status code should be 201
    And the response body should contain "id"
    And the response body should contain "url"
    When I list library files at root
    Then the response status code should be 200
    And the library file list should contain an entry with name "banner.png"

  Scenario: Upload a library file into a folder assigns correct folderId
    When I create a library folder with name "Logos" and no parent
    And I save the created event "id" as "folderId"
    When I upload a PNG file "logo.png" to library folder "{folderId}"
    Then the response status code should be 201
    When I list library files in folder "{folderId}"
    Then the response status code should be 200
    And the library file list should contain an entry with name "logo.png"

  Scenario: List library files with no folderId returns only root-level files
    When I upload a PNG file "root1.png" to library root
    When I create a library folder with name "SubDir" and no parent
    And I save the created event "id" as "folderId"
    When I upload a PNG file "nested.png" to library folder "{folderId}"
    When I list library files at root
    Then the response status code should be 200
    And the library file list should contain an entry with name "root1.png"
    And the library file list should not contain an entry with name "nested.png"

  Scenario: List library files in a specific folder returns only that folder's files
    When I create a library folder with name "FolderA" and no parent
    And I save the created event "id" as "folderAId"
    When I create a library folder with name "FolderB" and no parent
    And I save the created event "id" as "folderBId"
    When I upload a PNG file "fileA.png" to library folder "{folderAId}"
    When I upload a PNG file "fileB.png" to library folder "{folderBId}"
    When I list library files in folder "{folderAId}"
    Then the response status code should be 200
    And the library file list should contain an entry with name "fileA.png"
    And the library file list should not contain an entry with name "fileB.png"

  Scenario: Rename a library file via PATCH updates its name without touching the physical file
    When I upload a PNG file "old-name.png" to library root
    And I save the created event "id" as "fileId"
    When I patch library file "{fileId}" with name "new-name.png" and no folder
    Then the response status code should be 200
    And the response body should contain "name" with value "new-name.png"

  Scenario: Move a library file to a folder via PATCH updates its folderId
    When I upload a PNG file "moveme.png" to library root
    And I save the created event "id" as "fileId"
    When I create a library folder with name "Target" and no parent
    And I save the created event "id" as "folderId"
    When I patch library file "{fileId}" with name "moveme.png" under folder "{folderId}"
    Then the response status code should be 200
    And the response body should contain "folderId" with value "{folderId}"
    When I list library files in folder "{folderId}"
    Then the response status code should be 200
    And the library file list should contain an entry with name "moveme.png"

  Scenario: Move a library file to root by sending null folderId
    When I create a library folder with name "Source" and no parent
    And I save the created event "id" as "folderId"
    When I upload a PNG file "toroot.png" to library folder "{folderId}"
    And I save the created event "id" as "fileId"
    When I patch library file "{fileId}" with name "toroot.png" and no folder
    Then the response status code should be 200
    When I list library files at root
    Then the response status code should be 200
    And the library file list should contain an entry with name "toroot.png"

  Scenario: Delete a library file removes only the metadata record
    When I upload a PNG file "deleteme.png" to library root
    And I save the created event "id" as "fileId"
    When I delete library file "{fileId}"
    Then the response status code should be 204
    When I get file "{fileId}"
    Then the response status code should be 404

  Scenario: GET /api/v1/files/{id} still resolves a library file by UUID
    When I upload a PNG file "serve-me.png" to library root
    And I save the created event "id" as "fileId"
    When I get file "{fileId}"
    Then the response status code should be 200
    And the response should have header "X-Accel-Redirect" starting with "/internal-files/"

  Scenario: Upload library file with unsupported type returns 400
    When I upload a text file "readme.txt" to library root
    Then the response status code should be 400

  Scenario: Upload library file without authentication returns 403
    Given I log out
    When I upload a PNG file "anon.png" to library root
    Then the response status code should be 403

  Scenario: Upload library file to non-existent folder returns 404
    When I upload a PNG file "lost.png" to library folder "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: PATCH non-existent library file returns 404
    When I patch library file "00000000-0000-0000-0000-000000000000" with name "ghost.png" and no folder
    Then the response status code should be 404

  Scenario: PATCH library file to non-existent folder returns 404
    When I upload a PNG file "target.png" to library root
    And I save the created event "id" as "fileId"
    When I patch library file "{fileId}" with name "target.png" under folder "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: Delete non-existent library file returns 404
    When I delete library file "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 404

  Scenario: User without library permission cannot list library files
    Given I log in with user "user" and password "testpass"
    When I list library files at root
    Then the response status code should be 403

  Scenario: Library file list response is paginated
    When I upload a PNG file "p1.png" to library root
    When I upload a PNG file "p2.png" to library root
    When I list library files at root with page 0 size 1
    Then the response status code should be 200
    And the response body should contain "page.totalElements"
    And the response body should contain "content"
