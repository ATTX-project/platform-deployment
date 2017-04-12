/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import cucumber.api.java8.En;
import junit.framework.TestCase;

import org.uh.hulib.attx.dev.TestUtils;

/**
 * @author jkesanie
 */
public class DeploymentSteps implements En {

    public DeploymentSteps() throws Exception {
        Given("^platform has been started$", () -> {
            // Write code here that turns the phrase above into concrete actions
            TestCase.assertTrue(true);

        });

        Then("^there should APIs available for WF, GM and DC$", () -> {
            try {

                // DC = ES
                GetRequest getDC = Unirest.get(TestUtils.getESSiren()).header("accept", "application/json");
                HttpResponse<JsonNode> responseDC = getDC.asJson();
                TestCase.assertEquals(responseDC.getStatus(), 200);

                // GM = Fuseki
                GetRequest getGM = Unirest.get(TestUtils.getFuseki());
                HttpResponse<String> responseGM = getGM.asString();
                TestCase.assertEquals(responseGM.getStatus(), 200);

                // WF = UV                
                GetRequest getWF = Unirest.get(TestUtils.getUV() + "/unifiedviews");
                HttpResponse<String> responseWF = getWF.asString();
                TestCase.assertEquals(responseWF.getStatus(), 200);


            } catch (Exception ex) {
                TestCase.fail(ex.getMessage());
            }
        });
    }
}
