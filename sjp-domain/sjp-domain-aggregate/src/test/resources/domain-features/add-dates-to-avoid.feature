Feature: Receive a case

  Scenario: Dates to avoid is added to a case

    Given case is created
    When you addDatesToAvoid to a CaseAggregate using a dates to avoid details
    Then dates to avoid received

