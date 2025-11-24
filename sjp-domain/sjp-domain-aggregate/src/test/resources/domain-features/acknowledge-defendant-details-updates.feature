Feature: Acknowledge defendant details updates

  Scenario: Defendant details updates are acknowledged

    Given case is created
    And defendant details updated
    When you acknowledgeDefendantDetailsUpdates to a CaseAggregate using a acknowledge defendant details updates
    Then defendant details updates acknowledged

    Given no previous events
    When you acknowledgeDefendantDetailsUpdates to a CaseAggregate using a acknowledge defendant details updates
    Then acknowledge defendant details case not found
