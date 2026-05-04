Feature: CMS Widget Replacement
  As an administrator
  I want to use the new production-ready widgets (HeroWidget, RichTextWidget, NextEventWidget, NewsFeedWidget)
  So that I can create rich, dynamic content for the regiment website

  Scenario: Create draft with HeroWidget
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "home" and title "Home Page"
    And the page has a draft revision with layout data
    When I update the draft for page "home" with version 1 and a HeroWidget containing:
      | badgeText.en      | M&B Warband Regiment                           |
      | badgeText.uk      | Полк M&B Warband                               |
      | titleMain         | Nr.31                                          |
      | titleSub          | Feldkanonenregiment                            |
      | description.en    | Join the elite artillery regiment              |
      | description.uk    | Приєднуйтесь до елітного артилерійського полку |
      | ctaText.en        | Join Now                                       |
      | ctaText.uk        | Приєднатися зараз                              |
      | ctaTargetId       | how-to-join                                    |
      | backgroundImageId | 550e8400-e29b-41d4-a716-446655440000           |
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the draft should contain a widget of type "hero"

  Scenario: Create draft with RichTextWidget
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "about" and title "About Page"
    And the page has a draft revision with layout data
    When I update the draft for page "about" with version 1 and a RichTextWidget containing:
      | bodyContent.en | {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Welcome to Nr.31 FKR"}]}]}         |
      | bodyContent.uk | {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Ласкаво просимо до Nr.31 FKR"}]}]} |
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the draft should contain a widget of type "richtext"

  Scenario: Create draft with NextEventWidget
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "events" and title "Events Page"
    And the page has a draft revision with layout data
    When I update the draft for page "events" with version 1 and a NextEventWidget containing:
      | titleOverride.en | Upcoming Official Match  |
      | titleOverride.uk | Наступний офіційний матч |
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the draft should contain a widget of type "nextevent"

  Scenario: Create draft with NewsFeedWidget
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "news" and title "News Page"
    And the page has a draft revision with layout data
    When I update the draft for page "news" with version 1 and a NewsFeedWidget containing:
      | sectionTitle.en | Latest News    |
      | sectionTitle.uk | Останні новини |
      | itemCount       | 3              |
      | tagFilter       | announcements  |
    Then the response status code should be 200
    And the response body should contain "status" with value "DRAFT"
    And the draft should contain a widget of type "newsfeed"

  Scenario: Reject RichTextWidget exceeding size limit
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "large-content" and title "Large Content Page"
    And the page has a draft revision with layout data
    And the AppConfig key "cms.richtext.max_size_bytes" is set to 1048576
    When I update the draft for page "large-content" with version 1 and a RichTextWidget with content exceeding 1048576 bytes
    Then the response status code should be 400
    And the response body should contain validation error mentioning "size" or "exceeds"

  Scenario: Reject NewsFeedWidget with itemCount exceeding limit
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "news-feed" and title "News Feed Page"
    And the page has a draft revision with layout data
    And the AppConfig key "cms.newsfeed.max_items" is set to 50
    When I update the draft for page "news-feed" with version 1 and a NewsFeedWidget with itemCount 100
    Then the response status code should be 400
    And the response body should contain validation error mentioning "itemCount" or "exceeds"

  Scenario: Reject HeroWidget with non-existent backgroundImageId
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "hero-page" and title "Hero Page"
    And the page has a draft revision with layout data
    When I update the draft for page "hero-page" with version 1 and a HeroWidget with non-existent backgroundImageId "00000000-0000-0000-0000-000000000000"
    Then the response status code should be 400
    And the response body should contain validation error mentioning "backgroundImageId" or "not found"

  Scenario: Reject widget not allowed in slot (new widget types)
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "restricted" and title "Restricted Page"
    And the page has a draft revision with layout data
    And slot restrictions allow only "hero" in "hero" slots
    When I update the draft for page "restricted" with version 1 and a RichTextWidget in a "hero" slot
    Then the response status code should be 400
    And the response body should contain error message mentioning "richtext" and "hero"

  Scenario: Publish draft with new widgets
    Given I log in with user "admin" and password "testpass"
    And a page exists with slug "publish-test" and title "Publish Test Page"
    And the page has a draft revision with a HeroWidget
    When I publish the draft for page "publish-test" with version 1
    Then the response status code should be 200
    And the response body should contain "status" with value "PUBLISHED"
    And the response body should contain "version" with value "2"
    And the published page should contain a widget of type "hero"

  Scenario: Retrieve published page with new widgets as public user
    Given a page exists with slug "public-widgets" and title "Public Widgets Page"
    And the page has a published revision with the following widgets:
      | type      | slot    |
      | hero      | hero    |
      | richtext  | content |
      | nextevent | sidebar |
      | newsfeed  | content |
    When I retrieve the published page "public-widgets" as a public user
    Then the response status code should be 200
    And the response body should contain "slug" with value "public-widgets"
    And the response body should contain "status" with value "PUBLISHED"
    And the response body should contain "layoutData"
    And the published page should contain a widget of type "hero"
    And the published page should contain a widget of type "richtext"
    And the published page should contain a widget of type "nextevent"
    And the published page should contain a widget of type "newsfeed"
