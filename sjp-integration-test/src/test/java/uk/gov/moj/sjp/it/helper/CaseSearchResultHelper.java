package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
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

import org.apache.commons.lang.RandomStringUtils;

public class CaseSearchResultHelper extends AbstractTestHelper {

    private static final String ADD_PERSON_INFO_DATA_TYPE = "application/vnd.sjp.add-person-info+json";
    public static final String CASE_SEARCH_RESULTS_MEDIA_TYPE = "application/vnd.sjp.query.case-search-results+json";

    private final String firstName;
    private final String lastName;
    private final String updatedLastName;
    private final String personId;
    private final LocalDate updatedDateOfBirth;
    private final LocalDate dateOfBirth;
    private final String assignmentNatureType = "for-magistrate-decision";
    private final CaseSjpHelper caseSjpHelper;

    public CaseSearchResultHelper(CaseSjpHelper caseSjpHelper) {
        this.caseSjpHelper = caseSjpHelper;
        this.personId = caseSjpHelper.getDefendantPersonId();
        this.firstName = RandomStringUtils.randomAlphabetic(12);
        this.lastName = RandomStringUtils.randomAlphabetic(12);
        this.updatedLastName = this.lastName + "updated";
        this.dateOfBirth = LocalDate.now().minusYears(40);
        this.updatedDateOfBirth = LocalDate.now().minusYears(20);
    }

    public void addPersonInfo() {

        final JsonObject payload = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("personId", personId)
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("dateOfBirth", LocalDates.to(dateOfBirth))
                .add("postCode", "CR0 1XG").build();

        makePostCall(getWriteUrl("/cases/" + caseSjpHelper.getCaseId() + "/add-person-info"), ADD_PERSON_INFO_DATA_TYPE, payload.toString());
    }

    public void updatePersonInfo()  {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseSjpHelper.getCaseId())
                .add("personId", personId)
                .add("lastName", updatedLastName)
                .add("dateOfBirth", LocalDates.to(updatedDateOfBirth))
                .add("address", createObjectBuilder().add("postCode", "CR0 1XG"))
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("people.personal-details-updated"),
                payload);

        try (MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("people.personal-details-updated", eventEnvelope);
        }
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

    public void verifyPersonInfoByUrn() {
        verifyPersonInfo(caseSjpHelper.getCaseUrn(), lastName, dateOfBirth);
    }

    public void verifyPersonInfoByLastNameAndDateOfBirth(String lastName, LocalDate dateOfBirth) {
        verifyPersonInfo(lastName, lastName, dateOfBirth);
    }

    private void verifyPersonInfo(final String query, final String lastName, final LocalDate dateOfBirth) {
        poll(searchCases(query))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results[0].urn", is(caseSjpHelper.getCaseUrn())),
                        withJsonPath("$.results[0].lastName", is(lastName)),
                        withJsonPath("$.results[0].dateOfBirth", is(LocalDates.to(dateOfBirth)))
                )));
    }

    public void verifyPersonNotFound(final String lastName) {
        poll(searchCases(lastName))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results", hasSize(0))
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



    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUpdatedLastName() {
        return updatedLastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public LocalDate getUpdatedDateOfBirth() {
        return updatedDateOfBirth;
    }
}
