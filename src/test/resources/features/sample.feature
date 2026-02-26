Feature: Sample Cucumber Test
  To verify the cucumber infrastructure is set up properly with Spring configuration.

  Scenario: Application context loads and dependencies are injected
    Given the application environment is ready
    When a sample cucumber action is performed
    Then the result of the test should be successful
