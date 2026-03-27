Feature: Update Cart Item Quantity

  Scenario: User can update quantity of a product in the cart
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user updates the quantity of product 1 in the cart to 3
    Then the response status code is 200
    And the quantity of product 1 in the cart is 3

  Scenario: Returns 404 for non-existent product in cart
    Given a cart has been created
    When the user updates the quantity of product 999999 in the cart to 3
    Then the response status code is 404
