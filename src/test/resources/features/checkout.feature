Feature: Checkout

  Scenario: Authenticated user can initiate checkout
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    And a cart has been created
    And product with ID 1 has been added to the cart
    When the user sends a checkout request with their cart ID
    Then the response status code is 200
    And the response contains a Stripe checkout URL

  Scenario: Unauthenticated user cannot checkout
    Given a cart has been created
    When an unauthenticated user sends a checkout request
    Then the response status code is 401

  Scenario: Checkout fails with non-existent cart
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user sends a checkout request with a non-existent cart ID
    Then the response status code is 404

  Scenario: Stripe webhook updates order status to paid
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    And a cart has been created
    And product with ID 1 has been added to the cart
    And the user has completed a checkout
    When Stripe sends a successful payment webhook event
    Then the response status code is 200
    And the order status is updated to paid
