Feature: Create Cart

  Scenario: Visitor can create a cart
    When the visitor creates a new cart
    Then the response status code is 201
    And the response contains a cart ID

  Scenario: Authenticated user can create a cart
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user creates a new cart
    Then the response status code is 201
    And the response contains a cart ID

  Scenario: Each cart creation returns a unique ID
    When the visitor creates a new cart
    And the visitor creates another new cart
    Then both cart IDs are different
