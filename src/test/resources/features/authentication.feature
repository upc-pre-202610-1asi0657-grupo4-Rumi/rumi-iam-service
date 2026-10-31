@US01 @US02
Feature: Administrator sign-up and sign-in
  As the administrator of a building
  I want to register in Rumi (US01 Registro de administrador) and sign in (US02 Inicio de sesión)
  So that I get a token that the API gateway accepts

  @US01
  Scenario: An administrator signs up
    When a visitor signs up with email "ana.torres@rumi.pe", password "Sismo-Seguro-2026" and full name "Ana Torres"
    Then the response status is 201
    And the response has the role "ADMINISTRATOR"
    And the response does not contain the password
    And the password of "ana.torres@rumi.pe" is stored only as a BCrypt hash

  @US01
  Scenario: An email can only be registered once, whatever its case
    Given an administrator is registered with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"
    When a visitor signs up with email "ANA.TORRES@rumi.pe", password "Otra-Clave-2026" and full name "Ana T."
    Then the response status is 409
    And the response is a problem detail with the message "An account with this email already exists."

  @US01
  Scenario Outline: Invalid sign-up data is rejected
    When a visitor signs up with email "<email>", password "<password>" and full name "Ana Torres"
    Then the response status is 400
    And the field "<field>" is reported with the message "<message>"

    Examples:
      | email              | password          | field    | message                                                       |
      | not-an-email       | Sismo-Seguro-2026 | email    | must be a well-formed email address                           |
      | ana.torres@rumi.pe | short             | password | Password must have at least 8 characters and at most 72 bytes |
      | ana.torres@rumi.pe | ññññ              | password | Password must have at least 8 characters and at most 72 bytes |

  @US02
  Scenario: A registered administrator signs in and gets a token the gateway accepts
    Given an administrator is registered with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"
    When the user signs in with email "Ana.Torres@rumi.pe" and password "Sismo-Seguro-2026"
    Then the response status is 200
    And the response contains a bearer token
    And the API gateway accepts the token with the role "ADMINISTRATOR" and no buildings

  @US02
  Scenario Outline: Wrong credentials are rejected without telling which one is wrong
    Given an administrator is registered with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"
    When the user signs in with email "<email>" and password "<password>"
    Then the response status is 401
    And the response is a problem detail with the message "Invalid email or password."

    Examples:
      | email              | password          |
      | ana.torres@rumi.pe | wrong-password    |
      | nobody@rumi.pe     | Sismo-Seguro-2026 |
