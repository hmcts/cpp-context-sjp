Feature: Update hearing requirements

  Scenario: Hearing Requirements are updated

    Given case is created
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements empty
    Then no events occurred

  Scenario: When the Defendant wishes to speak Welsh

    Given case is created
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements where the defendant wants to speak in welsh
    Then hearing language preference for defendant updated with wish to speak welsh
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements where the defendant does not want to speak in welsh
    Then hearing language preference for defendant updated with wish to do not speak welsh
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements empty
    Then hearing language preference for defendant cancelled

  Scenario: When the Defendant wishes to have an interpreter

    Given case is created
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements where the defendant wants an interpreter
    Then interpreter for defendant updated
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements where the defendant does not want an interpreter
    Then interpreter for defendant cancelled
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements empty
    Then no events occurred

  Scenario: When case is assigned to himself

    Given case is created
     And case is assigned
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements with assignee user id
    Then interpreter for defendant updated
     And hearing language preference for defendant updated with wish to do not speak welsh

  Scenario: When case isn hearing language preference for defendant updated with wish to speak welsh assigned to some other user

    Given case is created
    And case is assigned
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements
    Then case update rejected because case assigned

  Scenario: When case is completed

    Given case is created
     And case is completed
    When you updateHearingRequirements on a CaseAggregate using a hearing requirements
    Then case update rejected because case completed