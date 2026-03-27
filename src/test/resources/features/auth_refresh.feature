Feature: User Authentication - Token Refresh

  Scenario: Successful token refresh with valid refresh token
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user sends a refresh token request
    Then the response status code is 200
    And the response contains a new access token

  Scenario: Token refresh fails with invalid token
    When the user sends a refresh request with an invalid token
    Then the response status code is 401

  Scenario: Token refresh fails with no token
    When the user sends a refresh request with no token
    Then the response status code is 401

  Scenario: Token refresh fails with expired token
    Given a registered user exists with email "harry@gmail.com"
    When the user sends a refresh request with an expired token
    Then the response status code is 401
