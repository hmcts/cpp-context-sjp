package uk.gov.moj.sjp.it.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder.defaultOffenceBuilder;
import static uk.gov.moj.sjp.it.helper.CaseProsecutingAuthorityHelper.getProsecutingAuthority;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest {

    @Test
    public void shouldMultiOffenceCaseBeCreatedWithEnterpriseId() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        final String offenceCode1 = "CA03010";
        final String offenceCode2 = "CA03011";

        final JsonObject offence1Definition = stubQueryOffencesByCode(offenceCode1);
        final JsonObject offence2Definition = stubQueryOffencesByCode(offenceCode2);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        final CreateCase.OffenceBuilder offence = createCase.getOffenceBuilder();

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertTrue(caseReceivedEvent.isPresent());
        assertThat(caseReceivedEvent.get().payloadAsJsonObject().getString("expectedDateReady"), is(createCase.getPostingDate().plusDays(NUMBER_DAYS_WAITING_FOR_PLEA).toString()));

        final JsonPath jsonResponse = CasePoller.pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("enterpriseId"), equalTo(createCase.getEnterpriseId()));
        assertThat(jsonResponse.get("status"), equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name()));
        assertThat(jsonResponse.get("defendant.id"), equalTo(defendant.getId().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(defendant.getTitle()));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(defendant.getFirstName()));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(defendant.getLastName()));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(defendant.getDateOfBirth().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(defendant.getGender().toString()));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(defendant.getNumPreviousConvictions()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(defendant.getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(defendant.getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(defendant.getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(defendant.getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address5"), equalTo(defendant.getAddressBuilder().getAddress5()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(defendant.getAddressBuilder().getPostcode()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.home"), equalTo(defendant.getContactDetailsBuilder().getHome()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.mobile"), equalTo(defendant.getContactDetailsBuilder().getMobile()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.email"), equalTo(defendant.getContactDetailsBuilder().getEmail()));
        assertThat(jsonResponse.get("defendant.personalDetails.nationalInsuranceNumber"), equalTo(defendant.getNationalInsuranceNumber()));

        assertOffenceData(jsonResponse, offence, offenceCode1, offence1Definition, true, false, 0);
        assertOffenceData(jsonResponse, offence, offenceCode2, offence2Definition, false, true, 1);

        assertThat(getProsecutingAuthority(caseId), is(prosecutingAuthority.name()));
    }

    @Test
    public void shouldSchemaValidationFailWhenEmailIsInvalid() {
        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(randomUUID(), TFL, newArrayList("CA03010"));
        final String email = "   ";
        final String email2 = "@b.co";
        createCase.getDefendantBuilder().getContactDetailsBuilder().withEmail(email).withEmail2(email2);

        final String response = CreateCase.createCaseForPayloadBuilder(createCase, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#/defendant/contactDetails: 2 schema violations found");
        Assert.assertThat(validationTrace, containsString(format("#/defendant/contactDetails/email2: string [%s] does not match pattern", email2)));
        Assert.assertThat(validationTrace, containsString(format("#/defendant/contactDetails/email: string [%s] does not match pattern", email)));
    }

    private void assertOffenceData(final JsonPath jsonResponse, final CreateCase.OffenceBuilder offence, final String offenceCode, final JsonObject offenceDefinition, boolean outOfTime, boolean notInEffect, final int index) {
        assertThat(jsonResponse.get(format("defendant.offences[%d].offenceSequenceNumber", index)), equalTo(index + 1));
        assertThat(jsonResponse.get(format("defendant.offences[%d].wording", index)), equalTo(offence.getOffenceWording()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].wordingWelsh", index)), equalTo(offence.getOffenceWordingWelsh()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].chargeDate", index)), equalTo(offence.getChargeDate().toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].startDate", index)), equalTo(offence.getOffenceCommittedDate().toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].offenceCode", index)), equalTo(offenceCode));
        assertThat(jsonResponse.get(format("defendant.offences[%d].titleWelsh", index)), equalTo(JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshoffencetitle").orElse(null)));
        assertThat(jsonResponse.get(format("defendant.offences[%d].legislation", index)), equalTo(offenceDefinition.getString("legislation")));
        assertThat(jsonResponse.get(format("defendant.offences[%d].legislationWelsh", index)), equalTo(JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshlegislation").orElse(null)));
        assertThat(jsonResponse.get(format("defendant.offences[%d].outOfTime", index)), equalTo(outOfTime));
        assertThat(jsonResponse.get(format("defendant.offences[%d].notInEffect", index)), equalTo(notInEffect));
    }

    private CreateCase.CreateCasePayloadBuilder createMultiOffenceCase(final UUID caseId, final ProsecutingAuthority prosecutingAuthority,
                                                                       final List<String> offenceCodes) {
        return defaultCaseBuilder()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilders(offenceCodes.stream()
                        .map(offenceCode -> defaultOffenceBuilder()
                                .withId(randomUUID())
                                .withLibraOffenceCode(offenceCode)
                                .withOffenceCommittedDate(now().minusMonths(4))
                                .withOffenceChargeDate(now())
                        ).collect(toList()))
                .withDefendantId(randomUUID());
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }
}
