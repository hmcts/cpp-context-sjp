Feature: Cancel requesting withdrawal all offences

  Scenario: When all offences withdrawal requested

    Given case is created
    And all offences withdrawal requested
    When you cancelRequestWithdrawalAllOffences on a CaseAggregate
    Then all offences withdrawal request cancelled

  Scenario: When all offences withdrawal NOT requested

    Given case is created
    When you cancelRequestWithdrawalAllOffences on a CaseAggregate
    Then no events occurred

  Scenario: When case is assigned

    Given case is created
    And case is assigned
    When you cancelRequestWithdrawalAllOffences on a CaseAggregate
    Then case update rejected because case assigned

  Scenario: When case is completed

    Given case is created
    And case is completed
    When you cancelRequestWithdrawalAllOffences on a CaseAggregate
    Then case update rejected because case completed
