package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldReturnCountryForAPostcode() {
        final String actualCountry = "Wales";
        final String outwardCode = "WA11";
        final JsonObject responsePayload = createObjectBuilder().add("country", "Wales").build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);
        when(requestCountryByPostcode(outwardCode)).thenReturn(queryResponse);
        final String country = referenceDataService.getCountryByPostcode(outwardCode, envelope);
        assertThat(actualCountry, is(country));
    }

    @Test
    public void shouldReturnNationalityForCodeWhenFound() {
        final JsonObject nationality = createObjectBuilder()
                .add("isoCode", "nationalityCode")
                .build();
        mockCountryNationalityResponseAndAssertOnResult(
                nationality,
                is(of(nationality)));
    }

    @Test
    public void shouldHandleNationalitiesWithEmptyIsoCode() {
        mockCountryNationalityResponseAndAssertOnResult(
                createObjectBuilder()
                        .add("id", "22ef7a73-df50-4349-8c72-ca3b9ace6363")
                        .add("cjsCode", 0)
                        .build(),
                is(empty()));
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNationalityNotFoundForCode() {
        mockCountryNationalityResponseAndAssertOnResult(
                createObjectBuilder()
                        .add("isoCode", "foo")
                        .build(),
                is(empty()));
    }

    private void mockCountryNationalityResponseAndAssertOnResult(final JsonObject nationality, final Matcher nationalityMatcher) {

        final JsonArray nationalities = createArrayBuilder()
                .add(nationality)
                .build();
        final JsonObject responsePayload = createObjectBuilder().add("countryNationality", nationalities).build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);

        when(requestCountryNationalities()).thenReturn(queryResponse);

        final Optional<JsonObject> nationalityResult = referenceDataService.getNationality("nationalityCode", envelope);
        assertThat(nationalityResult, nationalityMatcher);
    }

    @Test
    public void shouldReturnEthnicity() {
        final String ethnicityCode = "code";
        final JsonArray ethnicity = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", UUID.randomUUID().toString())
                        .add("code", ethnicityCode))
                .build();

        final JsonObject responsePayload = createObjectBuilder()
                .add("ethnicities", ethnicity)
                .build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);

        when(requestEthnicity(ethnicityCode)).thenReturn(queryResponse);

        final Optional<JsonObject> ethnicityResult = referenceDataService.getEthnicity(ethnicityCode, envelope);
        assertThat(ethnicityResult.get(), is(ethnicity.getJsonObject(0)));
    }

    @Test
    public void shouldReturnProsecutor() {
        final JsonObject responsePayload = createObjectBuilder().add("id", "prosecutorId").build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.prosecutors"),
                responsePayload);
        when(requestProsecutor(ProsecutingAuthority.TFL)).thenReturn(queryResponse);

        final JsonObject prosecutor = referenceDataService.getProsecutor(ProsecutingAuthority.TFL, envelope);
        assertThat(prosecutor, is(responsePayload));
    }

    @Test
    public void shouldReturnReferralReasons() {
        final JsonObject responsePayload = createObjectBuilder().add("referralReasons", Json.createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.referral-reasons"),
                responsePayload);

        when(requestReferralReasons()).thenReturn(queryResponse);

        final JsonObject referralReasons = referenceDataService.getReferralReasons(envelope);
        assertThat(referralReasons, is(responsePayload));
    }

    @Test
    public void shouldReturnDocumentsMetaData() {
        final JsonObject responsePayload = createObjectBuilder().add("date", Json.createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.get-all-document-metadata"),
                responsePayload);

        final LocalDate date = LocalDate.now();
        when(requestDocumentMetadata()).thenReturn(queryResponse);

        final JsonObject documentsMetadata = referenceDataService.getDocumentMetadata(date, envelope);
        assertThat(documentsMetadata, is(responsePayload));
    }

    private Object requestEthnicity(final String ethnicityCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.ethnicities"),
                payloadIsJson(withJsonPath("$.code", equalTo(ethnicityCode))))));
    }

    private Object requestDocumentMetadata() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.get-all-document-metadata"),
                payloadIsJson(notNullValue()))));
    }

    private Object requestReferralReasons() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.referral-reasons"),
                payloadIsJson(notNullValue()))));
    }

    private Object requestProsecutor(final ProsecutingAuthority prosecutingAuthority) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.prosecutors"),
                payloadIsJson(withJsonPath("$.prosecutorCode", equalTo(prosecutingAuthority.name()))))));
    }

    private Object requestCountryNationalities() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.country-nationality"),
                payloadIsJson(notNullValue()))));
    }

    private JsonEnvelope requestCountryByPostcode(final String postCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.country-by-postcode"),
                payloadIsJson(withJsonPath("$.postCode", equalTo(postCode))))));
    }

}
