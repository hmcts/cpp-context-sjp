package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCases;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.time.LocalDate;

import javax.json.JsonObject;

public class CaseSearchResultHelper extends AbstractTestHelper {

    public static final String CASE_SEARCH_RESULTS_MEDIA_TYPE = "application/vnd.sjp.query.case-search-results+json";

    private final String assignmentNatureType = "for-magistrate-decision";
    private final CaseSjpHelper caseSjpHelper;
    private final String lastName;
    private final LocalDate dateOfBirth;

    public CaseSearchResultHelper(CaseSjpHelper caseSjpHelper) {
        this.caseSjpHelper = caseSjpHelper;
        this.lastName = "LLOYD";
        this.dateOfBirth = LocalDates.from("1980-07-15");

    }

    public void assignmentCreated()  {
        final JsonObject payload = createObjectBuilder()
                .add("domainObjectId", caseSjpHelper.getCaseId())
                .add("assignmentNatureType", assignmentNatureType)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("assignment.assignment-created"), payload);

        try (MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("assignment.assignment-created", eventEnvelope);
        }
    }

    public void assignmentDeleted()  {
        final JsonObject payload = createObjectBuilder()
                .add("domainObjectId", caseSjpHelper.getCaseId())
                .add("assignmentNatureType", assignmentNatureType)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("assignment.assignment-deleted"),
                payload);

        try (MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("assignment.assignment-deleted", eventEnvelope);
        }
    }

    public void verifyPersonNotFound(final String urn, final String lastName) {
        poll(searchCases(lastName))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[?(@.urn=='"+urn+"')]", hasSize(0))
                ));
    }

    public void verifyPleaReceivedDate() {
        poll(searchCases(caseSjpHelper.getCaseUrn()))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[0].pleaDate", notNullValue())
                ));
    }

    public void verifyNoPleaReceivedDate() {
        poll(searchCases(caseSjpHelper.getCaseUrn()))
                .until(status().is(OK), payload().isJson(
                        withoutJsonPath("$.results[0].pleaDate")
                ));
    }

    public void verifyWithdrawalRequestedDate() {
        poll(searchCases(caseSjpHelper.getCaseUrn()))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[0].withdrawalRequestedDate", notNullValue())
                ));
    }

    public void verifyNoWithdrawalRequestedDate() {
        poll(searchCases(caseSjpHelper.getCaseUrn()))
                .until(status().is(OK), payload().isJson(
                        withoutJsonPath("$.results[0].withdrawalRequestedDate")
                ));
    }


    public void verifyAssignment(final boolean assigned) {
        poll(searchCases(caseSjpHelper.getCaseUrn()))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results[0].urn", is(caseSjpHelper.getCaseUrn())),
                        withJsonPath("$.results[0].assigned", is(assigned)))));
    }


    public void verifyPersonInfoByUrn(final int expectedHits) {
        poll(searchCases(caseSjpHelper.caseUrn))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results", hasSize(expectedHits)))));
    }

    public void verifyPersonInfoByUrn() {
        verifyPersonInfo(caseSjpHelper.getCaseUrn(), lastName, dateOfBirth);
    }

    public void verifyPersonInfoByLastNameAndDateOfBirth(String lastName, LocalDate dateOfBirth) {
        verifyPersonInfo(lastName, lastName, dateOfBirth);
    }

    private void verifyPersonInfo(final String query, final String lastName, final LocalDate dateOfBirth) {
        poll(searchCases(query))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results[*]", hasItem(isJson(
                                allOf(
                                        withJsonPath("urn", equalTo(caseSjpHelper.getCaseUrn())),
                                        withJsonPath("lastName", equalTo(lastName)),
                                        withJsonPath("dateOfBirth", equalTo(LocalDates.to(dateOfBirth)))
                                )))))));
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
