Feature: Get Single User

  Scenario: User can get their own profile by ID
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user requests their own user by ID
    Then the response status code is 200
    And the response contains the user email "harry@gmail.com"

  Scenario: Admin can get any user by ID
    Given a registered user exists with email "harry@gmail.com"
    And a registered admin user exists
    And the admin is authenticated
    When the admin requests a user by a valid ID
    Then the response status code is 200

  Scenario: Returns 404 for non-existent user
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user requests a user with a non-existent ID
    Then the response status code is 404

  Scenario: Unauthenticated user cannot get user by ID
    When an unauthenticated user requests a user by ID
    Then the response status code is 401
