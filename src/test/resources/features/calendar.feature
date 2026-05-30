Feature: Calendar Events Management
  As an authenticated user
  I want to manage calendar events
  So that I can schedule training and unit activities

  Scenario: Successfully create a new single event
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en           | Alpha Squad Briefing                   |
      | title.uk           | Брифінг загону Альфа                   |
      | description.en     | Morning briefing for operation details |
      | description.uk     | Ранковий брифінг щодо деталей операції |
      | start              | 2026-10-27T08:00:00Z                   |
      | end                | 2026-10-27T09:00:00Z                   |
      | type               | 1                                      |
      | serverName         | HQ Server                              |
      | participatingUnits | [1, 2]                                 |
      | recurrence         | null                                   |
    Then the response status code should be 201
    And the response body should contain "id"
    And the response body should contain "serverName" with value "HQ Server"

  Scenario: Successfully create a recurring event
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Weekly Maintenance   |
      | description.en       | Server maintenance   |
      | start                | 2026-11-01T10:00:00Z |
      | end                  | 2026-11-01T12:00:00Z |
      | type                 | 2                    |
      | serverName           | Backup Server        |
      | recurrence.frequency | WEEKLY               |
      | recurrence.interval  | 1                    |
      | recurrence.count     | 10                   |
      | recurrence.byDay     | ["MO"]               |
    Then the response status code should be 201
    And the response body should indicate "recurring" is true

  Scenario: Retrieve calendar events for a specific date range
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en   | Oct Event            |
      | start      | 2026-10-15T10:00:00Z |
      | end        | 2026-10-15T11:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    When I retrieve events with the following parameters:
      | from     | 2026-10-01 |
      | to       | 2026-10-31 |
      | timezone | UTC        |
    Then the response status code should be 200
    And the response body should be a list of calendar events
    And each event in the list should have a valid "id" and "start" date

  Scenario: Update an existing event details
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en   | Training             |
      | start      | 2026-10-28T14:00:00Z |
      | end        | 2026-10-28T16:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    And I save the response field "id" as "eventId"
    When I update the event "{eventId}" with the following details:
      | mode          | SINGLE                   |
      | title.en      | Updated Training Session |
      | title.uk      | Оновлене тренування      |
      | serverName    | Main Server              |
      | start         | 2026-10-28T14:00:00Z     |
      | end           | 2026-10-28T16:00:00Z     |
      | originalStart | 2026-10-28T14:00:00Z     |
      | type          | 1                        |
    Then the response status code should be 200
    And the response body should contain "title.en" with value "Updated Training Session"

  Scenario: Delete a single occurrence of an event
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en   | Event to delete      |
      | start      | 2026-10-29T10:00:00Z |
      | end        | 2026-10-29T11:00:00Z |
      | type       | 1                    |
      | serverName | Server 1             |
    And I save the response field "id" as "eventId"
    When I delete the event "{eventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 204
    And I should not be able to retrieve event "{eventId}"

  Scenario: Delete a recurring event series
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en             | Recurring series to delete |
      | start                | 2026-11-05T10:00:00Z       |
      | end                  | 2026-11-05T11:00:00Z       |
      | type                 | 1                          |
      | serverName           | Server 1                   |
      | recurrence.frequency | WEEKLY                     |
      | recurrence.interval  | 1                          |
      | recurrence.count     | 5                          |
    And I save the response field "id" as "eventId"
    When I delete the event "{eventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 204
    And all events in series "{eventId}" should be deleted

  Scenario: Get all events without authentication
    When I retrieve events with the following parameters:
      | from     | 2026-10-01 |
      | to       | 2026-10-31 |
      | timezone | UTC        |
    Then the response status code should be 200

  Scenario: Prevent unauthorized creation of events
    When I create an event with the following details:
      | title.en   | Unauthorized Event   |
      | start      | 2026-10-15T10:00:00Z |
      | end        | 2026-10-15T11:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    Then the response status code should be 403

  Scenario: Prevent users with insufficient permissions from creating events
    Given I log in with user "user" and password "testpass"
    When I create an event with the following details:
      | title.en   | Unauthorized Event   |
      | start      | 2026-10-15T10:00:00Z |
      | end        | 2026-10-15T11:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    Then the response status code should be 403

  Scenario: Create a recurring event ending on a specific date (Until)
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Project Phase 1            |
      | description.en       | Daily standup until launch |
      | start                | 2026-11-01T09:00:00Z       |
      | end                  | 2026-11-01T09:15:00Z       |
      | type                 | 1                          |
      | serverName           | HQ Server                  |
      | recurrence.frequency | DAILY                      |
      | recurrence.interval  | 1                          |
      | recurrence.until     | 2026-11-10T23:59:59Z       |
    Then the response status code should be 201
    And the response body should indicate "recurring" is true

  Scenario: Create a complex recurring event on specific days of the week
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | MWF PT Session       |
      | start                | 2026-11-02T06:00:00Z |
      | end                  | 2026-11-02T07:00:00Z |
      | type                 | 1                    |
      | serverName           | Gym Server           |
      | recurrence.frequency | WEEKLY               |
      | recurrence.interval  | 1                    |
      | recurrence.count     | 12                   |
      | recurrence.byDay     | ["MO", "WE", "FR"]   |
    Then the response status code should be 201
    And the response body should contain "seriesId"

  Scenario: Update 'FUTURE' events in a series (This and following)
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Future Update Base Series |
      | start                | 2026-12-01T10:00:00Z      |
      | end                  | 2026-12-01T10:30:00Z      |
      | type                 | 1                         |
      | serverName           | Main Server               |
      | recurrence.frequency | DAILY                     |
      | recurrence.interval  | 1                         |
      | recurrence.count     | 20                        |
    Then the response status code should be 201
    And I save the response field "id" as "seriesId"
    When I update the event "{seriesId}" with the following details:
      | mode          | FUTURE               |
      | title.en      | New Time Standup     |
      | serverName    | Main Server          |
      | start         | 2026-12-15T11:00:00Z |
      | end           | 2026-12-15T11:30:00Z |
      | originalStart | 2026-12-15T10:00:00Z |
      | type          | 1                    |
    Then the response status code should be 200
    And the response body should match the new start time "2026-12-15T11:00:00Z"

  Scenario: Update 'ALL' events in a series
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Old Title            |
      | start                | 2026-10-27T10:00:00Z |
      | end                  | 2026-10-27T12:00:00Z |
      | type                 | 1                    |
      | serverName           | Main Server          |
      | recurrence.frequency | DAILY                |
      | recurrence.interval  | 1                    |
      | recurrence.count     | 5                    |
    Then the response status code should be 201
    And I save the response field "id" as "seriesId"
    When I update the event "{seriesId}" with the following details:
      | mode          | ALL                  |
      | title.en      | Rebranded Title      |
      | serverName    | Main Server          |
      | start         | 2026-10-27T10:00:00Z |
      | end           | 2026-10-27T12:00:00Z |
      | originalStart | 2026-10-27T10:00:00Z |
      | type          | 1                    |
    Then the response status code should be 200
    And subsequent GET requests for any date in the series should show title "Rebranded Title"

  Scenario: Delete 'FUTURE' occurrences of an event series
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Generic Series       |
      | start                | 2026-10-27T10:00:00Z |
      | end                  | 2026-10-27T12:00:00Z |
      | type                 | 1                    |
      | serverName           | Main Server          |
      | recurrence.frequency | DAILY                |
      | recurrence.interval  | 1                    |
      | recurrence.count     | 5                    |
    Then the response status code should be 201
    And I save the response field "id" as "seriesId"
    When I delete the event "{seriesId}" with parameters:
      | mode          | FUTURE               |
      | exceptionDate | 2026-11-15T10:00:00Z |
    Then the response status code should be 204
    And events in series "{seriesId}" after "2026-11-15" should not exist

  Scenario: Verify Event Persistence (Create then Retrieve)
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en   | Persistence Check    |
      | start      | 2026-12-25T10:00:00Z |
      | end        | 2026-12-25T12:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    Then the response status code should be 201
    When I retrieve events with the following parameters:
      | from | 2026-12-24 |
      | to   | 2026-12-26 |
    Then the response status code should be 200
    And the list should contain an event with title "Persistence Check"

  Scenario: Retrieve events with specific Timezone parameter
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en   | Timezone check event |
      | start      | 2026-10-27T10:00:00Z |
      | end        | 2026-10-27T11:00:00Z |
      | type       | 1                    |
      | serverName | Main Server          |
    Then the response status code should be 201
    When I retrieve events with the following parameters:
      | from     | 2026-10-27  |
      | to       | 2026-10-28  |
      | timezone | Europe/Kyiv |
    Then the response status code should be 200
    And the event start time should be adjusted to local time in the response

  Scenario: Create a recurring series in JST, modify a single instance in EST, delete another in IST, alter future instances in AEDT, and verify in EET
    Given I log in with user "admin" and password "testpass"

    # Step 1: Create the base recurring series (Mon, Wed, Fri) submitting times in Japan Standard Time (UTC+09:00)
    # 2026-11-03T00:00:00+09:00 is equivalent to 2026-11-02T15:00:00Z (UTC)
    When I create an event with the following details:
      | title.en             | Global Synchronization    |
      | start                | 2026-11-03T00:00:00+09:00 |
      | end                  | 2026-11-03T01:00:00+09:00 |
      | type                 | 1                         |
      | serverName           | US-East-1                 |
      | recurrence.frequency | WEEKLY                    |
      | recurrence.interval  | 1                         |
      | recurrence.count     | 12                        |
      | recurrence.byDay     | ["MO", "WE", "FR"]        |
    Then the response status code should be 201
    And I save the response field "seriesId" as "globalSyncSeries"
    And I save the response field "id" as "baseInstanceId"

    # Step 2: Update a SINGLE instance (Exception: Move Wednesday session 2 hours later) submitting in Eastern Standard Time (UTC-05:00)
    # Original start 15:00 UTC is 10:00 EST. New start 17:00 UTC is 12:00 EST.
    When I update the event "{baseInstanceId}" with the following details:
      | mode          | SINGLE                           |
      | title.en      | Global Synchronization - Delayed |
      | start         | 2026-11-04T12:00:00-05:00        |
      | end           | 2026-11-04T13:00:00-05:00        |
      | originalStart | 2026-11-04T10:00:00-05:00        |
      | type          | 1                                |
      | serverName    | US-East-1                        |
    Then the response status code should be 200

    # Step 3: Delete a SINGLE instance (Exception: Cancel Friday session) submitting the exception date in India Standard Time (UTC+05:30)
    # Original start 15:00 UTC is 20:30 IST.
    When I delete the event "{baseInstanceId}" with parameters:
      | mode          | SINGLE                    |
      | exceptionDate | 2026-11-06T20:30:00+05:30 |
    Then the response status code should be 204

    # Step 4: Update FUTURE instances (Split series: Move all events from next Monday to a new server and time) submitting in Australian Eastern Daylight Time (UTC+11:00)
    # Original start 15:00 UTC (Nov 09) is 02:00 AEDT (Nov 10). New start 14:00 UTC (Nov 09) is 01:00 AEDT (Nov 10).
    When I update the event "{baseInstanceId}" with the following details:
      | mode                 | FUTURE                           |
      | title.en             | Global Synchronization - Phase 2 |
      | start                | 2026-11-10T01:00:00+11:00        |
      | end                  | 2026-11-10T02:00:00+11:00        |
      | originalStart        | 2026-11-10T02:00:00+11:00        |
      | type                 | 1                                |
      | serverName           | EU-Central                       |
      | recurrence.frequency | WEEKLY                           |
      | recurrence.interval  | 1                                |
      | recurrence.count     | 9                                |
      | recurrence.byDay     | ["MO", "WE", "FR"]               |
    Then the response status code should be 200

    # Step 5: Verify the complex state by retrieving data in Eastern European Time (Kyiv, UTC+02:00 in November)
    When I retrieve events with the following parameters:
      | from     | 2026-11-01  |
      | to       | 2026-11-15  |
      | timezone | Europe/Kyiv |
    Then the response status code should be 200
    And the response list should contain exactly the following state for the series:
      | Original Date (UTC) | Expected Title                   | Expected Start Time (Kyiv Time) | Expected Server | Status  |
      | 2026-11-02          | Global Synchronization           | 2026-11-02T17:00:00+02:00       | US-East-1       | Present |
      | 2026-11-04          | Global Synchronization - Delayed | 2026-11-04T19:00:00+02:00       | US-East-1       | Present |
      | 2026-11-06          | N/A                              | N/A                             | N/A             | Deleted |
      | 2026-11-09          | Global Synchronization - Phase 2 | 2026-11-09T16:00:00+02:00       | EU-Central      | Present |
      | 2026-11-11          | Global Synchronization - Phase 2 | 2026-11-11T16:00:00+02:00       | EU-Central      | Present |
      | 2026-11-13          | Global Synchronization - Phase 2 | 2026-11-13T16:00:00+02:00       | EU-Central      | Present |

  Scenario: Retrieve nearest event to a public user
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en   | First Event          |
      | start      | 2026-05-10T10:00:00Z |
      | end        | 2026-05-10T11:00:00Z |
      | type       | 1                    |
      | serverName | Public Server        |
    And I create an event with the following details:
      | title.en   | Second Event         |
      | start      | 2026-05-12T10:00:00Z |
      | end        | 2026-05-12T11:00:00Z |
      | type       | 1                    |
      | serverName | Public Server        |
    And I log out
    When I retrieve the nearest event to "2026-05-10T12:00:00Z"
    Then the response status code should be 200
    And the response body should contain "title.en" with value "Second Event"

  Scenario: Update a single occurrence of a recurring event with different participating units
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en             | Unit override base event |
      | start                | 2026-10-30T10:00:00Z     |
      | end                  | 2026-10-30T11:00:00Z     |
      | type                 | 1                        |
      | serverName           | Main Server              |
      | participatingUnits   | [1, 2]                   |
      | recurrence.frequency | DAILY                    |
      | recurrence.interval  | 1                        |
      | recurrence.count     | 5                        |
    And I save the response field "id" as "seriesId"
    When I update the event "{seriesId}" with the following details:
      | mode               | SINGLE               |
      | title.en           | Unit override update |
      | start              | 2026-10-31T10:00:00Z |
      | end                | 2026-10-31T11:00:00Z |
      | originalStart      | 2026-10-31T10:00:00Z |
      | type               | 1                    |
      | participatingUnits | [2]                  |
    Then the response status code should be 200
    And the response body should contain array "participatingUnits" with length 1
    And the response body should contain "participatingUnits.0.id" with value "2"

  Scenario: Prevent 'FUTURE' update for Discord-sourced events
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en             | Discord Event Base   |
      | start                | 2026-10-27T10:00:00Z |
      | end                  | 2026-10-27T11:00:00Z |
      | type                 | 1                    |
      | serverName           | Discord Server       |
      | recurrence.frequency | DAILY                |
      | recurrence.count     | 5                    |
    And I save the response field "id" as "discordEventId"
    And the event "{discordEventId}" has source "DISCORD"
    When I update the event "{discordEventId}" with the following details:
      | mode          | FUTURE               |
      | title.en      | Attempted Update     |
      | originalStart | 2026-10-28T10:00:00Z |
      | start         | 2026-10-28T11:00:00Z |
      | type          | 1                    |
    Then the response status code should be 400
    And the response body should contain "message" with value "Unable to update events with source DISCORD in FUTURE mode"

  Scenario: Delete a non-recurring event successfully regardless of mode
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en   | Single case event    |
      | start      | 2026-10-10T10:00:00Z |
      | end        | 2026-10-10T11:00:00Z |
      | type       | 1                    |
      | serverName | Test Server          |
    And I save the response field "id" as "eventId"
    When I delete the event "{eventId}" with parameters:
      | mode          | SINGLE               |
      | exceptionDate | 2026-10-10T10:00:00Z |
    Then the response status code should be 204
    And I should not be able to retrieve event "{eventId}"

  Scenario: Prevent 'ALL' deletion for Discord-sourced events
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en             | Discord Series To Delete |
      | start                | 2026-11-20T10:00:00Z     |
      | end                  | 2026-11-20T11:00:00Z     |
      | type                 | 1                        |
      | serverName           | Discord Server           |
      | recurrence.frequency | DAILY                    |
      | recurrence.count     | 5                        |
    And I save the response field "id" as "discordEventId"
    And the event "{discordEventId}" has source "DISCORD"
    When I delete the event "{discordEventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 400
    And the response body should contain "message" with value "Unable to delete event synced from Discord server"

  Scenario: Prevent 'FUTURE' deletion for Discord-sourced events
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en             | Discord Series Future Delete |
      | start                | 2026-11-20T10:30:00Z         |
      | end                  | 2026-11-20T11:30:00Z         |
      | type                 | 1                            |
      | serverName           | Discord Server               |
      | recurrence.frequency | DAILY                        |
      | recurrence.count     | 5                            |
    And I save the response field "id" as "discordEventId"
    And the event "{discordEventId}" has source "DISCORD"
    When I delete the event "{discordEventId}" with parameters:
      | mode          | FUTURE               |
      | exceptionDate | 2026-11-22T10:30:00Z |
    Then the response status code should be 400
    And the response body should contain "message" with value "Unable to delete event synced from Discord server"

  Scenario: Prevent deletion for Discord-sourced non-recurring event
    Given I log in with user "admin" and password "testpass"
    And I create an event with the following details:
      | title.en   | Discord Single Event |
      | start      | 2026-10-10T11:00:00Z |
      | end        | 2026-10-10T12:00:00Z |
      | type       | 1                    |
      | serverName | Discord Server       |
    And I save the response field "id" as "discordEventId"
    And the event "{discordEventId}" has source "DISCORD"
    When I delete the event "{discordEventId}" with parameters:
      | mode | SINGLE |
    Then the response status code should be 400
    And the response body should contain "message" with value "Unable to delete event synced from Discord server"

  Scenario: Lifecycle of event recurrence and deletions
    Given I log in with user "admin" and password "testpass"
    When I create an event with the following details:
      | title.en             | Alpha Squad Practice |
      | start                | 2026-05-01T10:00:00Z |
      | end                  | 2026-05-01T11:00:00Z |
      | type                 | 1                    |
      | serverName           | HQ Server            |
      | recurrence.frequency | DAILY                |
      | recurrence.count     | 5                    |
    Then the response status code should be 201
    And the response body should indicate "recurring" is true
    And I save the response field "id" as "eventId"
    When I update the event "{eventId}" with the following details:
      | mode       | ALL                  |
      | title.en   | Alpha Squad Single   |
      | start      | 2026-05-01T10:00:00Z |
      | end        | 2026-05-01T11:00:00Z |
      | type       | 1                    |
      | recurrence | null                 |
      | serverName | HQ Server            |
    Then the response status code should be 200
    When I retrieve events with the following parameters:
      | from     | 2026-05-01 |
      | to       | 2026-05-10 |
      | timezone | UTC        |
    Then the response list should have size 1
    When I update the event "{eventId}" with the following details:
      | mode                 | SINGLE                |
      | title.en             | Weekly Alpha Training |
      | start                | 2026-05-01T10:00:00Z  |
      | end                  | 2026-05-01T11:00:00Z  |
      | originalStart        | 2026-05-01T10:00:00Z  |
      | type                 | 1                     |
      | recurrence.frequency | WEEKLY                |
      | recurrence.count     | 3                     |
      | serverName           | HQ Server             |
    Then the response status code should be 200
    When I retrieve events with the following parameters:
      | from     | 2026-05-01 |
      | to       | 2026-05-20 |
      | timezone | UTC        |
    Then the response list should have size 3
    When I delete the event "{eventId}" with parameters:
      | mode          | SINGLE               |
      | exceptionDate | 2026-05-08T10:00:00Z |
    Then the response status code should be 204
    When I retrieve events with the following parameters:
      | from     | 2026-05-01  |
      | to       | 2026-05-20  |
      | timezone | Europe/Kyiv |
    Then the response list should contain exactly the following state for the series:
      | Expected Title        | Expected Start Time (Kyiv Time) | Expected Server | Status  |
      | Weekly Alpha Training | 2026-05-01T13:00:00+03:00       | HQ Server       | Present |
      | N/A                   | N/A                             | N/A             | Deleted |
      | Weekly Alpha Training | 2026-05-15T13:00:00+03:00       | HQ Server       | Present |
    When I delete the event "{eventId}" with parameters:
      | mode | ALL |
    Then the response status code should be 204
    And I should not be able to retrieve event "{eventId}"

  Scenario: Error cases for calendar events
    Given I log in with user "admin" and password "testpass"

    When I retrieve events with the following parameters:
      | from | 2026-12-31 |
      | to   | 2026-12-01 |
    Then the response status code should be 400

    When I retrieve the nearest event to "1990-01-01T00:00:00Z"
    Then the response status code should be 404

    When I create an event with the following details:
      | title.en |      |
      | start    | 2026-10-27T08:00:00Z |
      | end      | 2026-10-27T09:00:00Z |
    Then the response status code should be 400

    When I update the event "99999" with the following details:
      | mode     | SINGLE               |
      | title.en | Non-existent         |
      | start    | 2026-10-27T08:00:00Z |
      | end      | 2026-10-27T09:00:00Z |
      | type     | 1                    |
    Then the response status code should be 404

    When I delete the event "99999" with parameters:
      | mode | ALL |
    Then the response status code should be 404
