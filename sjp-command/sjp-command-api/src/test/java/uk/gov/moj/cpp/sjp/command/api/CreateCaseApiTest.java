package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressWithPostcode;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildDefendantWithAddress;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildDefendantWithAddressAndOffencesWithAOCPEligibilityAndStandardPenalty;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildDefendantWithContactDetails;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildDefendantWithAddressAndOffences;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.service.ReferenceDataService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateCaseApiTest {

    private static final String COMMAND_NAME = "sjp.create-sjp-case";
    private static final String NEW_COMMAND_NAME = "sjp.command.create-sjp-case";
    private static final String CASE_URN = "TFL736699173";

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private CreateCaseApi createCaseApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldHandleCommand() {
        assertThat(CreateCaseApi.class, isHandlerClass(COMMAND_API)
                .with(method("createSjpCase").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldRenameCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(buildDefendantWithAddress(buildAddressWithPostcode("se11pj")), "defendant")
                .withPayloadOf(UUID.fromString("4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d"), "caseId")
                .withPayloadOf(CASE_URN, "urn")
                .build();

        final JsonObject transformedPayload = createObjectBuilder()
                .add("defendant", buildDefendantWithAddress(buildAddressWithPostcode("SE1 1PJ")))
                .add("caseId", "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d")
                .add("urn", CASE_URN)
                .build();
        createCaseApi.createSjpCase(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(transformedPayload));
    }

    @Test
    public void shouldReplaceBlankEmailsToNull(){
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(buildDefendantWithContactDetails(createObjectBuilder()
                        .add("home", "02031234567")
                        .add("mobile", "07777123123")
                        .add("email", "   ")
                        .add("email2", "  ")
                        .build()), "defendant")
                .withPayloadOf(UUID.fromString("4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d"), "caseId")
                .withPayloadOf(CASE_URN, "urn")
                .build();

        final JsonObject transformedPayload = createObjectBuilder()
                .add("defendant", buildDefendantWithContactDetails(createObjectBuilder()
                        .add("home", "02031234567")
                        .add("mobile", "07777123123")
                        .build()))
                .add("caseId", "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d")
                .add("urn", CASE_URN)
                .build();

        createCaseApi.createSjpCase(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(transformedPayload));
    }


    @Test
    public void shouldPopulateOffenceWithAOCPEligibilityAndStandardPenalty() {
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();
        final String cjsOffenceCode1 = randomAlphanumeric(5);
        final String cjsOffenceCode2 = randomAlphanumeric(5);

        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(buildDefendantWithAddressAndOffences(buildAddressWithPostcode("se11pj"), offenceId1, offenceId2,  cjsOffenceCode1, cjsOffenceCode2), "defendant")
                .withPayloadOf(UUID.fromString("4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d"), "id")
                .withPayloadOf(CASE_URN, "urn")
                .build();

        final JsonObject transformedPayload = createObjectBuilder()
                .add("defendant", buildDefendantWithAddressAndOffencesWithAOCPEligibilityAndStandardPenalty(buildAddressWithPostcode("SE1 1PJ"), offenceId1, offenceId2, cjsOffenceCode1, cjsOffenceCode2, true))
                .add("id", "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d")
                .add("urn", CASE_URN)
                .build();

        when(referenceDataService.getOffenceDetail(any(), any())).thenReturn(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpEligible", true)
                .add("aocpStandardPenalty", 100)
                .build());

        createCaseApi.createSjpCase(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(transformedPayload));
    }

    @Test
    public void shouldPopulateOffenceIfNoAOCPDetailsAvailable() {
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();
        final String cjsOffenceCode1 = randomAlphanumeric(5);
        final String cjsOffenceCode2 = randomAlphanumeric(5);
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(buildDefendantWithAddressAndOffences(buildAddressWithPostcode("se11pj"), offenceId1, offenceId2,  cjsOffenceCode1, cjsOffenceCode2), "defendant")
                .withPayloadOf(UUID.fromString("4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d"), "id")
                .withPayloadOf(CASE_URN, "urn")
                .build();

        final JsonObject transformedPayload = createObjectBuilder()
                .add("defendant", buildDefendantWithAddressAndOffencesWithAOCPEligibilityAndStandardPenalty(buildAddressWithPostcode("SE1 1PJ"), offenceId1, offenceId2, cjsOffenceCode1, cjsOffenceCode2, false))
                .add("id", "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d")
                .add("urn", CASE_URN)
                .build();

        when(referenceDataService.getOffenceDetail(any(), any())).thenReturn(createObjectBuilder()
                .add("id", randomUUID().toString())
                .build());

        createCaseApi.createSjpCase(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(transformedPayload));
    }
}
