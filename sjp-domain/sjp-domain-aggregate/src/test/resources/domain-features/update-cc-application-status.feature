Feature: Criminal Court  Application Status Update

 Scenario: CC Application Status is Updated to completed case with Statutory Declaration Pending

   Given case is created
   And case is completed
   When you updateCaseApplicationStatus to a CaseAggregate using a  statutory declaration pending application status update
   Then cc application status created with statutory declaration pending

 Scenario: CC Application Status is Updated to completed case with Statutory Declaration Granted

   Given case is created
   And case is completed
   When you updateCaseApplicationStatus to a CaseAggregate using a  statutory declaration granted application status update
   Then cc application status updated with statutory declaration granted
   And case status is relisted

  Scenario: CC Application Status is Updated to completed case with Reopening Granted

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  reopening granted application status update
    Then cc application status updated with reopening granted
    And case status is relisted


  Scenario: CC Application Status is Updated to completed case with Appeal Allowed

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  appeal allowed application status update
    Then cc application status updated with appeal allowed
    And case status is appealed

  Scenario: CC Application Status is Updated to completed case with Statutory Declaration Refused

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  statutory declaration refused application status update
    Then cc application status updated with statutory declaration refused

  Scenario: CC Application Status is Updated to completed case with Statutory Declaration Withdrawn

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  statutory declaration withdrawn application status update
    Then cc application status updated with statutory declaration withdrawn

  Scenario: CC Application Status is Updated to completed case with Reopening Refused

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  reopening refused application status update
    Then cc application status updated with reopening refused

  Scenario: CC Application Status is Updated to completed case with Reopening Withdrawn

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  reopening withdrawn application status update
    Then cc application status updated with reopening withdrawn

  Scenario: CC Application Status is Updated to completed case with Appeal Withdrawn

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  appeal withdrawn application status update
    Then cc application status updated with appeal withdrawn

  Scenario: CC Application Status is Updated to completed case with Appeal Dismissed

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  appeal dismissed application status update
    Then cc application status updated with appeal dismissed

  Scenario: CC Application Status is Updated to completed case with Appeal Abandoned

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  appeal abandoned application status update
    Then cc application status updated with appeal abandoned

  Scenario: CC Application Status is Updated to completed case with Application Dismissed Sentence Varied

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  application dismissed sentence varied application status update
    Then cc application status updated with application dismissed sentence varied

  Scenario: CC Application Status is Updated to completed case with Application Status Not Known

    Given case is created
    And case is completed
    When you updateCaseApplicationStatus to a CaseAggregate using a  application status not known application status update
    Then cc application status updated with application status not known
