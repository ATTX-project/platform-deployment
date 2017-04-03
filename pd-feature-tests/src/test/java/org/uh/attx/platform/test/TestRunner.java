/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author jkesanie
 */
@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty"}, features = "classpath:features", tags = {"~@ignore"}, glue = {"classpath:org.uh.attx.platform.test"})
public class TestRunner {

}
