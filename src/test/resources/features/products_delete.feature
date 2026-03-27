Feature: Delete Product

  Scenario: Admin can delete a product
    Given a registered admin user exists
    And the admin is authenticated
    When the admin deletes product 1
    Then the response status code is 204

  Scenario: Returns 404 when deleting non-existent product
    Given a registered admin user exists
    And the admin is authenticated
    When the admin attempts to delete a product with a non-existent ID
    Then the response status code is 404

  Scenario: Regular user cannot delete a product
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user attempts to delete product 1
    Then the response status code is 403

  Scenario: Unauthenticated user cannot delete a product
    When an unauthenticated user attempts to delete a product
    Then the response status code is 401
