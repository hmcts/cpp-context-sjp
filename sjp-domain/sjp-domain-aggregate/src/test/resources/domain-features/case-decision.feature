Feature: Case Decision

  Scenario: Decision is saved when all offences has final decision

    Given case is created with multiple offences
    And case is marked ready with proved in absence reason
    And case is assigned
    When you saveDecision to a CaseAggregate using a final case decision
    Then decision is saved
    And decision with case note added
    And case is unassigned
    And case is completed
    And case status is completed


  Scenario: Decision is saved when one offence has adjourn decision

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a adjourn case decision
    Then decision is saved with adjourn decision
    And decision with case note added
    And case is unassigned
    And case is adjourned to later sjp hearing
    And decision with adjourn case note added

  Scenario: Decision is saved when all offences referred for court hearing

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a refer for court hearing decision
    Then decision is saved with refer for court hearing decision
    And case is unassigned
    And all case offences are referred for court hearing
    And listing notes are added
    And interpreter language preference for defendant updated
    And hearing language preference for defendant updated with wish to do not speak welsh
    And case is completed
    And case status is referred for court hearing

  Scenario: Decision is saved when some offences are withdrawn and rest are referred for court hearing

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a withdraw and refer for court hearing decisions
    Then decision is saved with withdraw and refer for court hearing decisions
    And case is unassigned
    And single case offence is referred for court hearing
    And listing notes are added
    And case is completed
    And case status is referred for court hearing

  Scenario: Decision is saved when some offences are withdrawn and rest are adjourn

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a withdraw and adjourn
    Then decision is saved with withdraw and adjourn
    And decision with case note added
    And case is unassigned
    And case is adjourned to later sjp hearing
    And decision with adjourn case note added

  Scenario: Decision submitted with adjourned, and resubmitted again with withdraw

    Given case is created with multiple offences
    And case is assigned
    And decision submitted with adjourn case decision
    When you saveDecision to a CaseAggregate using a withdraw final case decision
    Then decision is saved with withdraw final decision
    And withdraw decision with case note added
    And case is unassigned
    And case is completed


  Scenario: Decision is rejected when case is already completed

    Given case is created with multiple offences
    And case is assigned
    And case is completed
    When you saveDecision to a CaseAggregate using a final case decision
    Then decision is rejected with case is already completed

  Scenario: Decision is rejected with case is not assigned to caller

    Given case is created with multiple offences
    When you saveDecision to a CaseAggregate using a final case decision
    Then decision is rejected with case is not assigned to caller


  Scenario: Decision is rejected when offence has multiple decisions

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a offence with more than one decision
    Then decision is rejected with offence has more than one decision


  Scenario: Decision is rejected when offence does not have decision

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a offence with no decision
    Then decision is rejected with offence must have a decision


  Scenario: Decision is rejected when offence already have decision

    Given case is created with multiple offences
    And case is assigned
    And offence already has a decision
    When you saveDecision to a CaseAggregate using a final case decision
    Then decision is rejected with offence already has a decision


  Scenario: Decision is rejected when some offences are adjourned and others are referred for court hearing

    Given case is created with multiple offences
    And case is assigned
    When you saveDecision to a CaseAggregate using a adjourn and refer for court hearing decisions
    Then decision is rejected due to conflicting decisions
