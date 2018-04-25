Feature: Ready case

  Scenario: A not ready case is marked ready

    Given case is created
    When you markCaseReadyForDecision on a CaseAggregate using a proved in absence readiness reason
    Then case is marked ready with proved in absence reason

  Scenario: A ready case is marked ready with the same reason

    Given case is created
    And case is marked ready with pleaded guilty reason
    When you markCaseReadyForDecision on a CaseAggregate using a pleaded guilty readiness reason
    Then no events occurred

  Scenario: A ready case is marked ready with different reason

    Given case is created
    And case is marked ready with pleaded guilty reason
    When you markCaseReadyForDecision on a CaseAggregate using a proved in absence readiness reason
    Then case is marked ready with proved in absence reason

  Scenario: A not ready case is unmarked ready

    Given case is created
    When you unmarkCaseReadyForDecision in a CaseAggregate
    Then no events occurred

  Scenario: A ready case is unmarked ready

    Given case is created
    And case is marked ready with pleaded guilty reason
    When you unmarkCaseReadyForDecision in a CaseAggregate
    Then case is unmarked ready