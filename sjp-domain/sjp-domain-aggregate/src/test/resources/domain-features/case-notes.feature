Feature: Case notes

  Scenario: Case note added

    Given case is created
    When you addCaseNote to a CaseAggregate using a case note
    Then case note added
