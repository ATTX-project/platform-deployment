/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import cucumber.api.java8.En;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONObject;

import org.uh.hulib.attx.dev.TestUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;


/**
 * @author jkesanie
 */
public class UC1Steps implements En {

    static List<Integer> pipelineIDs = new ArrayList<Integer>();

    public UC1Steps() throws Exception {

        Given("^that the platform has no prior data$", () -> {
            
            try {
                // query for graphs in Fuseki 
                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(strStarts(str(?g), 'http://data.hulib.helsinki.fi/attx/work'))\n"
                        + "}";
                HttpResponse<JsonNode> emptyGraph = TestUtils.graphQueryResult(graphQuery);

                assertEquals(200, emptyGraph.getStatus());
                assertEquals(0, TestUtils.getQueryResultField(emptyGraph, "count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(TestUtils.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(TestUtils.API_USERNAME, TestUtils.API_PASSWORD)
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

                    pipelineIDs.add(TestUtils.importPipeline(resourceHYCRIS));
                    pipelineIDs.add(TestUtils.importPipeline(resourceInfras));
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
                    await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowStart(pipelineID.intValue()), equalTo(200));
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
                    await().atMost(240, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowExecution(pipelineID.intValue()), equalTo("FINISHED_SUCCESS"));
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

                TestUtils.askGraphStoreIfTrue(infrasQuery);
                TestUtils.askGraphStoreIfTrue(infrasQuery2);
                TestUtils.askGraphStoreIfTrue(pubsQuery);


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
                TestUtils.updateProv();

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
                TestUtils.askGraphStoreIfTrue(queryWork1Input);
                TestUtils.askGraphStoreIfTrue(queryWork1Output);
                TestUtils.askGraphStoreIfTrue(queryWork2Input);
                TestUtils.askGraphStoreIfTrue(queryWork2Output);
                TestUtils.askGraphStoreIfTrue(queryActivity1);
                TestUtils.askGraphStoreIfTrue(queryActivity2);

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
                HttpResponse<JsonNode> notEmptyGraph = TestUtils.graphQueryResult(graphQuery);

                assertEquals(200, notEmptyGraph.getStatus());
                assertTrue(0 < TestUtils.getQueryResultField(notEmptyGraph, "count").getInt("value"));

                // check for pipelines and executions in UV
                HttpResponse<JsonNode> uvResponse = Unirest.get(TestUtils.getUV() + "/master/api/1/pipelines?userExternalId=admin")
                        .header("content-type", "application/json")
                        .basicAuth(TestUtils.API_USERNAME, TestUtils.API_PASSWORD)
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
                TestUtils.updateProv();

                // there still exists only one output graph / harvesting pipeline

                String graphQuery = "SELECT (COUNT(DISTINCT ?g) as ?count)\n"
                        + "WHERE {\n"
                        + "  GRAPH ?g { ?s ?p ?o }\n"
                        + "  FILTER(strStarts(str(?g), 'http://data.hulib.helsinki.fi/attx/work'))\n"
                        + "}";

                await().atMost(30, TimeUnit.SECONDS).until(() -> {
                    try {
                        HttpResponse<JsonNode> workingGraphs = TestUtils.graphQueryResult(graphQuery);
                        assertEquals(200, workingGraphs.getStatus());
                        assertEquals(2, TestUtils.getQueryResultField(workingGraphs, "count").getInt("value"));
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
                TestUtils.updateProv();

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

                TestUtils.askGraphStoreIfTrue(actQuery1);
                TestUtils.askGraphStoreIfTrue(actQuery2);

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

                HttpResponse<JsonNode> postIndex = Unirest.post(TestUtils.getGmapi() + TestUtils.VERSION + "/index")
                        .header("content-type", "application/json")
                        .body(payload)
                        .asJson();
                JSONObject indexObj = postIndex.getBody().getObject();
                int createdID = indexObj.getInt("id");
                assertEquals(202, postIndex.getStatus());

                await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForIndexStatus(createdID), equalTo("Done"));

            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Then("^one should be able to search for documents$", () -> {
            try {
                // refresh index
                Unirest.post(TestUtils.getESSiren() + "/current/_refresh");

                // query
                await().atMost(45, TimeUnit.SECONDS).until(TestUtils.waitForESResults(TestUtils.getESSiren(), "current"), greaterThan(0));

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. ESSiren query failed.");
            }  catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });

        Given("^there are existing linking identifiers$", () -> {
            // test data is set up so, that there are linking identifiers
            
            // confirming that by querying both datasets
            try {
                String work1Query = "ASK\n"
                        + "FROM <http://data.hulib.helsinki.fi/attx/work/1> \n"
                        + "{?s <http://data.hulib.helsinki.fi/attx/urn> <urn:nbn:fi:research-infras-201607252>  \n"
                        + "}";

                String work2Query = "ASK\n"
                        + "FROM <http://data.hulib.helsinki.fi/attx/work/2> \n"
                        + "{?s <http://purl.org/dc/terms/identifier> <urn:nbn:fi:research-infras-201607252> \n"
                        + "}";

                TestUtils.askGraphStoreIfTrue(work1Query);
                TestUtils.askGraphStoreIfTrue(work2Query);


            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded, could not get infrastructure and publication data as it did not finish successfully. Graph Store exceeded time limit.");
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }            

        });

        When("^identifier based linking is executed$", () -> {
            try {
                // import Linking pipeline
                URL linkResource = UC1Steps.class.getResource("/linking.zip");
                int pipelineID = TestUtils.importPipeline(linkResource);

                // schedule linking
                await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowStart(pipelineID), equalTo(200));
                await().atMost(180, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowExecution(pipelineID), equalTo("FINISHED_SUCCESS"));

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

                TestUtils.askGraphStoreIfTrue(query);

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
                TestUtils.updateProv();

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

                TestUtils.askGraphStoreIfTrue(actQuery);

            } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded. Graph Store exceeded time limit.");
            }  catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        });
    }

}
