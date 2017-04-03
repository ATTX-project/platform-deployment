/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

import cucumber.api.java8.En;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.core.IsEqual;
import org.json.JSONObject;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;


/**
 * @author jkesanie
 */
public class UC1Steps implements En {

    PlatformServices s = new PlatformServices();
    private final String VERSION = "/0.1";

    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";
    private final String ACTIVITY = "{ \"debugging\" : \"false\", \"userExternalId\" : \"admin\" }";

    static List<Integer> pipelineIDs = new ArrayList<Integer>();

    private JSONObject getQueryResultField(HttpResponse<JsonNode> response, String field) {
        JSONObject queryObject = response.getBody().getObject().getJSONObject("results");
        return queryObject.getJSONArray("bindings").getJSONObject(0).getJSONObject(field);
    }

    private HttpResponse<JsonNode> graphQueryResult(String endpoint, String query) {
        String URL = String.format(s.getFuseki() + "/%s/query", endpoint);
        HttpResponse<JsonNode> queryResponse = null;
        try {
            queryResponse = Unirest.post(URL)
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body(query)
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
            fail("Could not query Graph Store at:" + URL);
        }
        return queryResponse;
    }

    private int importPipeline(URL resource) {
        int pipelineID = 0;
        try {
            HttpResponse<JsonNode> pipelineRequest = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                    .header("accept", "application/json")
                    .basicAuth(API_USERNAME, API_PASSWORD)
                    .field("importUserData", false)
                    .field("importSchedule", false)
                    .field("file", new File(resource.toURI()))
                    .asJson();
            assertEquals(200, pipelineRequest.getStatus());
            JSONObject pipelineObject = pipelineRequest.getBody().getObject();
            System.out.println(pipelineObject);
            pipelineID = pipelineObject.getInt("id");
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Could not import pipeline resource:" + resource);
        }
        return pipelineID;
    }

