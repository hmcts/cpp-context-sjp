Feature: Enforcement statdecs or reopening pending application notification
  
  Scenario: Mark Application notification email attachment as generated

    Given case is created
    When you markAsGenerated to a EnforcementPendingApplicationNotificationAggregate using a command enforcement pending application generate notification
    Then event enforcement pending application notification generated

  Scenario: Mark Application notification email attachment as generation failed

    Given case is created
    When you markAsGenerationFailed to a EnforcementPendingApplicationNotificationAggregate using a command enforcement pending application fail generation notification
    Then event enforcement pending application notification generation failed

  Scenario: Mark Application email notification as queued

    Given case is created
    When you markAsNotificationQueued to a EnforcementPendingApplicationNotificationAggregate using a command enforcement pending application queue notification
    Then event enforcement pending application notification queued

  Scenario: Mark Application email notification as sent

    Given case is created
    When you markAsNotificationSent to a EnforcementPendingApplicationNotificationAggregate using a command enforcement pending application send notification
    Then event enforcement pending application notification sent

  Scenario: Mark Application email notification failed

    Given case is created
    When you markAsNotificationFailed to a EnforcementPendingApplicationNotificationAggregate using a command enforcement pending application failed notification
    Then event enforcement pending application notification failed
