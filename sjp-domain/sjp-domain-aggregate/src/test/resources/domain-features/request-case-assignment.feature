Feature: Requesting case assignment

  Scenario: Case assignment is requested by owner of started session

    Given session is started
    When you requestCaseAssignment on a Session using a session identifier
    Then assignment is allowed

  Scenario: Case assignment is requested by owner of ended session

    Given session is started
    And session is ended
    When you requestCaseAssignment on a Session using a session identifier
    Then assignment is rejected because session is ended

  Scenario: Case assignment is requested for non existing session

    Given no previous events
    When you requestCaseAssignment on a Session using a session identifier
    Then assignment is rejected because session does not exist

  Scenario: Case assignment is requested not by owner of started session

    Given session is started by other user
    When you requestCaseAssignment on a Session using a session identifier
    Then assignment is rejected because session is not owned by user
