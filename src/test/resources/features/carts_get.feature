Feature: Get Cart

  Scenario: User can retrieve their cart
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user requests their cart
    Then the response status code is 200
    And the cart total price is greater than 0

  Scenario: Returns 404 for non-existent cart
    When the user requests a cart with a non-existent ID
    Then the response status code is 404

  Scenario: Cart total price equals sum of all items
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user requests their cart
    Then the response status code is 200
    And the cart total price equals the sum of all item prices
