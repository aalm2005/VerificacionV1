Feature: Remove Item from Cart

  Scenario: User can remove a product from the cart
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user removes product 1 from the cart
    Then the response status code is 204
    And the cart does not contain product 1

  Scenario: Returns 404 for non-existent cart
    When the user removes product 1 from a non-existent cart
    Then the response status code is 404
