Feature: Update interpreter

  Scenario: Interpreter updated

     Given case is created
     When you updateInterpreter on a CaseAggregate using a interpreter details
     Then interpreter for defendant updated

  Scenario: When case is assigned to you

     Given case is created
     And case is assigned
     When you updateInterpreter on a CaseAggregate using a interpreter details with assignee user id
     Then interpreter for defendant updated

  Scenario: When case is assigned to some other user

     Given case is created
     And case is assigned
     When you updateInterpreter on a CaseAggregate using a interpreter details
     Then case update rejected because case assigned

  Scenario: When case is completed

     Given case is created
     And case is completed
     When you updateInterpreter on a CaseAggregate using a interpreter details
     Then case update rejected because case completed
