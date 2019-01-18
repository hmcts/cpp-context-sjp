Feature: Case adjourned to later sjp hearing

  Scenario: Case adjournment is recorded

    Given case is created
    When you recordCaseAdjournedToLaterSjpHearing to a CaseAggregate using a record case adjourned to later sjp hearing
    Then case adjourned to later sjp hearing recorded