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
import java.io.File;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 *
 * @author jkesanie
 */
public class UVPluginsSteps  implements En {
    private PlatformServices s = new PlatformServices();

    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";


    public UVPluginsSteps() throws Exception {
        Given("^that platform is up and running$", () -> {
            // Write code here that turns the phrase above into concrete actions
            TestCase.assertTrue(true);

        });

        Then("^UV should contain uv-metadata plugin$", () -> {
            try {

                // using the UV rest API and trying to add the plugin again. Should response with an error.
                HttpResponse<JsonNode> response = Unirest.post(s.getUV() + "/master/api/1/import/dpu/jar")
                        .header("accept", "application/json")
                        .queryString("name", "attx-t-metadata-plugin")
                        .queryString("force", "false")
                        .field("file", new File("uv-dpu-t-attx-metadata-1.0-SNAPSHOT.jar"))
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();

                System.out.println("**"  + response.getBody().toString());
                JSONAssert.assertEquals("{\"errorMessage\": \"DPU with this name already exists.\"}", response.getBody().toString(), JSONCompareMode.LENIENT);

                TestCase.assertEquals(400, response.getStatus());



            } catch(Exception ex ) {
                TestCase.fail(ex.getMessage());
            }
        });
    }
}
