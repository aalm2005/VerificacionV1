Feature: User Registration

  Scenario: Successful user registration
    When the user registers with name "Harry Potter" email "harry@gmail.com" and password "123456"
    Then the response status code is 201
    And the response contains the name "Harry Potter"
    And the response contains the email "harry@gmail.com"

  Scenario: Registration fails with duplicate email
    Given a registered user exists with email "harry@gmail.com"
    When the user registers with name "Harry Potter" email "harry@gmail.com" and password "123456"
    Then the response status code is 409

  Scenario: Registration fails with missing name
    When the user registers with name "" email "harry@gmail.com" and password "123456"
    Then the response status code is 400

  Scenario: Registration fails with missing email
    When the user registers with name "Harry Potter" email "" and password "123456"
    Then the response status code is 400

  Scenario: Registration fails with missing password
    When the user registers with name "Harry Potter" email "harry@gmail.com" and password ""
    Then the response status code is 400
