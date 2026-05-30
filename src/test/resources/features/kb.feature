Feature: Knowledge Base Management
  As a user of the application
  I want to read and search articles in the Knowledge Base
  And as an author or administrator, I want to manage KB folders and articles

  Background:
    Given I log out

  Scenario: Retrieve root folders as a public user
    When I retrieve the root folders
    Then the response status code should be 200
    And the response list should have size 3
    And the response body should contain "0.slug" with value "admin-only"
    And the response body should contain "1.slug" with value "empty-folder"
    And the response body should contain "2.slug" with value "general-support"

  Scenario: Retrieve folder details by slug as a public user
    When I retrieve the folder details for "general-support"
    Then the response status code should be 200
    And the response body should contain "slug" with value "general-support"
    And the response body should contain "name.en" with value "General Support"
    And the response body should contain "restricted" with value "false"
    And the response body should contain array "subFolders" with length 1
    And the response body should contain "subFolders.0.slug" with value "user-guides"
    And the response body should contain "articles.content.0.slug" with value "how-to-reset-password"

  Scenario: Retrieve folder details with custom pagination size
    When I retrieve the folder details for "general-support" with pagination size 1
    Then the response status code should be 200
    And the response body should contain array "articles.content" with length 1

  Scenario: Retrieve article by slug as a public user
    When I retrieve the article details for "how-to-reset-password"
    Then the response status code should be 200
    And the response body should contain "slug" with value "how-to-reset-password"
    And the response body should contain "title.en" with value "How to Reset Password"
    And the response body should contain "authorName" with value "admin"
    And the response body should contain array "breadcrumbs" with length 1
    And the response body should contain "breadcrumbs.0.slug" with value "general-support"

  Scenario: Search articles via full-text search
    When I search articles with query "settings"
    Then the response status code should be 200
    And the response list should have size 1
    And the response body should contain "0.article.slug" with value "how-to-reset-password"
    And the response body should contain array "0.breadcrumbs" with length 1

  Scenario: Retrieve non-existent folder
    When I retrieve the folder details for "non-existent-folder"
    Then the response status code should be 404
    And the response body should contain "code" with value "KB_FOLDER_NOT_FOUND"

  Scenario: Retrieve non-existent article
    When I retrieve the article details for "non-existent-article"
    Then the response status code should be 404
    And the response body should contain "code" with value "KB_ARTICLE_NOT_FOUND"

  Scenario: Search articles with empty query returns empty list
    When I search articles with query ""
    Then the response status code should be 200
    And the response list should have size 0

  Scenario: Admin successfully creates a new folder
    Given I log in with user "admin" and password "testpass"
    When I create a new folder named "New Support Folder" and restricted is false
    Then the response status code should be 201
    And the response body should contain "slug" with value "new-support-folder"
    And the response body should contain "name.en" with value "New Support Folder"
    And the response body should contain "restricted" with value "false"

  Scenario: Admin successfully creates a sub-folder
    Given I log in with user "admin" and password "testpass"
    When I create a sub-folder named "Advanced Guides" under parent 102 and restricted is true
    Then the response status code should be 201
    And the response body should contain "slug" with value "advanced-guides"
    And the response body should contain "restricted" with value "true"

  Scenario: Admin updates folder name and restricted status
    Given I log in with user "admin" and password "testpass"
    When I rename folder 103 to "Renamed Empty Folder" and set restricted to true
    Then the response status code should be 200
    And the response body should contain "slug" with value "renamed-empty-folder"
    And the response body should contain "restricted" with value "true"

  Scenario: Admin updates subfolder parent to root
    Given I log in with user "admin" and password "testpass"
    When I update folder 102 parent to root
    Then the response status code should be 200
    And the response body should contain "slug" with value "user-guides"

  Scenario: Admin deletes empty folder
    Given I log in with user "admin" and password "testpass"
    When I delete folder 103
    Then the response status code should be 204

  Scenario: Try to delete non-empty folder
    Given I log in with user "admin" and password "testpass"
    When I delete folder 100
    Then the response status code should be 409
    And the response body should contain "code" with value "KB_FOLDER_NOT_EMPTY"

  Scenario: Try to set parent folder to self
    Given I log in with user "admin" and password "testpass"
    When I set folder 100 parent to 100
    Then the response status code should be 409
    And the response body should contain "code" with value "CONFLICT"

  Scenario: Try circular parent hierarchy
    Given I log in with user "admin" and password "testpass"
    When I set folder 100 parent to 102
    Then the response status code should be 409
    And the response body should contain "code" with value "CONFLICT"

  Scenario: Public user cannot manage folders
    When I attempt to create a folder named "Public Folder Attempt"
    Then the response status code should be 403

  Scenario: Author cannot manage folders
    Given I log in with user "kbauthor" and password "testpass"
    When I attempt to create a folder named "Author Folder Attempt"
    Then the response status code should be 403

  Scenario: Create folder with invalid payload (missing name)
    Given I log in with user "admin" and password "testpass"
    When I create a folder with an invalid payload missing the name
    Then the response status code should be 400
    And the response body should contain validation error for "name"

  Scenario: Author successfully creates a new article
    Given I log in with user "kbauthor" and password "testpass"
    When I create a new article in folder 100 with title "Getting Started" and content "This is a welcome article."
    Then the response status code should be 201
    And the response body should contain "slug" with value "getting-started"
    And the response body should contain "title.en" with value "Getting Started"
    And the response body should contain "authorName" with value "kbauthor"

  Scenario: Author successfully updates their own article
    Given I log in with user "kbauthor" and password "testpass"
    When I update article 201 with title "Updated Author Guide" and content "Updated content here."
    Then the response status code should be 200
    And the response body should contain "slug" with value "updated-author-guide"
    And the response body should contain "title.en" with value "Updated Author Guide"

  Scenario: Admin successfully updates any article
    Given I log in with user "admin" and password "testpass"
    When I update article 201 with title "Admin Overrides Guide"
    Then the response status code should be 200
    And the response body should contain "slug" with value "admin-overrides-guide"

  Scenario: Author successfully deletes their own article
    Given I log in with user "kbauthor" and password "testpass"
    When I delete article 201
    Then the response status code should be 204

  Scenario: Admin successfully deletes any article
    Given I log in with user "admin" and password "testpass"
    When I delete article 201
    Then the response status code should be 204

  Scenario: Public user cannot create article
    When I attempt to create an article in folder 100 with title "Public Article Attempt"
    Then the response status code should be 403

  Scenario: Regular user cannot create article
    Given I log in with user "user" and password "testpass"
    When I attempt to create an article in folder 100 with title "Regular User Article Attempt"
    Then the response status code should be 403

  Scenario: Author cannot create article in a restricted folder
    Given I log in with user "kbauthor" and password "testpass"
    When I attempt to create an article in folder 101 with title "Restricted Article Attempt"
    Then the response status code should be 403

  Scenario: Author cannot update an article authored by someone else
    Given I log in with user "kbauthor" and password "testpass"
    When I update article 200 with title "Malicious Update Guide"
    Then the response status code should be 403

  Scenario: Author cannot delete an article authored by someone else
    Given I log in with user "kbauthor" and password "testpass"
    When I delete article 200
    Then the response status code should be 403

  Scenario: Create article with invalid payload (missing folderId)
    Given I log in with user "kbauthor" and password "testpass"
    When I create an article with an invalid payload missing folderId
    Then the response status code should be 400
    And the response body should contain validation error for "folderId"
