@US03
Feature: Password recovery
  As a user of Rumi who forgot the password
  I want to receive a single-use link (US03 Recuperar contraseña)
  So that I can set a new password and sign in again

  Background:
    Given an administrator is registered with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"

  Scenario: A registered administrator asks for a reset link
    When the user asks for a password reset link for "Ana.Torres@rumi.pe"
    Then the response status is 202
    And the response body is empty
    And a reset link is sent to "ana.torres@rumi.pe"
    And only the hash of the link is stored

  Scenario: An unknown email gets the same answer and no link is sent
    When the user asks for a password reset link for "nobody@rumi.pe"
    Then the response status is 202
    And the response body is empty
    And no reset link is sent

  Scenario: The user resets the password with the link and signs in with the new one
    Given the user asked for a password reset link for "ana.torres@rumi.pe"
    When the user resets the password to "Nueva-Clave-Sismo-2026" with the link
    Then the response status is 204
    And the response does not contain the link or the password
    When the user signs in with email "ana.torres@rumi.pe" and password "Nueva-Clave-Sismo-2026"
    Then the response status is 200
    When the user signs in with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"
    Then the response status is 401

  Scenario: A link works only once
    Given the user asked for a password reset link for "ana.torres@rumi.pe"
    And the user reset the password to "Nueva-Clave-Sismo-2026" with the link
    When the user resets the password to "Otra-Clave-Sismo-2026" with the link
    Then the response status is 400
    And the response is a problem detail with the message "Invalid or expired token."
    And the response does not contain the link or the password

  Scenario: A link expires after 30 minutes
    Given the user asked for a password reset link for "ana.torres@rumi.pe"
    And 31 minutes have passed
    When the user resets the password to "Nueva-Clave-Sismo-2026" with the link
    Then the response status is 400
    And the response is a problem detail with the message "Invalid or expired token."

  Scenario: A weak new password is rejected and the link still works afterwards
    Given the user asked for a password reset link for "ana.torres@rumi.pe"
    When the user resets the password to "short" with the link
    Then the response status is 400
    And the field "newPassword" is reported with the message "Password must have at least 8 characters and at most 72 bytes"
    When the user resets the password to "Nueva-Clave-Sismo-2026" with the link
    Then the response status is 204
