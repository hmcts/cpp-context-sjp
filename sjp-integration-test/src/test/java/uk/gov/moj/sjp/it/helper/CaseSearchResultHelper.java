package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCases;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.Matcher;

public class CaseSearchResultHelper extends AbstractTestHelper {

    public static final String CASE_SEARCH_RESULTS_MEDIA_TYPE = "application/vnd.sjp.query.case-search-results+json";

    private final PersonalDetails personalDetails = new PersonalDetails("Mr","David", "LLOYD", LocalDates.from("1980-07-15"),
            "Male", "nationalInsuranceNumber",
            new Address("14 Tottenham Court Road", "London", "England", "UK", "W1T 1JY"),
            new ContactDetails(null, null, null)
    );
    private final String updatedLastName;
    private final LocalDate updatedDateOfBirth;
    private final String assignmentNatureType = "for-magistrate-decision";
    private final CaseSjpHelper caseSjpHelper;

    public CaseSearchResultHelper(CaseSjpHelper caseSjpHelper) {
        this.caseSjpHelper = caseSjpHelper;
        this.updatedLastName = this.personalDetails.getLastName() + "updated";
        this.updatedDateOfBirth = LocalDate.now().minusYears(20);
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

    public void verifyPersonInfo() {
        verifyPersonInfo(this.personalDetails, false);
    }

    public void verifyPersonInfo(final PersonalDetails personalDetails, final boolean includeContactsAndNiNumberFields) {
        List<Matcher> fieldMatchers = getCommonFieldMatchers(personalDetails);
        if (includeContactsAndNiNumberFields) {
            fieldMatchers = Stream.of(fieldMatchers, getContactsAndNiNumberMatchers(personalDetails))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        poll(getCaseById(caseSjpHelper.getCaseId()))
                .until(status().is(OK), payload().isJson(allOf(
                        fieldMatchers.toArray(new Matcher[fieldMatchers.size()])
                )));
    }

    private List<Matcher> getCommonFieldMatchers(final PersonalDetails personalDetails) {
        return Arrays.asList(
                withJsonPath("urn", equalTo(caseSjpHelper.getCaseUrn())),
                withJsonPath("$.defendant.personalDetails.title", equalTo(personalDetails.getTitle())),
                withJsonPath("$.defendant.personalDetails.firstName", equalTo(personalDetails.getFirstName())),
                withJsonPath("$.defendant.personalDetails.lastName", equalTo(personalDetails.getLastName())),
                withJsonPath("$.defendant.personalDetails.gender", equalTo(personalDetails.getGender())),
                withJsonPath("$.defendant.personalDetails.dateOfBirth", equalTo(LocalDates.to(personalDetails.getDateOfBirth()))),
                withJsonPath("$.defendant.personalDetails.address.address1", equalTo(personalDetails.getAddress().getAddress1())),
                withJsonPath("$.defendant.personalDetails.address.address2", equalTo(personalDetails.getAddress().getAddress2())),
                withJsonPath("$.defendant.personalDetails.address.address3", equalTo(personalDetails.getAddress().getAddress3())),
                withJsonPath("$.defendant.personalDetails.address.address4", equalTo(personalDetails.getAddress().getAddress4())),
                withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(personalDetails.getAddress().getPostcode()))
        );
    }

    private List<Matcher> getContactsAndNiNumberMatchers(final PersonalDetails personalDetails) {
        return Arrays.asList(
                withJsonPath("$.defendant.personalDetails.nationalInsuranceNumber", equalTo(personalDetails.getNationalInsuranceNumber())),
                withJsonPath("$.defendant.personalDetails.contactDetails.email", equalTo(personalDetails.getContactDetails().getEmail())),
                withJsonPath("$.defendant.personalDetails.contactDetails.home", equalTo(personalDetails.getContactDetails().getHome())),
                withJsonPath("$.defendant.personalDetails.contactDetails.mobile", equalTo(personalDetails.getContactDetails().getMobile()))
        );
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

    public String getUpdatedLastName() {
        return updatedLastName;
    }

    public LocalDate getUpdatedDateOfBirth() {
        return updatedDateOfBirth;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }
}
