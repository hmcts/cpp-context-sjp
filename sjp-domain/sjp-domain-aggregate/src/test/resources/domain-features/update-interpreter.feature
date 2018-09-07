Feature: Interpreter

  Scenario: its update when defendant wishes to have an interpreter

    Given case is created
    When you updateInterpreter on a CaseAggregate using a interpreter details
    Then interpreter for defendant updated with wish to have an interpreter
    When you updateInterpreter on a CaseAggregate using an interpreter details without language
    Then interpreter for defendant cancelled

  Scenario: its update when case assigned to himself

    Given case is created
    And case is assigned
    When you updateInterpreter on a CaseAggregate using a interpreter details with assignee user id
    Then interpreter for defendant updated with wish to have an interpreter

  Scenario: its reject update when case is assigned to some other user

    Given case is created
    And case is assigned
    When you updateInterpreter on a CaseAggregate using a interpreter details
    Then case update rejected because case assigned

  Scenario: its reject update when case is completed

    Given case is created
    And case is completed
    When you updateInterpreter on a CaseAggregate using a interpreter details
    Then case update rejected because case completed

  Scenario: its reject update when already updated

    Given case is created
     When you updateInterpreter on a CaseAggregate using a interpreter details
    Then interpreter for defendant updated with wish to have an interpreter
     When you updateInterpreter on a CaseAggregate using a interpreter details
     Then no events occurred
