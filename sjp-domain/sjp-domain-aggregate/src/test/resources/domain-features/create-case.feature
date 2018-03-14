Feature: Receive a case

  Scenario: A dummy

    Given no previous events
    When you dummy to a CaseAggregate
    Then no events occurred

#  Scenario: A case is received with correct details
#
#    Given no previous events
#    When you receiveCase to a CaseAggregate using a new case details
#    Then new case is created

#  Scenario: A case is received but it already has URN
#
#    Given no previous events
#    When you receiveCase to a CaseAggregate using an already existing case details and createdOnDate
#    Then case creation failed because case already existed

