Feature: Get All Users

  Scenario: Admin can get all users
    Given a registered admin user exists
    And the admin is authenticated
    When the admin requests all users
    Then the response status code is 200
    And the response contains a list of users

  Scenario: Regular user cannot get all users
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user requests all users
    Then the response status code is 403

  Scenario: Unauthenticated user cannot get all users
    When an unauthenticated user requests all users
    Then the response status code is 401
