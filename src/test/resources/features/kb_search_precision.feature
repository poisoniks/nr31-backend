Feature: Knowledge Base Search Precision Levels
  As a knowledge base user
  I want search results to adapt based on the application's search precision configuration
  So that I get relevant results depending on whether basic, standard, or full precision is active.

  Background:
    Given I log in with user "admin" and password "testpass"

  Scenario Outline: Verify search precision level behavior
    When I update the config "kb_search_precision" with the following payload:
      | name            | kb_search_precision |
      | description.en  | Search Precision |
      | configValue     | "<precision>" |
      | configSchema    | {"type": "string", "enum": ["basic", "standard", "full"]} |
    Then the response status code should be 200
    When I search articles with query "<query>"
    Then the response status code should be 200
    And the response list should have size <expected_size>

    Examples:
      | precision | query    | expected_size |
      # Level 1: Basic Precision (Stems only)
      | basic     | password | 1             |
      | basic     | passwo   | 0             |
      | basic     | passwrod | 0             |
      | basic     | swo      | 0             |
      # Level 2: Standard Precision (Stems + Prefixes + Typos)
      | standard  | password | 1             |
      | standard  | passwo   | 1             |
      | standard  | passwrod | 1             |
      | standard  | swo      | 0             |
      # Level 3: Full Precision (Stems + Prefixes + Typos + Substrings)
      | full      | password | 1             |
      | full      | passwo   | 1             |
      | full      | passwrod | 1             |
      | full      | swo      | 1             |
