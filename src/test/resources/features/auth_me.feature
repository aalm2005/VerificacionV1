Feature: User Authentication - Get Current User

  Scenario: Authenticated user can get their own profile
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user requests their own profile
    Then the response status code is 200
    And the response contains the user email "harry@gmail.com"

  Scenario: Unauthenticated user cannot get profile
    When an unauthenticated user requests the profile endpoint
    Then the response status code is 401
