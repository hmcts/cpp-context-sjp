Feature: Requesting case assignment

  Scenario: Case assignment is requested by owner of started session

    Given session is started
    When you requestCaseAssignment on a Session using a user identifier
    Then assignment is allowed

  Scenario: Case assignment is requested by owner of ended session

    Given session is started
    And session is ended
    When you requestCaseAssignment on a Session using a user identifier
    Then assignment is rejected because session is ended

  Scenario: Case assignment is requested for non existing session

    Given no previous events
    When you requestCaseAssignment on a Session using a user identifier
    Then assignment is rejected because session does not exist

  Scenario: Case assignment is requested not by owner of started session

    Given session is started by other user
    When you requestCaseAssignment on a Session using a user identifier
    Then assignment is rejected because session is not owned by user

  Scenario: Case assignment is rejected because case is already assigned to other user

    Given case is created
    And case is assigned to other user
    When you assignCase on a CaseAggregate using an assignee and assignment type
    Then assignment is rejected because case is already assigned

  Scenario: Case is assigned to user

    Given case is created
    When you assignCase on a CaseAggregate using an assignee and assignment type
    Then case is assigned

  Scenario: Case is already assigned to user

    Given case is created
    And case is assigned
    When you assignCase on a CaseAggregate using an assignee and assignment type
    Then case remains assigned

  Scenario: Case unassignment is rejected

    Given case is created
    When you unassignCase on a CaseAggregate
    Then unassignment is rejected

  Scenario: Case is unassigned

    Given case is created
    And case is assigned
    When you unassignCase on a CaseAggregate
    Then case is unassigned

  Scenario: Case is assigned with old version of event which contains session id and does not contain assignedAt - backward compatibility

    Given case is created
    And case is assigned with old version of event with session id
    When you assignCase on a CaseAggregate using an assignee and assignment type
    Then case remains assigned