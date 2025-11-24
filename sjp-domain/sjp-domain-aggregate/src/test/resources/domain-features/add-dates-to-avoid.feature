Feature: Add dates to avoid

  Scenario: Dates to avoid is added to a case

    Given case is created
    When you addDatesToAvoid to a CaseAggregate using a dates to avoid details
    Then dates to avoid received

  Scenario: Dates to avoid not required when they were previously added

    Given case is created
    And dates to avoid received
    When you setPleas on a CaseAggregate using a set pleas details
    Then plea set
    And interpreter for defendant updated
    And hearing language preference for defendant updated with wish to do not speak welsh
    And trial requested
    And pleaded not guilty
    And case is marked ready with pleaded not guilty reason
    And case status is plea received ready for decision

