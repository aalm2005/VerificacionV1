Feature: Delete User

  Scenario: Admin can delete a user
    Given a registered user exists with email "harry@gmail.com"
    And a registered admin user exists
    And the admin is authenticated
    When the admin deletes the user by ID
    Then the response status code is 204

  Scenario: Admin returns 404 when deleting non-existent user
    Given a registered admin user exists
    And the admin is authenticated
    When the admin attempts to delete a user with a non-existent ID
    Then the response status code is 404

  Scenario: Regular user cannot delete another user
    Given a registered user exists with email "harry@gmail.com"
    And a registered admin user exists
    And the user is authenticated
    When the user attempts to delete another user
    Then the response status code is 403

  Scenario: Unauthenticated user cannot delete a user
    When an unauthenticated user attempts to delete a user
    Then the response status code is 401
