Feature: Plead Online

  Scenario: online plea received

    Given case is created

    When you pleadOnline on a CaseAggregate using a plea details

    Then pleas is set
    And hearing language preference for defendant is updated
    And pleaded guilty
    And defendant details updated
    And defendant date of birth update requested
    And defendant personal name update requested
    And defendant detail update requested
    And financial means updated
    And employer is updated
    And employment status updated
    And online plea received

  Scenario: Block online plea in the backend when case is in completed state

    Given case is created

    And case is completed

    When you setPleas on a CaseAggregate using a set pleas details

    Then case update rejected because case completed

  Scenario: Block online plea in the backend when case is referred For Court Hearing state

    Given case is created

    And all case offences are referred for court hearing

    When you setPleas on a CaseAggregate using a set pleas details

    And case update rejected because case referred to court hearing
