Feature: User Authentication - Login

  Scenario: Successful login with valid credentials
    Given a registered user exists with email "harry@gmail.com"
    When the user logs in with email "harry@gmail.com" and password "123456"
    Then the response status code is 200
    And the response contains an access token
    And the response contains a refresh token

  Scenario: Login fails with wrong password
    Given a registered user exists with email "harry@gmail.com"
    When the user logs in with email "harry@gmail.com" and password "wrongpassword"
    Then the response status code is 401

  Scenario: Login fails with non-existent email
    When the user logs in with email "nonexistent@gmail.com" and password "123456"
    Then the response status code is 401

  Scenario: Login fails with empty credentials
    When the user logs in with email "" and password ""
    Then the response status code is 400
