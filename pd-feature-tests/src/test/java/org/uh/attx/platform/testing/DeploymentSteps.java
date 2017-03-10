/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.testing;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import cucumber.api.java8.En;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 *
 * @author jkesanie
 */
public class DeploymentSteps  implements En {
    
    PlatformServices s = new PlatformServices(false);
    
    public DeploymentSteps() throws Exception {
        Given("^platform has been started$", () -> {
            // Write code here that turns the phrase above into concrete actions
            TestCase.assertTrue(true);
            
        });

        Then("^there should APIs available for WF, GM and DC$", () -> {
            try {
                
                // DC = ES
                GetRequest getDC = Unirest.get(s.getESSiren()).header("accept", "application/json");
                HttpResponse<JsonNode> responseDC =  getDC.asJson();
                TestCase.assertEquals(responseDC.getStatus(), 200);
                
                // GM = Fuseki
                GetRequest getGM = Unirest.get(s.getFuseki());
                HttpResponse<String> responseGM  = getGM.asString();
                TestCase.assertEquals(responseGM.getStatus(), 200);
                
                // WF = UV                
                GetRequest getWF = Unirest.get(s.getUV() + "/unifiedviews");
                HttpResponse<String> responseWF = getWF.asString();
                TestCase.assertEquals(responseWF.getStatus(), 200);
                
                
            } catch(Exception ex ) {
                TestCase.fail(ex.getMessage());
            }
        });   
    }
}
