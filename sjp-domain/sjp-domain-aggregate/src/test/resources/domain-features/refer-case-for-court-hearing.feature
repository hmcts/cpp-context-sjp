Feature: Refer case for court hearing

  Scenario: Case is referred for court hearing

    Given case is created
    And case is assigned
    When you referCaseForCourtHearing on a CaseAggregate using a court referral details
    Then case is referred for court hearing

  Scenario: Already reffered case is again referred for court hearing

    Given case is created
    And case is assigned
    And case is referred for court hearing
    And case is unassigned
    And case is completed
    When you referCaseForCourtHearing on a CaseAggregate using a court referral details
    Then case referral rejected for already referred case

  Scenario: Non existing case is referred for court hearing

    Given no previous events
    When you referCaseForCourtHearing on a CaseAggregate using a court referral details
    Then case referral rejected for non existing case

  Scenario: Court referral rejection for non-existing case

    Given no previous events
    When you recordCaseReferralForCourtHearingRejection on a CaseAggregate using a record court referral rejection
    Then rejection for non existing case created on record referral rejection request

  Scenario: Case court hearing referral is rejected

    Given case is created
    And case is assigned
    And case is referred for court hearing
    When you recordCaseReferralForCourtHearingRejection on a CaseAggregate using a record court referral rejection
    Then case referral for court hearing rejection recorded
