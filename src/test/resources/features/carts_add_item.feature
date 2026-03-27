Feature: Add Item to Cart

  Scenario: User can add a product to the cart
    Given a cart has been created
    And a product exists with ID 1
    When the user adds product with ID 1 to the cart
    Then the response status code is 200
    And the cart contains 1 item(s)

  Scenario: Adding duplicate product increases quantity
    Given a cart has been created
    And a product exists with ID 1
    And product with ID 1 has been added to the cart
    When the user adds product with ID 1 to the cart again
    Then the response status code is 200
    And the cart contains 1 item(s)

  Scenario: Returns 404 for non-existent product
    Given a cart has been created
    When the user adds a product with a non-existent ID to the cart
    Then the response status code is 404

  Scenario: Returns 404 for non-existent cart
    When the user adds product with ID 1 to a non-existent cart
    Then the response status code is 404
