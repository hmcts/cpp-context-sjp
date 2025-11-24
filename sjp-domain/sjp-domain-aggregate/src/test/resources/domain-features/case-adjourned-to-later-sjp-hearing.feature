Feature: Case adjourned to later sjp hearing

  Scenario: Case adjournment is recorded

    Given case is created
    When you recordCaseAdjournedToLaterSjpHearing to a CaseAggregate using a record case adjourned to later sjp hearing
    Then case adjourned to later sjp hearing recorded
    And case is unassigned

  Scenario: Case adjournment elpased

    Given case is created
    And case adjourned to later sjp hearing recorded
    When you recordCaseAdjournmentToLaterSjpHearingElapsed to a CaseAggregate using a record case adjournment to later sjp hearing elapsed
    Then case adjournment to later sjp hearing elapsed
