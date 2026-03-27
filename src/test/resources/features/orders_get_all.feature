Feature: Get All Orders

  Scenario: Authenticated user can get their orders
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    And the user has a completed order
    When the user requests all their orders
    Then the response status code is 200
    And the response contains at least 1 order

  Scenario: Unauthenticated user cannot get orders
    When an unauthenticated user requests all orders
    Then the response status code is 401

  Scenario: Admin can get all orders
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    And the user has a completed order
    And a registered admin user exists
    And the admin is authenticated
    When the admin requests all orders
    Then the response status code is 200
    And the response contains at least 1 order
