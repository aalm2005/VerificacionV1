Feature: Update User

  Scenario: User can update their own name
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user updates their name to "Harry Updated"
    Then the response status code is 200
    And the response contains the name "Harry Updated"

  Scenario: User can update their own email
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user updates their email to "harry.updated@gmail.com"
    Then the response status code is 200
    And the response contains the email "harry.updated@gmail.com"

  Scenario: Update fails with duplicate email
    Given a registered user exists with email "harry@gmail.com"
    And another user exists with email "ron@gmail.com"
    And the user is authenticated
    When the user updates their email to "ron@gmail.com"
    Then the response status code is 409

  Scenario: Unauthenticated user cannot update
    When an unauthenticated user sends an update request
    Then the response status code is 401
