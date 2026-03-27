Feature: Create Product

  Scenario: Admin can create a product
    Given a registered admin user exists
    And the admin is authenticated
    When the admin creates a product with name "New Product" and price 29.99
    Then the response status code is 201
    And the response contains the product name "New Product"

  Scenario: Create product fails with missing name
    Given a registered admin user exists
    And the admin is authenticated
    When the admin creates a product with name "" and price 29.99
    Then the response status code is 400

  Scenario: Regular user cannot create a product
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user attempts to create a product
    Then the response status code is 403

  Scenario: Unauthenticated user cannot create a product
    When an unauthenticated user attempts to create a product
    Then the response status code is 401
