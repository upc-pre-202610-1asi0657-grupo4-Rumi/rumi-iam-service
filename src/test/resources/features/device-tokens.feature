@US14
Feature: Device tokens for earthquake push notifications
  As a signed-in user of Rumi
  I want to register the Firebase token of my phone (US14 Notificación push de sismo)
  So that Rumi can push earthquake alerts to it

  Background:
    Given an administrator is registered with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"
    And the user is signed in through the gateway with email "ana.torres@rumi.pe" and password "Sismo-Seguro-2026"

  Scenario: A signed-in user registers a device
    When the user registers the device token "fcm-token-of-ana-phone" for platform "ANDROID"
    Then the response status is 201
    And the response describes an "ANDROID" device without its token
    And the user has 1 registered device

  Scenario: Registering the same device again does not duplicate it
    Given the user registered the device token "fcm-token-of-ana-phone" for platform "ANDROID"
    When the user registers the device token "fcm-token-of-ana-phone" for platform "ANDROID"
    Then the response status is 200
    And the response describes an "ANDROID" device without its token
    And the user has 1 registered device

  Scenario: A request that did not come through the gateway is rejected
    When a client registers the device token "fcm-token-of-ana-phone" for platform "ANDROID" without the gateway header
    Then the response status is 401
    And the response is a problem detail with the message "The request has no valid X-User-Id header. Call this endpoint through the API gateway with a valid token."
    And the user has 0 registered devices

  Scenario: An unknown platform is rejected
    When the user registers the device token "fcm-token-of-ana-phone" for platform "BLACKBERRY"
    Then the response status is 400
    And the field "platform" is reported with the message "must be one of ANDROID, IOS, WEB"
    And the user has 0 registered devices
