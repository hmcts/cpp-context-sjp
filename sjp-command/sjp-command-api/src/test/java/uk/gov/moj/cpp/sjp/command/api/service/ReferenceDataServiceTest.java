package uk.gov.moj.cpp.sjp.command.api.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static java.util.UUID.randomUUID;
import static java.util.Objects.isNull;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private ReferenceDataService referenceDataService;

    final String courtHouseOUCode = "B01OK";
    final String courtHouseName = "Wimbledon Magistrates' Court";
    final String localJusticeAreaNationalCourtCode = "2577";
    final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final String OFFENCE_CODE = "CA03010";

    @Test
    public void shouldGetSessionCourt() {
        final JsonEnvelope expectedOrganisationUnitsQuery = argThat(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("referencedata.query.organisationunits"),
                        payloadIsJson(withJsonPath("$.oucode", equalTo(courtHouseOUCode)))));

        final JsonEnvelope organisationUnitsResponse = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("organisationunits", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("oucodeL3Name", courtHouseName)
                                .add("lja", localJusticeAreaNationalCourtCode)))
                        .build());

        when(requester.requestAsAdmin(expectedOrganisationUnitsQuery)).thenReturn(organisationUnitsResponse);

        final Optional<SessionCourt> sessionCourt = referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, envelope);

        assertThat(sessionCourt.isPresent(), equalTo(true));
        assertThat(sessionCourt.get().getCourtHouseName(), equalTo(courtHouseName));
        assertThat(sessionCourt.get().getLocalJusticeAreaNationalCourtCode(), equalTo(String.valueOf(localJusticeAreaNationalCourtCode)));
    }

    @Test
    public void shouldReturnEmptySessionCourtWhenNotFound() {
        final JsonEnvelope organisationUnitsResponse = envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("organisationunits", createArrayBuilder()).build());

        when(requester.requestAsAdmin(any())).thenReturn(organisationUnitsResponse);

        final Optional<SessionCourt> sessionCourt = referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, envelope);

        assertThat(sessionCourt.isPresent(), equalTo(false));
    }

    @Test
    public void shouldGetOffenceDetails() {

        final String offenceId = randomUUID().toString();
        final JsonObject offences = createObjectBuilder()
                .add("offenceId", offenceId)
                .add("cjsOffenceCode", OFFENCE_CODE)
                .add("aocpEligible", true)
                .add("aocpStandardPenalty", 100)
                .build();
        final JsonObject offenceResponsePayload = createObjectBuilder()
                .add("offences", createArrayBuilder().add(offences))
                .build();

        final Envelope responseJsonEnvelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("referencedataoffences.query.offences-list").build(), offenceResponsePayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(responseJsonEnvelope);

        final JsonObject result = referenceDataService.getOffenceDetail(envelope, OFFENCE_CODE);

        assertThat(result, is(offences));
        assertThat(result.getString("offenceId"), is(offenceId));
        assertThat(result.getBoolean("aocpEligible"), is(true));
        assertThat(result.getInt("aocpStandardPenalty"), is(100));
    }

    @Test
    public void shouldReturnNullWhenOffenceDoesNotExist() {

        final JsonObject offenceResponsePayload = createObjectBuilder()
                .add("offences", createArrayBuilder())
                .build();
        final Envelope responseJsonEnvelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("referencedataoffences.query.offences-list").build(), offenceResponsePayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(responseJsonEnvelope);

        final JsonObject result = referenceDataService.getOffenceDetail(envelope, OFFENCE_CODE);
        assertThat(isNull(result), is(true));
    }

}
