Feature: Compliting case

  Scenario: Case is completed

    Given case is created
    And case is assigned
    When you completeCase on a CaseAggregate
    Then case is unassigned
    And case is completed

  Scenario: Case is already completed

    Given case is created
    And case is assigned
    And case is completed
    When you completeCase on a CaseAggregate
    Then case completion is rejected
