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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;


/**
 *
 * @author jkesanie
 */
public class UC1Steps implements En {

    private static final long START_DELAY = 1000;
    private static final long POLLING_INTERVAL = 3000;
    private final String VERSION = "/0.1";

    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";
    private final String ACTIVITY = "{ \"debugging\" : \"false\", \"userExternalId\" : \"admin\" }";

    PlatformServices s = new PlatformServices();

    static List<Integer> pipelineIDs = new ArrayList<Integer>();
    static boolean pollingSuccesful = false;

    private void pollForProcessing(int createdIDPython) throws Exception {
        Timer timer = new Timer();
        final CountDownLatch latch = new CountDownLatch(1);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HttpResponse<JsonNode> resp = null;
                try {
                    String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdIDPython);
                    GetRequest get = Unirest.get(URL);
                    HttpResponse<JsonNode> response1 = get.asJson();
                    JSONObject myObj = response1.getBody().getObject();
                    String status = myObj.getString("status");
                    int result1 = response1.getStatus();
                    if (status.equalsIgnoreCase("Done")){
                        pollingSuccesful = true;
                        latch.countDown();
                        cancel();                        
                    } else if (status.equalsIgnoreCase("Error")) {
                        latch.countDown();
                        cancel();
                        fail("Polling returned Error status.");
                    }
                    //assertThat(status, anyOf(is("WIP"), is("Done")));
                    //assertEquals(200, result1);

                } catch (Exception ex) {
                    latch.countDown();
                    cancel();
                    fail(ex.getMessage());
                }
            }
        }, START_DELAY, POLLING_INTERVAL);
        latch.await();
    }

    public UC1Steps() throws Exception {

        Given("^that the platform has no prior data$", () -> {
            System.out.println("*** 1");
            try {
                // query for graphs in Fuseki 
                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(strStarts(str(?g), 'http://data.hulib.helsinki.fi/attx/work'))\n"
                        + "}";
                HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(graphQuery)
                        .asJson();

                assertEquals(200, queryResponse.getStatus());
                assertEquals(0, queryResponse.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(s.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                //System.out.println(uvResponse.getBody());
                assertEquals(0, uvResponse.getBody().getArray().length());

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not check if platform has no data");
            }
        });
        Given("^than harvesting pipelines have been scheduled$", () -> {
            try {                
                // schedule executions
                for (Integer i : pipelineIDs) {
                    Thread.sleep(2000);
                    String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", i.intValue());
                    HttpResponse<JsonNode> schedulePipelineResponse = Unirest.post(URL)
                            .header("accept", "application/json")
                            .header("Content-Type", "application/json")
                            .basicAuth(API_USERNAME, API_PASSWORD)
                            .body(ACTIVITY)
                            .asJson();
                    assertEquals(200, schedulePipelineResponse.getStatus());
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not schedule pipelines");
            }
        });

        Given("^that the harvesting pipelines have been created$", () -> {
            // import workflows
            try {
                if (pipelineIDs.isEmpty()) {
                    URL resource = UC1Steps.class.getResource("/harvestHYCRIS.zip");
                    HttpResponse<JsonNode> postResponse = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                            .header("accept", "application/json")
                            .basicAuth(API_USERNAME, API_PASSWORD)
                            .field("importUserData", false)
                            .field("importSchedule", false)
                            .field("file", new File(resource.toURI()))
                            .asJson();
                    assertEquals(200, postResponse.getStatus());
                    JSONObject myObj = postResponse.getBody().getObject();
                    System.out.println(myObj);
                    int pipelineID1 = myObj.getInt("id");
                    pipelineIDs.add(pipelineID1);

                    resource = UC1Steps.class.getResource("/harvestInfras.zip");

                    postResponse = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                            .header("accept", "application/json")
                            .basicAuth(API_USERNAME, API_PASSWORD)
                            .field("importUserData", false)
                            .field("importSchedule", false)
                            .field("file", new File(resource.toURI()))
                            .asJson();

                    assertEquals(200, postResponse.getStatus());
                    JSONObject myObj2 = postResponse.getBody().getObject();
                    System.out.println(myObj2);
                    int pipelineID2 = myObj2.getInt("id");
                    pipelineIDs.add(pipelineID2);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not import required pipelines");
            }

        });

        When("^harvesting is succesfully executed$", () -> {
            try {
                Thread.sleep(5000);
                // poll until all pipelines executions are finished or fail after 10 attempts
                String status = "";

                for (Integer pipelineID : pipelineIDs) {
                    System.out.println("Executing pipeline: " + pipelineID);
                    for (int i = 0; i < 10; i++) {
                        String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID.intValue());
                        HttpResponse<JsonNode> schedulePipelineResponse = Unirest.get(URL)
                                .header("accept", "application/json")
                                .header("Content-Type", "application/json")
                                .basicAuth(API_USERNAME, API_PASSWORD)
                                .asJson();
                        assertEquals(200, schedulePipelineResponse.getStatus());
                        JSONArray execs = schedulePipelineResponse.getBody().getArray();
                        Iterator<Object> iterator = execs.iterator();
                        while (iterator.hasNext()) {
                            JSONObject obj = (JSONObject) iterator.next();
                            status = obj.getString("status");
                            if (status.equals("FAILED")) {
                                fail("Pipeline execution failed.");
                            } else if (status.equals("FINISHED_SUCCESS")) {
                                break;
                            }
                        }
                        if (status.equals("FINISHED_SUCCESS")) {
                            break;
                        }
                        Thread.sleep(1000);
                    }

                }
                assertTrue(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Pipeline execution failed.");
            }

        });

        Then("^there should be both publication and infrastructure data available for internal use$", () -> {

            try {
                
                Thread.sleep(5000);
                HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body("ASK\n"
                                + "FROM <http://data.hulib.helsinki.fi/attx/work/1> \n"
                                + "{?s a <http://data.hulib.helsinki.fi/attx/types/Infrastructure> \n"
                                + "}")
                        .asJson();

                HttpResponse<JsonNode> queryResponse2 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body("ASK\n"
                                + "FROM <http://data.hulib.helsinki.fi/attx/work/1> \n"
                                + "{?s a <http://data.hulib.helsinki.fi/attx/types/Publication> \n"
                                + "}")
                        .asJson();

                HttpResponse<JsonNode> queryResponse3 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body("ASK\n"
                                + "FROM <http://data.hulib.helsinki.fi/attx/work/2> \n"
                                + "{?s a <http://data.hulib.helsinki.fi/attx/types/Infrastructure> \n"
                                + "}")
                        .asJson();
                
                assertTrue(queryResponse.getBody().getObject().getBoolean("boolean"));
                assertTrue(queryResponse2.getBody().getObject().getBoolean("boolean"));
                assertTrue(queryResponse3.getBody().getObject().getBoolean("boolean"));

            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }

        });
        
        Then("^there should be provenance data available for each pipeline$", () -> {
            try {
                Thread.sleep(5000);
                // update prov
                HttpResponse<JsonNode> provResponse = Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=http://wfapi:4301/0.1&graphStore=http://fuseki:3030/ds")
                        .header("content-type", "application/json")
                        .asJson();
                
                System.out.println(provResponse.getBody());
                JSONObject provObj = provResponse.getBody().getObject();
                
                
                // query prov graph 
                
                // input dataset for infrat pipeline
                String queryWork2Input = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"<http://infrat.avointiede.fi>\n" +
"        a       <http://data.hulib.helsinki.fi/attx/onto#Dataset> , <http://www.w3.org/ns/sparql-service-description#Dataset> ;\n" +
"        <http://purl.org/dc/elements/1.1/description>\n" +
"                \"Test\" ;\n" +
"        <http://purl.org/dc/elements/1.1/publisher>\n" +
"                \"CSC\" ;\n" +
"        <http://purl.org/dc/elements/1.1/source>\n" +
"                \"http://infrat.avointiede.fi\" ;\n" +
"        <http://purl.org/dc/elements/1.1/title>\n" +
"                \"Infrat\" ;\n" +
"        <https://creativecommons.org/ns#license>\n" +
"                <http://data.hulib.helsinki.fi/attx/onto#Unknown> .}	";
                
                
                
                // output dataset for infrat pipeline 
                String queryWork2Output = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"<http://data.hulib.helsinki.fi/attx/work/2>\n" +
"        a       <http://data.hulib.helsinki.fi/attx/onto#Dataset> , <http://www.w3.org/ns/sparql-service-description#Dataset> ;\n" +
"        <http://purl.org/dc/elements/1.1/description>\n" +
"                \"Test\" ;\n" +
"        <http://purl.org/dc/elements/1.1/publisher>\n" +
"                \"HY\" ;\n" +
"        <http://purl.org/dc/elements/1.1/source>\n" +
"                \"http://infrat.avointiede.fi\" ;\n" +
"        <http://purl.org/dc/elements/1.1/title>\n" +
"                \"Work2 - infras\" ;\n" +
"        <https://creativecommons.org/ns#license>\n" +
"                <http://data.hulib.helsinki.fi/attx/onto#Unknown> .   \n" +
"}	";
                
                // input dataset for TUHAT pipeline 
                String queryWork1Input = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"<http://data.hulib.helsinki.fi/attx/work/1/input>\n" +
"        a       <http://data.hulib.helsinki.fi/attx/onto#Dataset> , <http://www.w3.org/ns/sparql-service-description#Dataset> ;\n" +
"        <http://purl.org/dc/elements/1.1/description>\n" +
"                \"Test\" ;\n" +
"        <http://purl.org/dc/elements/1.1/publisher>\n" +
"                \"Test\" ;\n" +
"        <http://purl.org/dc/elements/1.1/source>\n" +
"                \"http://www.helsinki.fi\" ;\n" +
"        <http://purl.org/dc/elements/1.1/title>\n" +
"                \"Original TUHAT pubs and infras\" ;\n" +
"        <https://creativecommons.org/ns#license>\n" +
"                <http://data.hulib.helsinki.fi/attx/onto#Unknown> .\n" +
"\n" +
"}";
                
                // output dataset for TUHAT pipeline
                String queryWork1Output = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"<http://data.hulib.helsinki.fi/attx/work/1>\n" +
"        a       <http://data.hulib.helsinki.fi/attx/onto#Dataset> , <http://www.w3.org/ns/sparql-service-description#Dataset> ;\n" +
"        <http://purl.org/dc/elements/1.1/description>\n" +
"                \"Test\" ;\n" +
"        <http://purl.org/dc/elements/1.1/publisher>\n" +
"                \"HY\" ;\n" +
"        <http://purl.org/dc/elements/1.1/source>\n" +
"                \"http://www.helsinki.fi\" ;\n" +
"        <http://purl.org/dc/elements/1.1/title>\n" +
"                \"Internal Tuhat pubs and infras\" ;\n" +
"        <https://creativecommons.org/ns#license>\n" +
"                <http://data.hulib.helsinki.fi/attx/onto#Unknown> .\n" +
"}";
                
                // activities             
                String queryActivity1 = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"	?act1 a <http://data.hulib.helsinki.fi/attx/onto#WorkflowExecution> , <http://www.w3.org/ns/prov#Activity> .\n" +
"    ?act1 <http://www.w3.org/ns/prov#used>\n" +
"                <http://data.hulib.helsinki.fi/attx/work/1/input> .\n" +
"    ?act1 <http://www.w3.org/ns/prov#generated>\n" +
"                <http://data.hulib.helsinki.fi/attx/work/1> ;      \n" +
"}	";
                
                String queryActivity2 = "ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"	?act1 a <http://data.hulib.helsinki.fi/attx/onto#WorkflowExecution> , <http://www.w3.org/ns/prov#Activity> .\n" +
"    ?act1 <http://www.w3.org/ns/prov#used>\n" +
"                <http://infrat.avointiede.fi> .\n" +
"    ?act1 <http://www.w3.org/ns/prov#generated>\n" +
"                <http://data.hulib.helsinki.fi/attx/work/2> ;      \n" +
"}	";
                
                
                // do some quering
                HttpResponse<JsonNode> resp1 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryWork1Input)
                        .asJson();
                HttpResponse<JsonNode> resp2 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryWork1Output)
                        .asJson();
                HttpResponse<JsonNode> resp3 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryWork2Input)
                        .asJson();
                HttpResponse<JsonNode> resp4 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryWork2Output)
                        .asJson();
                HttpResponse<JsonNode> resp5 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryActivity1)
                        .asJson();                
                HttpResponse<JsonNode> resp6 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(queryActivity2)
                        .asJson();          
                
                
                assertTrue(resp1.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp2.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp3.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp4.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp5.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp6.getBody().getObject().getBoolean("boolean"));                
                
                
            }catch(Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });
        
        
        Given("^that platform contains earlier version of the data$", () -> {
            System.out.println("****: 2");
            try {
                // query for graphs in Fuseki 
                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(?g != <http://data.hulib.helsinki.fi/attx/onto> && ?g != <http://data.hulib.helsinki.fi/attx/prov>)\n"
                        + "}";
                HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(graphQuery)
                        .asJson();

                assertEquals(200, queryResponse.getStatus());
                assertTrue( 0 <queryResponse.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(s.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                //System.out.println(uvResponse.getBody());
                assertTrue(0 < uvResponse.getBody().getArray().length());

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not check that contains data");
            }
            
        });
        
        Then("^the data should be updated and old version removed$", () -> {
            try {
                Thread.sleep(5000);
                // update prov
                HttpResponse<JsonNode> provResponse = Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=http://wfapi:4301/0.1&graphStore=http://fuseki:3030/ds")
                        .header("content-type", "application/json")
                        .asJson();                
                
                // there still exists only one output graph / harvesting pipeline
                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n" +
"WHERE {\n" +
"  GRAPH ?g { ?s ?p ?o }\n" +
"  FILTER(?g = <http://data.hulib.helsinki.fi/attx/work/1> || ?g = <http://data.hulib.helsinki.fi/attx/work/2>)\n" +
"}";
                
                HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(graphQuery)
                        .asJson();

                assertEquals(200, queryResponse.getStatus());
                assertEquals(2, queryResponse.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));
                                

                
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }

        });
        
        Then("^the provenance should be updated with a new activity$", () -> {
            try {
                Thread.sleep(5000);
                // update prov (again)
                Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=http://wfapi:4301/0.1&graphStore=http://fuseki:3030/ds")
                        .header("content-type", "application/json")
                        .asJson();
            
                // using timestamps to test that there are atleast two activities linked to the working graphs
                
                String actQuery1 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                                    "ASK \n" +
                                    "FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
                                    "  ?act1 a <http://www.w3.org/ns/prov#Activity> .\n" +
                                    "  ?act2 a <http://www.w3.org/ns/prov#Activity> .\n" +
                                    "  ?act1 <http://www.w3.org/ns/prov#endedAtTime> ?t1 .\n" +
                                    "  ?act2 <http://www.w3.org/ns/prov#endedAtTime> ?t2 .\n" +
                                    "  ?act1 <http://www.w3.org/ns/prov#generated>\n" +
                                    "                <http://data.hulib.helsinki.fi/attx/work/1> .\n" +
                                    "  ?act2 <http://www.w3.org/ns/prov#generated>\n" +
                                    "                <http://data.hulib.helsinki.fi/attx/work/1> .  \n" +
                                    "  FILTER(?t1 < ?t2)\n" +
                                    "}";

                String actQuery2 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                                    "ASK \n" +
                                    "FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
                                    "  ?act1 a <http://www.w3.org/ns/prov#Activity> .\n" +
                                    "  ?act2 a <http://www.w3.org/ns/prov#Activity> .\n" +
                                    "  ?act1 <http://www.w3.org/ns/prov#endedAtTime> ?t1 .\n" +
                                    "  ?act2 <http://www.w3.org/ns/prov#endedAtTime> ?t2 .\n" +
                                    "  ?act1 <http://www.w3.org/ns/prov#generated>\n" +
                                    "                <http://data.hulib.helsinki.fi/attx/work/2> .\n" +
                                    "  ?act2 <http://www.w3.org/ns/prov#generated>\n" +
                                    "                <http://data.hulib.helsinki.fi/attx/work/2> .  \n" +
                                    "  FILTER(?t1 < ?t2)\n" +
                                    "}";

                HttpResponse<JsonNode> resp1 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(actQuery1)
                        .asJson();          
                
                HttpResponse<JsonNode> resp2 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(actQuery2)
                        .asJson();          
                
                
                assertTrue(resp1.getBody().getObject().getBoolean("boolean"));                
                assertTrue(resp2.getBody().getObject().getBoolean("boolean"));                
                
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
            
        });
        
        
        When("^the aggregated dataset is published to a public endpoint$", () -> {
            try {
                String payload = new String(Files.readAllBytes(Paths.get(getClass().getResource("/indexPayload.json").toURI())));

                HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + VERSION + "/index")
                        .header("content-type", "application/json")
                        .body(payload)
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                int indexingID = myObj.getInt("id");
                int result3 = postResponse.getStatus();
                assertEquals(202, result3);
                pollingSuccesful = false;
                pollForProcessing(indexingID);
                
                assertTrue(pollingSuccesful);
                
            }catch(Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });
        
        Then("^one should be able to search for documents$", () -> {
            try {
                // refresh index
                Unirest.post(s.getESSiren() + "/current/_refresh");                
                
                // query
                int total = 0;
                for(int i = 0; i < 10; i++) {

                    HttpResponse<JsonNode> jsonResponse = Unirest.get(s.getESSiren() + "/current/_search?q=*")
                    .asJson();

                    JSONObject obj = jsonResponse.getBody().getObject();
                    if(obj.has("hits")) {
                        total = obj.getJSONObject("hits").getInt("total");
                        if(total > 0) {
                            assertTrue(true);
                            return;
                        }
                    }                    
                    Thread.sleep(1000);
                }
                fail("Could not query indexing results");
            }catch(Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });
        
        Given("^that there existing linking identifier in the data$", () -> {
            // test data is set up so, that there are linking identifiers
            assertTrue(true);
            
        });
        
        When("^identifier based linking is executed$", () -> {
            try {
                // import Linking pipeline
                URL resource = UC1Steps.class.getResource("/linking.zip");
                HttpResponse<JsonNode> postResponse = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                        .header("accept", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .field("importUserData", false)
                        .field("importSchedule", false)
                        .field("file", new File(resource.toURI()))
                        .asJson();
                assertEquals(200, postResponse.getStatus());
                JSONObject myObj = postResponse.getBody().getObject();
                int pipelineID = myObj.getInt("id");
                
                // schedule linking 
                String schedulingURL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
                HttpResponse<JsonNode> schedulePipelineResponse1 = Unirest.post(schedulingURL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .body(ACTIVITY)
                        .asJson();
                assertEquals(200, schedulePipelineResponse1.getStatus());
                
                // poll for results                
                for (int i = 0; i < 10; i++) {
                    String executionGetURL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
                    HttpResponse<JsonNode> schedulePipelineResponse2 = Unirest.get(executionGetURL)
                            .header("accept", "application/json")
                            .header("Content-Type", "application/json")
                            .basicAuth(API_USERNAME, API_PASSWORD)
                            .asJson();
                    assertEquals(200, schedulePipelineResponse2.getStatus());
                    JSONArray execs = schedulePipelineResponse2.getBody().getArray();
                    Iterator<Object> iterator = execs.iterator();
                    while (iterator.hasNext()) {
                        JSONObject obj = (JSONObject) iterator.next();
                        String status = obj.getString("status");
                        if (status.equals("FAILED")) {
                            fail("Pipeline execution failed.");
                            return;
                        } else if (status.equals("FINISHED_SUCCESS")) {
                            assertTrue(true);
                            return;
                        }
                    }
                    Thread.sleep(1000);
                }
                
            }catch(Exception ex) {
                ex.printStackTrace();
                fail("Linking failed." + ex.getMessage());
            }
            
            
        });
        
        Then("^there should be a new working dataset that contains links$", () -> {
            try {
                String query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
"ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/work3> {\n" +
" ?id1 <http://www.w3.org/2004/02/skos/core#exactMatch> ?id2 }";

                HttpResponse<JsonNode> resp1 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(query)
                        .asJson();  
                assertTrue(resp1.getBody().getObject().getBoolean("boolean"));
                
            }catch(Exception ex) {
                ex.printStackTrace();
                fail("Checking for link data set failed." + ex.getMessage());
            }
        });

        
        Then("^there should be a new activity in the provenance dataset$", () -> {
            try {
                Thread.sleep(5000);
                // update prov (again)
                Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=http://wfapi:4301/0.1&graphStore=http://fuseki:3030/ds")
                        .header("content-type", "application/json")
                        .asJson();
                
                Thread.sleep(2000);
            
                String actQuery = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
"ASK \n" +
"FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"  ?act1 a <http://www.w3.org/ns/prov#Activity> .\n" +
"  ?act1 <http://www.w3.org/ns/prov#generated>\n" +
"                <http://data.hulib.helsinki.fi/attx/work3> .\n" +
"  ?act1 <http://www.w3.org/ns/prov#used>\n" +
"                <http://data.hulib.helsinki.fi/attx/work/1> .  \n" +
"  ?act1 <http://www.w3.org/ns/prov#used>\n" +
"                <http://data.hulib.helsinki.fi/attx/work/2> .  \n" +                       
"}";


                Thread.sleep(2000);
                
                HttpResponse<JsonNode> resp1 = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body(actQuery)
                        .asJson();          
                
                assertTrue(resp1.getBody().getObject().getBoolean("boolean"));                
                
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }            
        });
    }

}
