Feature: Set pleas

  Scenario: Not guilty plea raises dates to avoid required event

    Given case is created

    When you setPleas on a CaseAggregate using a set pleas details

    Then plea set

    And interpreter for defendant updated

    And hearing language preference for defendant updated with wish to do not speak welsh

    And trial requested

    And pleaded not guilty

    And dates to avoid required

    And case expected date ready changed to dates to avoid required date

    And case status is plea received