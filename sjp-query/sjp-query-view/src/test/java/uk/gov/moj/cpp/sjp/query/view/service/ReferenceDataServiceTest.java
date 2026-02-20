package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;


    @Test
    public void shouldRequestProsecutors() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(prosecutorsResponseEnvelope());
        final Optional<JsonArray> prosecutors = referenceDataService.getProsecutorsByProsecutorCode("TFL");

        if (!prosecutors.isPresent()) {
            fail("no prosecutors found");
        }

        verify(requester).requestAsAdmin(argThat(jsonEnvelope(
                metadata().withName("referencedata.query.prosecutors"),
                payloadIsJson(withJsonPath("$.prosecutorCode", equalTo("TFL")))
        )));

        prosecutors.ifPresent(prosecutorList -> {
            assertEquals(1, prosecutorList.size());
            final JsonObject prosecutor = prosecutorList.getJsonObject(0);
            assertEquals("TFL", prosecutor.getString("shortName"));
            assertEquals("Transport for London", prosecutor.getString("fullName"));
        });

    }

    @Test
    public void shouldRequestAllProsecutors() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(prosecutorsResponseEnvelope());
        final Optional<JsonArray> prosecutors = referenceDataService.getAllProsecutors();

        if (!prosecutors.isPresent()) {
            fail("no prosecutors found");
        }

        verify(requester).requestAsAdmin(argThat(jsonEnvelope().withMetadataOf(
                metadata().withName("referencedata.query.prosecutors"))
        ));

        prosecutors.ifPresent(prosecutorList -> {
            assertEquals(1, prosecutorList.size());
            final JsonObject prosecutor = prosecutorList.getJsonObject(0);
            assertEquals("TFL", prosecutor.getString("shortName"));
            assertEquals("Transport for London", prosecutor.getString("fullName"));
        });

    }

    @Test
    public void shouldRequestOffenceDetails() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(offenceDataEnvelope());
        final Optional<JsonObject> offenceData = referenceDataService.getOffenceData("CA03013");

        if (!offenceData.isPresent()) {
            fail("no offence data found");
        }

        verify(requester).requestAsAdmin(argThat(jsonEnvelope(
                metadata().withName("referencedataoffences.query.offences-list"),
                payloadIsJson(withJsonPath("$.cjsoffencecode", equalTo("CA03013")))
        )));

        offenceData.ifPresent(offence -> {
            assertEquals("legislation", offence.getString("legislation"));
            assertEquals("PS90010", offence.getString("cjsoffencecode"));
        });

    }

    @Test
    public void shouldRequestReferralReasons() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(referralReasonsEnvelope());
        final JsonArray referralReasons = referenceDataService.getReferralReasons();

        if (referralReasons.isEmpty()) {
            fail("no referral reasons data found");
        }

        verify(requester).requestAsAdmin(argThat(jsonEnvelope().withMetadataOf(metadata().withName("referencedata.query.referral-reasons"))));

        assertEquals("Sections 135", referralReasons.getJsonObject(0).getString("reason"));
    }

    @Test
    public void shouldRequestRegionalOrganisations() {
        final JsonObject payload = createObjectBuilder().add("regionalOrganisations", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("seqNum", 10)
                        .add("regionName", "London")
                        .add("cbwaEnforcerEmail", "lccccpbw@justice.gov.uk;")
                )).build();
        final JsonEnvelope response = regionalOrganisationsEnvelope(payload);
        when(requester.request(any())).thenReturn(response);

        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(regionsEnvelope());

        assertThat(regionalOrganisations, hasSize(1));
        assertThat(regionalOrganisations.get(0).getId(), notNullValue());
        assertThat(regionalOrganisations.get(0).getSeqNum(), equalTo(10));
        assertThat(regionalOrganisations.get(0).getRegionName(), equalTo("London"));
        assertThat(regionalOrganisations.get(0).getCbwaEnforcerEmail(), equalTo("lccccpbw@justice.gov.uk;"));
    }

    @Test
    public void shouldRequestRegionalOrganisationsAndHandleEmptyRegionalOrganisations() {
        final JsonObject payload = createObjectBuilder().add("regionalOrganisations", createArrayBuilder()).build();
        final JsonEnvelope response = regionalOrganisationsEnvelope(payload);
        when(requester.request(any())).thenReturn(response);

        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(regionsEnvelope());

        assertThat(regionalOrganisations, empty());
    }

    @Test
    public void shouldRequestRegionalOrganisationsAndHandleNullResponse() {
        final JsonEnvelope response = regionalOrganisationsEnvelope(JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);

        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(regionsEnvelope());

        assertThat(regionalOrganisations, empty());
    }

    @Test
    public void shouldRequestRegionalOrganisationsAndHandleEmptyResponse() {
        final JsonObject payload = createObjectBuilder().build();
        final JsonEnvelope response = regionalOrganisationsEnvelope(payload);
        when(requester.request(any())).thenReturn(response);

        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(regionsEnvelope());

        assertThat(regionalOrganisations, empty());
    }

    @Test
    public void shouldRequestRegionalOrganisationsAndHandleOptionalFields() {
        final JsonObject payload = createObjectBuilder().add("regionalOrganisations", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("regionName", "North West")
                )).build();
        final JsonEnvelope response = regionalOrganisationsEnvelope(payload);
        when(requester.request(any())).thenReturn(response);

        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(regionsEnvelope());

        assertThat(regionalOrganisations, hasSize(1));
        assertThat(regionalOrganisations.get(0).getId(), notNullValue());
        assertThat(regionalOrganisations.get(0).getRegionName(), equalTo("North West"));
    }

    private JsonEnvelope prosecutorsResponseEnvelope() {
        return envelopeFrom(
                metadataBuilder().
                        withName("referencedata.query.prosecutors").
                        withId(randomUUID()),
                createReader(getClass().getClassLoader().
                        getResourceAsStream("prosecutors-ref-data.json")).
                        readObject()
        );
    }

    private JsonEnvelope offenceDataEnvelope() {
        return envelopeFrom(
                metadataBuilder().
                        withName("referencedata.query.offence").
                        withId(randomUUID()),
                createReader(getClass().getClassLoader().
                        getResourceAsStream("offence-ref-data.json")).
                        readObject()
        );
    }

    private JsonEnvelope referralReasonsEnvelope() {
        return envelopeFrom(
                metadataBuilder().
                        withName("referencedata.query.referral-reasons").
                        withId(randomUUID()),
                createReader(getClass().getClassLoader().
                        getResourceAsStream("referral-reasons-data.json")).
                        readObject()
        );
    }

    private JsonEnvelope regionalOrganisationsEnvelope(final JsonObject payload) {
        return regionalOrganisationsEnvelope((JsonValue) payload);
    }

    private JsonEnvelope regionalOrganisationsEnvelope(final JsonValue payload) {
        return envelopeFrom(metadataBuilder().
                        withName("referencedata.query.regional-organisations").
                        withId(randomUUID()),
                payload
        );
    }

    private JsonEnvelope regionsEnvelope() {
        return envelopeFrom(metadataBuilder()
                        .withName("referencedata.query.regional-organisations")
                        .withId(randomUUID()),
                createObjectBuilder()
        );
    }
}
