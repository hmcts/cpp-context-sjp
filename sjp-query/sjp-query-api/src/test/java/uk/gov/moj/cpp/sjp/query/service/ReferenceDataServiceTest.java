package uk.gov.moj.cpp.sjp.query.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createArrayBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.exception.OffenceNotFoundException;

import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    private static final String OFFENCE_CODE = "CA03010";
    private static final String OFFENCE_DATE = "2017-11-07";
    private final JsonEnvelope queryJsonEnvelope = envelope().with(metadataWithRandomUUID("sjp.query.case-details")).build();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void shouldGetOffenceDefinition() {
        final JsonObject offenceDefinition = createObjectBuilder()
                .add("cjsOffenceCode", OFFENCE_CODE)
                .build();
        final JsonObject offenceDefinitionResponsePayload = createObjectBuilder()
                .add("offences", createArrayBuilder().add(offenceDefinition))
                .build();
        final JsonEnvelope responseJsonEnvelope = envelopeFrom(metadataWithRandomUUID("referencedataoffences.query.offences-list"), offenceDefinitionResponsePayload);

        when(requester.request(any())).thenReturn(responseJsonEnvelope);

        final JsonObject result = referenceDataService.getOffenceDefinition(OFFENCE_CODE, OFFENCE_DATE, queryJsonEnvelope);

        assertThat(result, is(offenceDefinition));
        verifyOffenceListQueried();
    }

    @Test
    public void shouldThrowOffenceNotFoundExceptionWhenOffenceDoesNotExist() {
        final JsonObject offenceDefinitionResponsePayload = createObjectBuilder()
                .add("offences", createArrayBuilder())
                .build();
        final JsonEnvelope responseJsonEnvelope = envelopeFrom(metadataWithRandomUUID("referencedataoffences.query.offences-list"), offenceDefinitionResponsePayload);

        when(requester.request(any())).thenReturn(responseJsonEnvelope);

        try {
            referenceDataService.getOffenceDefinition(OFFENCE_CODE, OFFENCE_DATE, queryJsonEnvelope);
            fail("OffenceNotFoundException exception expected");
        } catch (final OffenceNotFoundException e) {
            assertThat(e.getOffenceCode(), is(OFFENCE_CODE));
        }

        verifyOffenceListQueried();
    }

    private void verifyOffenceListQueried() {
        verify(requester).request(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(queryJsonEnvelope).withName("referencedataoffences.query.offences-list"),
                payloadIsJson(allOf(
                        withJsonPath("$.cjsoffencecode", is(OFFENCE_CODE)),
                        withJsonPath("$.date", is(OFFENCE_DATE))
                )))));
    }

    @Test
    public void shouldReturnReferralReasons() {
        final JsonEnvelope sourceEnvelope = envelopeFrom(metadataWithRandomUUID("query"), JsonValue.NULL);

        final JsonObject referralReason1 = createObjectBuilder().add("id", randomUUID().toString()).build();
        final JsonObject referralReason2 = createObjectBuilder().add("id", randomUUID().toString()).build();

        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.referral-reasons"),
                createObjectBuilder().add("referralReasons", createArrayBuilder()
                        .add(referralReason1)
                        .add(referralReason2))
                        .build());

        when(requestReferralReasons(sourceEnvelope)).thenReturn(queryResponse);

        final List<JsonObject> referralReasons = referenceDataService.getReferralReasons(sourceEnvelope);

        assertThat(referralReasons, containsInAnyOrder(referralReason1, referralReason2));
    }

    private Object requestReferralReasons(final JsonEnvelope sourceEnvelope) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(sourceEnvelope).withName("referencedata.query.referral-reasons"),
                payloadIsJson(notNullValue()))));
    }

    @Test
    public void shouldReturnWithdrawalReasons() {
        final JsonEnvelope sourceEnvelope = envelopeFrom(metadataWithRandomUUID("query"), JsonValue.NULL);

        final JsonObject withdrawalReason1 = createObjectBuilder().add("id", randomUUID().toString()).build();
        final JsonObject withdrawalReason2 = createObjectBuilder().add("id", randomUUID().toString()).build();

        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.offence-withdraw-request-reasons"),
                createObjectBuilder().add("offenceWithdrawRequestReasons", createArrayBuilder()
                        .add(withdrawalReason1)
                        .add(withdrawalReason2))
                        .build());

        when(requestWithdrawalReasons(sourceEnvelope)).thenReturn(queryResponse);

        final List<JsonObject> withdrawalReasons = referenceDataService.getWithdrawalReasons(sourceEnvelope);

        assertThat(withdrawalReasons, containsInAnyOrder(withdrawalReason1, withdrawalReason2));
    }

    private Object requestWithdrawalReasons(final JsonEnvelope sourceEnvelope) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(sourceEnvelope).withName("referencedata.query.offence-withdraw-request-reasons"),
                payloadIsJson(notNullValue()))));
    }
}
