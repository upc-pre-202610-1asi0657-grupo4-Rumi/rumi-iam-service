@US04
Feature: Building membership and token reissue
  As the administrator of a building
  I want IAM to add the building I register to my account and give me a new token
  So that the services let me manage that building through the API gateway

  Background:
    Given the administrator "ana.torres@rumi.pe" with password "Sismo-Seguro-2026" has signed in

  Scenario: The administrator gets a new token that carries the building it registered
    When Building Management reports that the administrator registered the building "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"
    And the user asks for a new token
    Then the response status is 200
    And the response lists the building "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"
    And the API gateway accepts the new token with the role "ADMINISTRATOR" and the building "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"
    And the new token keeps the time of the password sign-in

  Scenario: The same BuildingRegistered event delivered twice is applied once
    When Building Management reports that the administrator registered the building "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"
    And the same BuildingRegistered event is delivered again
    Then the administrator belongs only to the building "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"
    And the event is recorded once as processed

  Scenario: A token cannot be reissued more than 12 hours after the password sign-in
    When the user renews the token every 10 minutes for 12 hours
    And 1 more minute passes and the user asks for a new token
    Then the response status is 401
    And the response is a problem detail with the message "The session started more than 12 hours ago; sign in again"
