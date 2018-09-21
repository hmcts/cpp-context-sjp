Feature: Plead Online

  Scenario: online plea received

    Given case is created

    When you pleadOnline on a CaseAggregate using a plea details

    Then plea-updated, defendant-details-updated, defendant-personal-name-updated, financial-means-updated-by-onlineplea, employer-updated-by-onlineplea, employment-status-updated-by-onlineplea, online-plea-received
