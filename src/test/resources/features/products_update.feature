Feature: Update Product

  Scenario: Admin can update a product name
    Given a registered admin user exists
    And the admin is authenticated
    When the admin updates product 1 name to "Updated Product"
    Then the response status code is 200
    And the response contains the product name "Updated Product"

  Scenario: Admin can update a product price
    Given a registered admin user exists
    And the admin is authenticated
    When the admin updates product 1 price to 49.99
    Then the response status code is 200
    And the response contains the product price 49.99

  Scenario: Returns 404 when updating non-existent product
    Given a registered admin user exists
    And the admin is authenticated
    When the admin updates a product with a non-existent ID
    Then the response status code is 404

  Scenario: Regular user cannot update a product
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user attempts to update product 1
    Then the response status code is 403

  Scenario: Unauthenticated user cannot update a product
    When an unauthenticated user attempts to update a product
    Then the response status code is 401
