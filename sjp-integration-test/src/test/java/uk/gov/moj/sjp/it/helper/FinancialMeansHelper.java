package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.getOnlinePlea;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;

import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.json.JSONObject;

public class FinancialMeansHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public FinancialMeansHelper() {
        messageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(
                "public.sjp.financial-means-updated", "public.sjp.case-update-rejected");
    }

    public void updateFinancialMeans(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/financial-means", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-financial-means+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public Response getFinancialMeans(final String defendantId) {
        final String resource = format("/defendant/%s/financial-means", defendantId);
        final String contentType = "application/vnd.sjp.query.financial-means+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
    }

    public String getFinancialMeans(final String defendantId, final Matcher<Object> jsonMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getFinancialMeans(defendantId).readEntity(String.class), jsonMatcher);
    }

    public String getEventFromPublicTopic(final Matcher<Object> jsonMatcher) {
        final String event = retrieveMessageAsJsonObject(messageConsumer).get().toString();
        assertThat(event, jsonMatcher);
        return event;
    }

    public void deleteFinancialMeans(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/financial-means", caseId, defendantId);
        final String contentType = "application/vnd.sjp.delete-financial-means+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public void pleadOnline(final String payload, UUID caseId, String defendantId) {
        String writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
        HttpClientUtil.makePostCall(writeUrl, "application/vnd.sjp.plead-online+json", payload, Response.Status.ACCEPTED);
    }

    public static String getOnlinePleaData(final String caseId, final Matcher<Object> jsonMatcher, final UUID userId) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> {
            final Response onlinePlea = getOnlinePlea(caseId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                return "{}";
            }
            return onlinePlea.readEntity(String.class);
        }, jsonMatcher);
    }

    @Override
    public void close() throws Exception {
        messageConsumer.close();
    }
}
