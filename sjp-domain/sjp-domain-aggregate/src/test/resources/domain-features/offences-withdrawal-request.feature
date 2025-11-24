Feature: Request withdrawal of offences

  Scenario: Request offence withdrawal

    Given case is created
    When you requestForOffenceWithdrawal on a CaseAggregate using a offence withdrawal request with reason
    Then withdrawal request status is set for the requested offence
    And withdrawal is requested for the offences

  Scenario: Request for offence withdrawal cancellation

      Given case is created
      And withdrawal is requested for the offences
      When you requestForOffenceWithdrawal on a CaseAggregate using a cancel offence withdrawal request
      Then withdrawal request status is set
      And request for withdrawal is cancelled

  Scenario: Request for offence withdrawal with new reason

      Given case is created
      And withdrawal is requested for the offences
      When you requestForOffenceWithdrawal on a CaseAggregate using a offence withdrawal request with different reason
      Then withdrawal request status is set for offence with new withdrawal reason
      And offence withdrawal request has different reason