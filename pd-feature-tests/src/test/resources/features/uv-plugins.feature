Feature: All the custom plugins and installed and working

    Scenario: Check that ATTX uv-metadata plugin is available
        Given that platform is up and running
        Then UV should contain uv-metadata plugin
