Feature: Get All Products

  Scenario: Any user can get all products
    When the user requests all products
    Then the response status code is 200
    And the response contains a list of products

  Scenario: Returns 10 seeded products
    When the user requests all products
    Then the response status code is 200
    And the response contains 10 products
