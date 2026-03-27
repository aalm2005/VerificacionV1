Feature: Change User Password

  Scenario: User can change their own password
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user changes their password from "123456" to "newpassword123"
    Then the response status code is 200

  Scenario: Change password fails with wrong old password
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user changes their password from "wrongpassword" to "newpassword123"
    Then the response status code is 400

  Scenario: Change password fails with empty new password
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user changes their password with an empty new password
    Then the response status code is 400

  Scenario: Change password fails with short new password
    Given a registered user exists with email "harry@gmail.com"
    And the user is authenticated
    When the user changes their password to "short"
    Then the response status code is 400

  Scenario: Unauthenticated user cannot change password
    When an unauthenticated user attempts to change a password
    Then the response status code is 401
