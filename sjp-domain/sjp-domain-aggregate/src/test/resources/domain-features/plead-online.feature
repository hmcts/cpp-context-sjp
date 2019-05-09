Feature: Plead Online

  Scenario: online plea received

    Given case is created

    When you pleadOnline on a CaseAggregate using a plea details

    Then plea-updated, defendant-details-updated, defendant-personal-name-updated, financial-means-updated-by-onlineplea, employer-updated-by-onlineplea, employment-status-updated-by-onlineplea, online-plea-received

  Scenario: Block online plea in the backend when case is in completed state

    Given case is created

    And case is assigned

    When you completeCase on a CaseAggregate

    When you updatePlea on a CaseAggregate using a update plea details

    Then case is unassigned

    Then case is completed

    Then case update rejected because case completed

  Scenario: Block online plea in the backend when case is referred For Court Hearing state

    Given case is created

    When you referCaseForCourtHearing on a CaseAggregate using a court referral details

    Then case is referred for court hearing

    And listing case note is added

    When you updatePlea on a CaseAggregate using a update plea details

    Then case update rejected because case referred to court hearing
