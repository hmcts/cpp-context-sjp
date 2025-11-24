Feature: Case sent for listing update

  Scenario: Non SJP case is sent for listing update

    Given no previous events
    When you updateCaseListedInCriminalCourts on a CaseAggregate using an update case listing
    Then no events occurred
