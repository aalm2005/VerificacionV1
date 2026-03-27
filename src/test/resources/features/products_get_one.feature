Feature: Get Single Product

  Scenario: Any user can get a product by ID
    When the user requests the product with ID 1
    Then the response status code is 200
    And the response contains a product with ID 1

  Scenario: Returns 404 for non-existent product
    When the user requests a product with a non-existent ID
    Then the response status code is 404
