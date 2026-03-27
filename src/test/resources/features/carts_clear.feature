Feature: Clear Cart

  Scenario: User can clear all items from the cart
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user clears the cart
    Then the response status code is 204
    And the cart contains 0 item(s)

  Scenario: Returns 404 when clearing a non-existent cart
    When the user clears a non-existent cart
    Then the response status code is 404
