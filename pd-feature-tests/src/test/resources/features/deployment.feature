Feature: All the required platform components are up and running

    Scenario: Platform up
        Given platform has been started
        Then there should APIs available for WF, GM and DC
