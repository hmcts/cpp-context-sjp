package uk.gov.moj.sjp.it.helper;

import static org.awaitility.Awaitility.await;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.getOnlinePlea;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class FinancialMeansHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public static final String FINANCIAL_MEANS_DOCUMENT_TYPE = "FINANCIAL_MEANS";
    public static final String SJPN_DOCUMENT_TYPE = "SJPN";

    public FinancialMeansHelper() {
        messageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(
                "public.sjp.financial-means-updated", "public.sjp.case-update-rejected", "public.sjp.events.defendant-financial-means-deleted");
    }

    public FinancialMeansHelper(String eventName) {
        messageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(
                eventName);
    }

    public void updateFinancialMeans(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/financial-means", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-financial-means+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public void updateAllFinancialMeans(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/all-financial-means", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-all-financial-means+json";
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

    public static void assertDocumentDeleted(final CaseDocumentHelper caseDocumentHelper, final UUID userId, final List<String> fmiMaterials) {

        final Optional<String> materialIdFromQueryResult = new Poller(10, 1000L)
                .pollUntilFound(() -> {
                    final JsonObject caseDocument = caseDocumentHelper.findAllDocumentsForTheUser(userId);
                    final String materialIdFromQuery = caseDocument.getString("materialId");

                    for (final String materialId : fmiMaterials) {
                        if (materialIdFromQuery.equals(materialId)) {
                            return Optional.empty();
                        }
                    }

                    return Optional.of(materialIdFromQuery);
                });

        assertThat("A deleted material is present", materialIdFromQueryResult.isPresent(), is(true));
    }

    public void pleadOnline(final String payload, final UUID caseId, final String defendantId) {
        final String writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
        HttpClientUtil.makePostCall(writeUrl, "application/vnd.sjp.plead-online+json", payload, Response.Status.ACCEPTED);
    }

    public static String getOnlinePleaData(final String caseId, final Matcher<Object> jsonMatcher, final UUID userId, final String defendantId) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> {
            final Response onlinePlea = getOnlinePlea(caseId, defendantId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                return "{}";
            }
            return onlinePlea.readEntity(String.class);
        }, jsonMatcher);
    }

    @Override
    public void close() throws Exception {
        if (null != messageConsumer) {
            try {
                messageConsumer.close();
            } catch (JMSException e) {
                // do nothing
            }
        }
    }

    public List<String> associateCaseWithFinancialMeansDocuments(UUID userId, UUID caseId, CaseDocumentHelper caseDocumentHelper) {

        final int NUMBER_OF_FMI_DOCUMENTS = 2;
        List<String> fmiMaterials = new ArrayList<>();
        final UUID legalAdviserId = randomUUID();
        try {
            //Create a Non FinancialDocument...
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, SJPN_DOCUMENT_TYPE);

            //retain the materialID
            for (int i = 0; i < NUMBER_OF_FMI_DOCUMENTS; i++) {

                //Create Financial Documents for this case....with defined material ids from the test.
                final UUID documentId = randomUUID();
                final UUID materialId = randomUUID();
                caseDocumentHelper.addCaseDocument(userId, documentId, materialId, FINANCIAL_MEANS_DOCUMENT_TYPE);

                //Add to a list of Financial Means materials ids and raise a stub,stubbing the Materials command
                fmiMaterials.add(materialId.toString());
                MaterialStub.stubDeleteMaterial(materialId.toString());
            }
        } catch (Exception exception) {
            fail("An Exception has occured on the creation of case documents and the defendant");
        }
        return fmiMaterials;
    }

}
