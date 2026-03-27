Feature: Get Single Order

  Scenario: Authenticated user can get their order by ID
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    And the user has a completed order
    When the user requests their order by ID
    Then the response status code is 200
    And the response contains the order details

  Scenario: Returns 404 for non-existent order
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user requests an order with a non-existent ID
    Then the response status code is 404

  Scenario: Unauthenticated user cannot get order by ID
    When an unauthenticated user requests an order by ID
    Then the response status code is 401
