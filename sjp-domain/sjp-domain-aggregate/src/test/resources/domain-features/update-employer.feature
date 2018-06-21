Feature: Update employer

  Scenario: Case not found

    Given no previous events
    When you updateEmployer to a CaseAggregate using a employer details
    Then case not found

  Scenario: Employer added

    Given case is created
    When you updateEmployer to a CaseAggregate using a employer details
    Then employer updated
    And employment status updated

  Scenario: Employer updated

    Given case is created
    And employer updated
    When you updateEmployer to a CaseAggregate using a employer details
    Then employer updated

  Scenario: Employer updated when case is assigned to user

    Given case is created
    And case is assigned
    When you updateEmployer to a CaseAggregate using a employer details
    Then employer updated
    And employment status updated

  Scenario: Case update rejected when case assigned to another user

    Given case is created
    And case is assigned to another user
    When you updateEmployer to a CaseAggregate using a employer details
    Then case update rejected because case assigned

  Scenario: Case update rejected when case is already completed

    Given case is created
    And case is completed
    When you updateEmployer to a CaseAggregate using a employer details
    Then case update rejected because case completed