#----------------------------------
# Scenarios related to the UC1 - Publications and infrastructure
#----------------------------------
Feature: Publications and infrastructures use case (aka UC1)

    Scenario: UC1: Getting first version of the data
    Given that the platform has no prior data
    And that the harvesting pipelines have been created
    And than harvesting pipelines have been scheduled
    When harvesting is succesfully executed
    Then there should be both publication and infrastructure data available for internal use
    And there should be provenance data available for each pipeline
        
    Scenario: UC1: Updating data
    Given that platform contains earlier version of the data
    And than harvesting pipelines have been scheduled
    When harvesting is succesfully executed
    Then the data should be updated and old version removed
    And the provenance should be updated with a new activity

    Scenario: UC1: Linking Tuhat and infrat.avointiede.fi infrastructures
    Given that platform contains earlier version of the data
    And that there existing linking identifier in the data
    When identifier based linking is executed
    Then there should be a new working dataset that contains links
    And there should be a new activity in the provenance dataset 

    
    Scenario: UC1: Publishing new dataset
    Given that platform contains earlier version of the data
    When the aggregated dataset is published to a public endpoint
    Then one should be able to search for documents
   
