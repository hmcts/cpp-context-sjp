package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.FixDefendantDetailsHelper;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.apache.tika.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FixDefendantIT extends BaseIntegrationTest {

    private FixDefendantDetailsHelper fixDefendantDetailsHelper = new FixDefendantDetailsHelper();

    private CaseSjpHelper caseSjpHelper = new CaseSjpHelper() {
        @Override
        protected void doAdditionalReadCallResponseVerification(JsonPath jsonRequest, JsonPath jsonResponse) {
        }
    };

    private CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper);

    @Before
    public void createSjpCaseAndVerifyInQueue() throws IOException {
        final String sjpCaseCreatedJson = IOUtils.toString(getClass().getResourceAsStream("/FixDefendantIT/sjp.events.sjp-case-created.json"))
                .replace("{caseId}", caseSjpHelper.getCaseId())
                .replace("{caseUrn}", caseSjpHelper.getCaseUrn())
                .replace("{personId}", UUID.randomUUID().toString())
                .replace("{defendantId}", UUID.randomUUID().toString())
                .replace("{offenceId}", UUID.randomUUID().toString());

        final JsonObject payload = Json.createReader(new StringReader(sjpCaseCreatedJson)).readObject();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.sjp-case-created")
                        .withStreamId(UUID.randomUUID())
                        .withVersion(1), payload);

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("sjp.event");
            producerClient.sendMessage("sjp.events.sjp-case-created", eventEnvelope);
        }

        caseSjpHelper.verifyCaseCreatedUsingId();
    }

    @Test
    public void verifyInitialSearchDetailsAndUpdateToDefendantDetails() throws IOException {

        JsonObject payload = FileUtil.givenPayload("/payload/sjp.fix-defendant-details.json");

        fixDefendantDetailsHelper.fixDefendantDetails(caseSjpHelper.getCaseId(), payload);

        PersonalDetails personalDetails = new PersonalDetails(
                payload.getString("title"),
                payload.getString("firstName"),
                payload.getString("lastName"),
                LocalDates.from(payload.getString("dateOfBirth")),
                payload.getString("gender"),
                payload.getString("nationalInsuranceNumber"),
                new Address(
                        payload.getJsonObject("address").getString("address1"),
                        payload.getJsonObject("address").getString("address2"),
                        payload.getJsonObject("address").getString("address3"),
                        payload.getJsonObject("address").getString("address4"),
                        payload.getJsonObject("address").getString("postcode")),
                new ContactDetails(
                        payload.getString("email"),
                        payload.getJsonObject("contactNumber").getString("home"),
                        payload.getJsonObject("contactNumber").getString("mobile"))
        );
        caseSearchResultHelper.verifyPersonInfo(personalDetails, true);

        final JsonPath updatedCase = caseSjpHelper.getCaseResponseUsingId();

        final String firstName = updatedCase.getString("defendant.personalDetails.firstName");
        final String lastName = updatedCase.getString("defendant.personalDetails.lastName");
        assertThat(firstName, is(payload.getString("firstName")));
        assertThat(lastName, is(payload.getString("lastName")));
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        caseSearchResultHelper.close();
        fixDefendantDetailsHelper.close();
    }

}
