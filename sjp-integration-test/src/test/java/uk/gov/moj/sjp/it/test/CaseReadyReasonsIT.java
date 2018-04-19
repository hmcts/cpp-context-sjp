package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.sjp.it.helper.CaseReadyReasonsHelper.requestCaseMarkedReady;
import static uk.gov.moj.sjp.it.helper.CaseReadyReasonsHelper.requestCaseUnmarkedReady;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;

import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseReadyReasonsIT extends BaseIntegrationTest {

    private final UUID caseId = randomUUID();
    final SjpDatabaseCleaner sjpDatabaseCleaner = new SjpDatabaseCleaner();

    @Before
    public void cleanDatabase() throws Exception {
        sjpDatabaseCleaner.cleanAll();
    }

    @Test
    public void shouldRecordCaseReadyReasons() {

        requestCaseMarkedReady(randomUUID(), "PIA");
        requestCaseMarkedReady(randomUUID(), "PIA");
        assertEquals(getReadyCasesReasonsCounts().getJsonArray("reasons").size(), 1);
        assertEquals(getReadyCasesReasonsCounts().getJsonArray("reasons").getJsonObject(0).getInt("count"), 2);

        requestCaseMarkedReady(caseId, "GUILTY");
        JsonObject response = getReadyCasesReasonsCounts();
        assertEquals(response.getJsonArray("reasons").size(), 2);

        requestCaseMarkedReady(caseId, "PIA");
        response = getReadyCasesReasonsCounts();
        assertEquals(response.getJsonArray("reasons").size(), 1);
        assertEquals(response.getJsonArray("reasons").getJsonObject(0).getInt("count"), 3);

        requestCaseUnmarkedReady(caseId);
        response = getReadyCasesReasonsCounts();
        assertEquals(response.getJsonArray("reasons").size(), 1);
        assertEquals(response.getJsonArray("reasons").getJsonObject(0).getInt("count"), 2);
    }

    private static JsonObject getReadyCasesReasonsCounts() {
        Response response = makeGetCall("/cases/ready-cases-reasons-counts", "application/vnd.sjp.query.ready-cases-reasons-counts+json");
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        return Json.createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

}
