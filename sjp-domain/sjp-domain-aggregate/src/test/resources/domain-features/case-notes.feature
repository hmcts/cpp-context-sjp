Feature: Case notes

  Scenario: Case note added

    Given case is created
    When you addCaseNote to a CaseAggregate using a case note
    Then case note added

  Scenario: Case note added to completed case

    Given case is created
    And case is assigned
    And case is completed
    When you addCaseNote to a CaseAggregate using a case note
    Then case note added

  Scenario: Case note rejected if case referred for court hearing

    Given case is created
    And case is assigned
    And case is referred for court hearing
    When you addCaseNote to a CaseAggregate using a case note
    Then case note rejected
