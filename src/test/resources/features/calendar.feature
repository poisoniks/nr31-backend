Feature: Calendar Events Management
  As an authenticated user
  I want to manage calendar events
  So that I can schedule training and unit activities

  Scenario: Successfully create a new single event
    Given I log in with user "admin" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": {
          "en": "Alpha Squad Briefing",
          "uk": "Брифінг загону Альфа"
        },
        "description": {
          "en": "Morning briefing for operation details",
          "uk": "Ранковий брифінг щодо деталей операції"
        },
        "start": "2026-10-27T08:00:00Z",
        "end": "2026-10-27T09:00:00Z",
        "type": 1,
        "serverName": "HQ Server",
        "participatingUnits": [1, 2],
        "recurrence": null
      }
      """
    Then the response status code should be 201
    And the response body should contain the created event ID
    And the response body should contain "serverName" with value "HQ Server"

  Scenario: Successfully create a recurring event
    Given I log in with user "admin" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Weekly Maintenance" },
        "description": { "en": "Server maintenance" },
        "start": "2026-11-01T10:00:00Z",
        "end": "2026-11-01T12:00:00Z",
        "type": 2,
        "serverName": "Backup Server",
        "recurrence": {
          "frequency": "WEEKLY",
          "interval": 1,
          "count": 10,
          "byDay": ["MO"]
        }
      }
      """
    Then the response status code should be 201
    And the response body should indicate "recurring" is true

  Scenario: Retrieve calendar events for a specific date range
    Given I log in with user "admin" and password "testpass"
    And I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Oct Event" },
        "start": "2026-10-15T10:00:00Z",
        "end": "2026-10-15T11:00:00Z",
        "type": 1,
        "serverName": "Main Server"
      }
      """
    When I send a GET request to "/api/v1/calendar/events" with parameters:
      | from     | 2026-10-01 |
      | to       | 2026-10-31 |
      | timezone | UTC        |
    Then the response status code should be 200
    And the response body should be a list of calendar events
    And each event in the list should have a valid "id" and "start" date

  Scenario: Update an existing event details
    Given I log in with user "admin" and password "testpass"
    And I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Training" },
        "start": "2026-10-28T14:00:00Z",
        "end": "2026-10-28T16:00:00Z",
        "type": 1,
        "serverName": "Main Server"
      }
      """
    And I save the created event ID as "eventId"
    When I send a PUT request to "/api/v1/calendar/events/{eventId}" with the following body:
      """
      {
        "mode": "SINGLE",
        "title": {
          "en": "Updated Training Session",
          "uk": "Оновлене тренування"
        },
        "serverName": "Main Server",
        "start": "2026-10-28T14:00:00Z",
        "end": "2026-10-28T16:00:00Z",
        "originalStart": "2026-10-28T14:00:00Z"
      }
      """
    Then the response status code should be 200
    And the response body should contain "title.en" with value "Updated Training Session"

  Scenario: Delete a single occurrence of an event
    Given I log in with user "admin" and password "testpass"
    And I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Event to delete" },
        "start": "2026-10-29T10:00:00Z",
        "end": "2026-10-29T11:00:00Z",
        "type": 1,
        "serverName": "Server 1"
      }
      """
    And I save the created event ID as "eventId"
    When I send a DELETE request to "/api/v1/calendar/events/{eventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 204
    And I should not be able to retrieve event "{eventId}"

  Scenario: Delete a recurring event series
    Given I log in with user "admin" and password "testpass"
    And I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Recurring series to delete" },
        "start": "2026-11-05T10:00:00Z",
        "end": "2026-11-05T11:00:00Z",
        "type": 1,
        "serverName": "Server 1",
        "recurrence": {
          "frequency": "WEEKLY",
          "interval": 1,
          "count": 5
        }
      }
      """
    And I save the created event ID as "eventId"
    When I send a DELETE request to "/api/v1/calendar/events/{eventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 204
    And all events in series "{eventId}" should be deleted

  Scenario: Get all events without authentication
    When I send a GET request to "/api/v1/calendar/events" with parameters:
      | from     | 2026-10-01 |
      | to       | 2026-10-31 |
      | timezone | UTC        |
    Then the response status code should be 200

  Scenario: Prevent unauthorized creation of events
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Unauthorized Event" },
        "start": "2026-10-15T10:00:00Z",
        "end": "2026-10-15T11:00:00Z",
        "type": 1,
        "serverName": "Main Server"
      }
      """
    Then the response status code should be 403

  Scenario: Prevent users with insufficient permissions from creating events
    Given I log in with user "user" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Unauthorized Event" },
        "start": "2026-10-15T10:00:00Z",
        "end": "2026-10-15T11:00:00Z",
        "type": 1,
        "serverName": "Main Server"
      }
      """
    Then the response status code should be 403

  Scenario: Create a recurring event ending on a specific date (Until)
    Given I log in with user "admin" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Project Phase 1" },
        "description": { "en": "Daily standup until launch" },
        "start": "2026-11-01T09:00:00Z",
        "end": "2026-11-01T09:15:00Z",
        "type": 1,
        "serverName": "HQ Server",
        "recurrence": {
          "frequency": "DAILY",
          "interval": 1,
          "until": "2026-11-10T23:59:59Z"
        }
      }
      """
    Then the response status code should be 201
    And the response body should indicate "recurring" is true

  Scenario: Create a complex recurring event on specific days of the week
    Given I log in with user "admin" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "MWF PT Session" },
        "start": "2026-11-02T06:00:00Z",
        "end": "2026-11-02T07:00:00Z",
        "type": 1,
        "serverName": "Gym Server",
        "recurrence": {
          "frequency": "WEEKLY",
          "interval": 1,
          "count": 12,
          "byDay": ["MO", "WE", "FR"]
        }
      }
      """
    Then the response status code should be 201
    And the response body should contain "seriesId"

  Scenario: Update 'FUTURE' events in a series (This and following)
    Given I log in with user "admin" and password "testpass"
    And I have created a recurring event series starting "2026-12-01T10:00:00Z" with ID "seriesId"
    When I send a PUT request to "/api/v1/calendar/events/{seriesId}" with the following body:
      """
      {
        "mode": "FUTURE",
        "title": { "en": "New Time Standup" },
        "serverName": "Main Server",
        "start": "2026-12-15T11:00:00Z",
        "end": "2026-12-15T11:30:00Z",
        "originalStart": "2026-12-15T10:00:00Z",
        "type": 1
      }
      """
    Then the response status code should be 200
    And the response body should match the new start time "2026-12-15T11:00:00Z"

  Scenario: Update 'ALL' events in a series
    Given I log in with user "admin" and password "testpass"
    And I have created a recurring event series with title "Old Title" and ID "seriesId"
    When I send a PUT request to "/api/v1/calendar/events/{seriesId}" with the following body:
      """
      {
        "mode": "ALL",
        "title": { "en": "Rebranded Title" },
        "serverName": "Main Server",
        "start": "2026-10-27T10:00:00Z",
        "end": "2026-10-27T12:00:00Z",
        "originalStart": "2026-10-27T10:00:00Z",
        "type": 1
      }
      """
    Then the response status code should be 200
    And subsequent GET requests for any date in the series should show title "Rebranded Title"

  Scenario: Delete 'FUTURE' occurrences of an event series
    Given I log in with user "admin" and password "testpass"
    And I have created a recurring event series with ID "seriesId"
    When I send a DELETE request to "/api/v1/calendar/events/{seriesId}" with parameters:
      | mode          | FUTURE               |
      | exceptionDate | 2026-11-15T10:00:00Z |
    Then the response status code should be 204
    And events in series "{seriesId}" after "2026-11-15" should not exist

  Scenario: Verify Event Persistence (Create then Retrieve)
    Given I log in with user "admin" and password "testpass"
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Persistence Check" },
        "start": "2026-12-25T10:00:00Z",
        "end": "2026-12-25T12:00:00Z",
        "type": 1,
        "serverName": "Main Server"
      }
      """
    Then the response status code should be 201
    When I send a GET request to "/api/v1/calendar/events" with parameters:
      | from | 2026-12-24 |
      | to   | 2026-12-26 |
    Then the response status code should be 200
    And the list should contain an event with title "Persistence Check"

  Scenario: Retrieve events with specific Timezone parameter
    Given I log in with user "admin" and password "testpass"
    And an event exists at "2026-10-27T10:00:00Z"
    When I send a GET request to "/api/v1/calendar/events" with parameters:
      | from     | 2026-10-27 |
      | to       | 2026-10-28 |
      | timezone | Europe/Kyiv |
    Then the response status code should be 200
    And the event start time should be adjusted to local time in the response

  Scenario: Create a recurring series in JST, modify a single instance in EST, delete another in IST, alter future instances in AEDT, and verify in EET
    Given I log in with user "admin" and password "testpass"

    # Step 1: Create the base recurring series (Mon, Wed, Fri) submitting times in Japan Standard Time (UTC+09:00)
    # 2026-11-03T00:00:00+09:00 is equivalent to 2026-11-02T15:00:00Z (UTC)
    When I send a POST request to "/api/v1/calendar/events" with the following body:
      """
      {
        "title": { "en": "Global Synchronization" },
        "start": "2026-11-03T00:00:00+09:00",
        "end": "2026-11-03T01:00:00+09:00",
        "type": 1,
        "serverName": "US-East-1",
        "recurrence": {
          "frequency": "WEEKLY",
          "interval": 1,
          "count": 12,
          "byDay": ["MO", "WE", "FR"]
        }
      }
      """
    Then the response status code should be 201
    And I save the created event "seriesId" as "globalSyncSeries"
    And I save the created event "id" for the Wednesday instance (2026-11-04) as "wedInstanceId"
    And I save the created event "id" for the Friday instance (2026-11-06) as "friInstanceId"
    And I save the created event "id" for the next Monday instance (2026-11-09) as "nextMonInstanceId"

    # Step 2: Update a SINGLE instance (Exception: Move Wednesday session 2 hours later) submitting in Eastern Standard Time (UTC-05:00)
    # Original start 15:00 UTC is 10:00 EST. New start 17:00 UTC is 12:00 EST.
    When I send a PUT request to "/api/v1/calendar/events/{wedInstanceId}" with the following body:
      """
      {
        "mode": "SINGLE",
        "title": { "en": "Global Synchronization - Delayed" },
        "start": "2026-11-04T12:00:00-05:00",
        "end": "2026-11-04T13:00:00-05:00",
        "originalStart": "2026-11-04T10:00:00-05:00",
        "type": 1,
        "serverName": "US-East-1"
      }
      """
    Then the response status code should be 200

    # Step 3: Delete a SINGLE instance (Exception: Cancel Friday session) submitting the exception date in India Standard Time (UTC+05:30)
    # Original start 15:00 UTC is 20:30 IST.
    When I send a DELETE request to "/api/v1/calendar/events/{friInstanceId}" with parameters:
      | mode          | SINGLE               |
      | exceptionDate | 2026-11-06T20:30:00+05:30 |
    Then the response status code should be 204

    # Step 4: Update FUTURE instances (Split series: Move all events from next Monday to a new server and time) submitting in Australian Eastern Daylight Time (UTC+11:00)
    # Original start 15:00 UTC (Nov 09) is 02:00 AEDT (Nov 10). New start 14:00 UTC (Nov 09) is 01:00 AEDT (Nov 10).
    When I send a PUT request to "/api/v1/calendar/events/{nextMonInstanceId}" with the following body:
      """
      {
        "mode": "FUTURE",
        "title": { "en": "Global Synchronization - Phase 2" },
        "start": "2026-11-10T01:00:00+11:00",
        "end": "2026-11-10T02:00:00+11:00",
        "originalStart": "2026-11-10T02:00:00+11:00",
        "type": 1,
        "serverName": "EU-Central",
        "recurrence": {
          "frequency": "WEEKLY",
          "interval": 1,
          "count": 9,
          "byDay": ["MO", "WE", "FR"]
        }
      }
      """
    Then the response status code should be 200

    # Step 5: Verify the complex state by retrieving data in Eastern European Time (Kyiv, UTC+02:00 in November)
    When I send a GET request to "/api/v1/calendar/events" with parameters:
      | from     | 2026-11-01  |
      | to       | 2026-11-15  |
      | timezone | Europe/Kyiv |
    Then the response status code should be 200
    And the response list should contain exactly the following state for the series:
      | Original Date (UTC) | Expected Title                     | Expected Start Time (Kyiv Time) | Expected Server | Status  |
      | 2026-11-02          | Global Synchronization             | 2026-11-02T17:00:00+02:00       | US-East-1       | Present |
      | 2026-11-04          | Global Synchronization - Delayed   | 2026-11-04T19:00:00+02:00       | US-East-1       | Present |
      | 2026-11-06          | N/A                                | N/A                             | N/A             | Deleted |
      | 2026-11-09          | Global Synchronization - Phase 2   | 2026-11-09T16:00:00+02:00       | EU-Central      | Present |
      | 2026-11-11          | Global Synchronization - Phase 2   | 2026-11-11T16:00:00+02:00       | EU-Central      | Present |
      | 2026-11-13          | Global Synchronization - Phase 2   | 2026-11-13T16:00:00+02:00       | EU-Central      | Present |