    private Callable<String> pollForWorkflowExecution(Integer pipelineID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions/last", pipelineID.intValue());
                HttpResponse<JsonNode> schedulePipelineResponse = Unirest.get(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                if (schedulePipelineResponse.getStatus() == 200) {
                    JSONObject execution = schedulePipelineResponse.getBody().getObject();
                    String status = execution.getString("status");
                    System.out.println(status);
                    return status;
                } else {
                    return "Not yet";
                }
            }
        };
    }

    private Callable<Integer> pollForWorkflowStart(Integer pipelineID) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
                HttpResponse<JsonNode> workflowStart = Unirest.post(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .body(ACTIVITY)
                        .asJson();
                JSONObject execution = workflowStart.getBody().getObject();
                String status = execution.getString("status");
                System.out.println(status);
                return workflowStart.getStatus();
            }
        };
    }

    private void updateProv() throws Exception{
        String provRequest = String.format(s.getGmapi() + VERSION + "/prov?start=true&wfapi=http://wfapi:4301" + VERSION +
                "&graphStore=http://fuseki:3030/ds");

        HttpResponse<JsonNode> wfProv = Unirest.get(provRequest)
                .header("content-type", "application/json")
                .asJson();
        JSONObject provObj = wfProv.getBody().getObject();

        assertEquals(200, wfProv.getStatus());
        assertEquals("Done", provObj.getString("status"));
    }

    private Callable<Boolean> askQueryAnswer(String query) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                boolean queryResult = false;
                try {
                    HttpResponse<JsonNode> queryWork = graphQueryResult("ds", query);
                    System.out.println(queryWork.getBody().getObject().getBoolean("boolean"));
                    queryResult = queryWork.getBody().getObject().getBoolean("boolean");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail("Query not true.\n" + query);
                }
                return queryResult;
            }
        };
    }

    private void askGraphStoreIfTrue(String query) throws Exception {
        await().atMost(120, TimeUnit.SECONDS).until(askQueryAnswer(query), equalTo(true));
    }

    private Callable<String> pollForIndexStatus(Integer createdID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdID);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                JSONObject myObj = response1.getBody().getObject();
                String status = myObj.getString("status");
                System.out.println(status);
                return status;
            }
        };
    }

    private Callable<Integer> waitForESResults(String esEndpoint, String esIndex) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                int totalHits = 0;
                Unirest.post(esEndpoint + "/"+ esIndex +"/_refresh");
                HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(esEndpoint + "/"+ esIndex +"/_search?q=*")
                        .asJson();

                JSONObject esObj = jsonResponse.getBody().getObject();
                if(esObj.has("hits")) {
                    totalHits = esObj.getJSONObject("hits").getInt("total");
                }
                System.out.println(esEndpoint + ": " + totalHits);
                return totalHits;
            }
        };
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
                HttpResponse<JsonNode> emptyGraph = graphQueryResult("ds", graphQuery);

                assertEquals(200, emptyGraph.getStatus());
                assertEquals(0, getQueryResultField(emptyGraph, "count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(s.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                assertEquals(0, uvResponse.getBody().getArray().length());

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not check if platform has no data");
            }
        });

        Given("^that the harvesting pipelines have been created$", () -> {
            // import workflows
            try {
                if (pipelineIDs.isEmpty()) {
                    URL resourceHYCRIS = UC1Steps.class.getResource("/harvestHYCRIS.zip");
                    URL resourceInfras = UC1Steps.class.getResource("/harvestInfras.zip");

                    pipelineIDs.add(importPipeline(resourceHYCRIS));
                    pipelineIDs.add(importPipeline(resourceInfras));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not import required pipelines");
            }
        });

        Given("^harvesting pipelines have been scheduled$", () -> {
            try {
                // schedule executions
                for (Integer pipelineID : pipelineIDs) {
                    await().atMost(20, TimeUnit.SECONDS).until(pollForWorkflowStart(pipelineID.intValue()), equalTo(200));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not schedule pipelines");
            }
        });

        When("^harvesting is successfully executed$", () -> {
            try {
                for (Integer pipelineID : pipelineIDs) {
                    System.out.println("Executing pipeline: " + pipelineID);
                    await().atMost(180, TimeUnit.SECONDS).until(pollForWorkflowExecution(pipelineID.intValue()), equalTo("FINISHED_SUCCESS"));
                }
            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded, could not get WorkflowExecution as it did not finish successfully.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Pipeline execution failed.");
            }
        });

        Then("^there should be both publication and infrastructure data available for internal use$", () -> {
            try {
                String infrasQuery = "ASK\n"
                        + "FROM <http://data.hulib.helsinki.fi/attx/work/1> \n"
                        + "{?s a <http://data.hulib.helsinki.fi/attx/types/Infrastructure> \n"
                        + "}";

                String pubsQuery = "ASK\n"
                        + "FROM <http://data.hulib.helsinki.fi/attx/work/1> \n"
                        + "{?s a <http://data.hulib.helsinki.fi/attx/types/Publication> \n"
                        + "}";

                String infrasQuery2 = "ASK\n"
                        + "FROM <http://data.hulib.helsinki.fi/attx/work/2> \n"
                        + "{?s a <http://data.hulib.helsinki.fi/attx/types/Infrastructure> \n"
                        + "}";

                askGraphStoreIfTrue(infrasQuery);
                askGraphStoreIfTrue(infrasQuery2);
                askGraphStoreIfTrue(pubsQuery);


            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded, could not get infrastructure and publication data as it did not finish successfully. Graph Store exceeded time limit.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Then("^there should be provenance data available for each pipeline$", () -> {
            try {
                // update prov
                updateProv();

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
                askGraphStoreIfTrue(queryWork1Input);
                askGraphStoreIfTrue(queryWork1Output);
                askGraphStoreIfTrue(queryWork2Input);
                askGraphStoreIfTrue(queryWork2Output);
                askGraphStoreIfTrue(queryActivity1);
                askGraphStoreIfTrue(queryActivity2);

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Graph Store exceeded time limit.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });


        Given("^that platform contains an earlier version of the data$", () -> {
            System.out.println("****: 2");
            try {
                // query for graphs in Fuseki 
                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(?g != <http://data.hulib.helsinki.fi/attx/onto> && ?g != <http://data.hulib.helsinki.fi/attx/prov>)\n"
                        + "}";
                HttpResponse<JsonNode> notEmptyGraph = graphQueryResult("ds", graphQuery);

                assertEquals(200, notEmptyGraph.getStatus());
                assertTrue(0 < getQueryResultField(notEmptyGraph, "count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(s.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                assertTrue(0 < uvResponse.getBody().getArray().length());

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Could not check that contains data.");
            }
        });

        Then("^the data should be updated and old version removed$", () -> {
            try {
                // update prov
                updateProv();

                // there still exists only one output graph / harvesting pipeline

                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(strStarts(str(?g), 'http://data.hulib.helsinki.fi/attx/work'))\n"
                        + "}";

                await().atMost(30, TimeUnit.SECONDS).until(() -> {
                    try {
                        HttpResponse<JsonNode> workingGraphs = graphQueryResult("ds", graphQuery);
                        assertEquals(200, workingGraphs.getStatus());
                        assertEquals(2, getQueryResultField(workingGraphs, "count").getInt("value"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        fail("Query for work graphs failed.");
                    }
                });

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Graph Store exceeded time limit.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Then("^the provenance should be updated with a new activity$", () -> {
            try {
                // update prov (again)
                updateProv();

                // using timestamps to test that there are atleast two activities linked to the working graphs

                String actQuery1 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "ASK\n" +
                        "FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
                        "?act1 a <http://www.w3.org/ns/prov#Activity> ; " +
                        "<http://www.w3.org/ns/prov#endedAtTime> ?t1 ; " +
                        "<http://www.w3.org/ns/prov#generated> <http://data.hulib.helsinki.fi/attx/work/1> .\n" +
                        "?act2 a <http://www.w3.org/ns/prov#Activity> ; " +
                        "<http://www.w3.org/ns/prov#endedAtTime> ?t2 ; " +
                        "<http://www.w3.org/ns/prov#generated> <http://data.hulib.helsinki.fi/attx/work/1> .\n" +
//                        "FILTER(?t1 < ?t2)\n" +
                        "}";

                String actQuery2 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "ASK\n" +
                        "FROM <http://data.hulib.helsinki.fi/attx/prov> {\n" +
                        "?act1 a <http://www.w3.org/ns/prov#Activity> ; " +
                        "<http://www.w3.org/ns/prov#endedAtTime> ?t1 ; " +
                        "<http://www.w3.org/ns/prov#generated> <http://data.hulib.helsinki.fi/attx/work/2> .\n" +
                        "?act2 a <http://www.w3.org/ns/prov#Activity> ; " +
                        "<http://www.w3.org/ns/prov#endedAtTime> ?t2 ; " +
                        "<http://www.w3.org/ns/prov#generated> <http://data.hulib.helsinki.fi/attx/work/2> .\n" +
//                        "FILTER(?t1 < ?t2)\n" +
                        "}";

                askGraphStoreIfTrue(actQuery1);
                askGraphStoreIfTrue(actQuery2);

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Could not get activities from graph.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }

        });


        When("^the aggregated dataset is published to a public endpoint$", () -> {
            try {
                String payload = new String(Files.readAllBytes(Paths.get(getClass().getResource("/indexPayload.json").toURI())));

                HttpResponse<JsonNode> postIndex = Unirest.post(s.getGmapi() + VERSION + "/index")
                        .header("content-type", "application/json")
                        .body(payload)
                        .asJson();
                JSONObject indexObj = postIndex.getBody().getObject();
                int createdID = indexObj.getInt("id");
                assertEquals(202, postIndex.getStatus());

                await().atMost(20, TimeUnit.SECONDS).until(pollForIndexStatus(createdID), equalTo("Done"));

            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Then("^one should be able to search for documents$", () -> {
            try {
                // refresh index
                Unirest.post(s.getESSiren() + "/current/_refresh");

                // query
                await().atMost(45, TimeUnit.SECONDS).until(waitForESResults(s.getESSiren(), "current"), greaterThan(0));

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. ESSiren query failed.");
            }  catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Given("^there are existing linking identifiers$", () -> {
            // test data is set up so, that there are linking identifiers
            assertTrue(true);

        });

        When("^identifier based linking is executed$", () -> {
            try {
                // import Linking pipeline
                URL linkResource = UC1Steps.class.getResource("/linking.zip");
                int pipelineID = importPipeline(linkResource);

                // schedule linking
                await().atMost(20, TimeUnit.SECONDS).until(pollForWorkflowStart(pipelineID), equalTo(200));
                await().atMost(180, TimeUnit.SECONDS).until(pollForWorkflowExecution(pipelineID), equalTo("FINISHED_SUCCESS"));

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. UnifiedViews did not do its job.");
            } catch (Exception ex) {
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

                askGraphStoreIfTrue(query);

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Graph Store exceeded time limit.");
            }  catch (Exception ex) {
                ex.printStackTrace();
                fail("Checking for link data set failed." + ex.getMessage());
            }
        });


        Then("^there should be a new activity in the provenance dataset$", () -> {
            try {
                // update prov (again)
                updateProv();

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

                askGraphStoreIfTrue(actQuery);

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Graph Store exceeded time limit.");
            }  catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });
    }

}
