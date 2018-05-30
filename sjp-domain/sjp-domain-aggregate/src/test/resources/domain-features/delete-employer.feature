Feature: Delete employer

  Scenario: Employer deleted

    Given case is created
    And employer updated
    And employment status updated
    When you deleteEmployer on a CaseAggregate using a user id and defendant id
    Then employer deleted

  Scenario: When defendant not employed

    Given case is created
    When you deleteEmployer on a CaseAggregate using a user id and defendant id
    Then defendant not employed

  Scenario: When case is assigned to you

    Given case is created
    And employer updated
    And employment status updated
    And case is assigned
    When you deleteEmployer on a CaseAggregate using a user id and defendant id with assignee user id
    Then employer deleted

  Scenario: When case is assigned to some other user

    Given case is created
    And case is assigned
    When you deleteEmployer on a CaseAggregate using a user id and defendant id
    Then case update rejected because case assigned

  Scenario: When case is completed

    Given case is created
    And case is completed
    When you deleteEmployer on a CaseAggregate using a user id and defendant id
    Then case update rejected because case completed
