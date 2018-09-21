Feature: Requesting withdrawal all offences

  Scenario: all offences withdrawal requested

    Given case is created
    When you requestWithdrawalAllOffences on a CaseAggregate
    Then all offences withdrawal requested

  Scenario: When case is assigned

    Given case is created
    And case is assigned
    When you requestWithdrawalAllOffences on a CaseAggregate
    Then case update rejected because case assigned

  Scenario: When case is completed

    Given case is created
    And case is completed
    When you requestWithdrawalAllOffences on a CaseAggregate
    Then case update rejected because case completed
