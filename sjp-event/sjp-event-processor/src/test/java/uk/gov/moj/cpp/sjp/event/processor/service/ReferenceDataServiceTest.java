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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

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

    private final static String TFL = "TFL";

    private static final String FIELD_ID = "id";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_WELSH_LABEL = "welshLabel";
    private static final String FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT = "isAvailableForCourtExtract";
    private static final String FIELD_SHORT_CODE = "shortCode";
    private static final String FIELD_LEVEL = "level";
    private static final String FIELD_RANK = "rank";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_USER_GROUPS = "userGroups";
    private static final String PLACEHOLDER = "PLACEHOLDER";
    private static final String WITHDRAWN_RESULT_ID = "6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc";
    private static final String DISMISSED_RESULT_ID = "14d66587-8fbe-424f-a369-b1144f1684e3";
    private static final String WITHDRAWN_SHORT_CODE = "WDRNNOT";
    private static final String DISMISSED_SHORT_CODE = "DISM";
    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

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
        final JsonEnvelope response = responseProsecutor();
        when(requestProsecutor(TFL)).thenReturn(response);


        final JsonObject prosecutor = referenceDataService.getProsecutor(TFL, envelope);
        assertThat(prosecutor, is(response.payloadAsJsonObject()));
    }

    @Test
    public void shouldReturnProsecutorInEnglish() {
        when(requestProsecutor(TFL)).thenReturn(responseProsecutor());

        final String prosecutor = referenceDataService.getProsecutor(TFL, false, envelope);
        assertThat(prosecutor, is("Transport For London"));
    }

    @Test
    public void shouldReturnProsecutorInWelsh() {
        when(requestProsecutor(TFL)).thenReturn(responseProsecutor());

        final String prosecutor = referenceDataService.getProsecutor(TFL, true, envelope);
        assertThat(prosecutor, is("Transport For London - Welsh"));
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

    @Test
    public void shouldReturnHearingTypes() {
        final JsonObject responsePayload = createObjectBuilder().add("hearingTypes", Json.createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.hearing-types"),
                responsePayload);

        when(requestHearingTypes()).thenReturn(queryResponse);

        final JsonObject hearingTypes = referenceDataService.getHearingTypes(envelope);
        assertThat(hearingTypes, is(responsePayload));
    }


    @Test
    public void shouldReturnAllResultDefinitions() {

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), getAllResultsDefinitionJsonObject());

        when(requester.request(any())).thenReturn(queryResponse);

        final JsonArray allResultDefinitions = referenceDataService.getAllResultDefinitions(envelope, LocalDate.now());

        assertThat(allResultDefinitions.getJsonObject(0).getString(FIELD_SHORT_CODE), is(WITHDRAWN_SHORT_CODE));
        assertThat(allResultDefinitions.getJsonObject(0).getString(FIELD_ID), is(WITHDRAWN_RESULT_ID));
    }

    private JsonEnvelope organisationUnitsQuery(final String oucode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.organisationunits"),
                payloadIsJson(withJsonPath("$.oucode", equalTo(oucode))))));
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

    private Object requestProsecutor(final String prosecutingAuthority) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.prosecutors"),
                payloadIsJson(withJsonPath("$.prosecutorCode", equalTo(prosecutingAuthority))))));
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

    private JsonEnvelope responseProsecutor() {
        final JsonObject responsePayload = createObjectBuilder()
                .add("id", "prosecutorId")
                .add("prosecutors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("nameWelsh", "Transport For London - Welsh")
                                .add("fullName", "Transport For London")))
                .build();
        return envelopeFrom(
                metadataWithRandomUUID("referencedata.query.prosecutors"),
                responsePayload);
    }

    private Object requestHearingTypes() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.hearing-types"),
                payloadIsJson(notNullValue()))));
    }

    private final JsonObject getAllResultsDefinitionJsonObject() {

        final JsonObject withdrawnResultDefinitionJsonObject = createObjectBuilder()
                .add(FIELD_ID, WITHDRAWN_RESULT_ID)
                .add(FIELD_VERSION, PLACEHOLDER)
                .add(FIELD_LABEL, PLACEHOLDER)
                .add(FIELD_WELSH_LABEL, PLACEHOLDER)
                .add(FIELD_SHORT_CODE, WITHDRAWN_SHORT_CODE)
                .add(FIELD_LEVEL, PLACEHOLDER)
                .add(FIELD_USER_GROUPS, PLACEHOLDER)
                .add(FIELD_RANK, PLACEHOLDER)
                .add(FIELD_START_DATE, PLACEHOLDER)
                .add(FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT, PLACEHOLDER)
                .build();

        final JsonObject dismissedResultDefinitionJsonObject = createObjectBuilder()
                .add(FIELD_ID, DISMISSED_RESULT_ID)
                .add(FIELD_VERSION, PLACEHOLDER)
                .add(FIELD_LABEL, PLACEHOLDER)
                .add(FIELD_WELSH_LABEL, PLACEHOLDER)
                .add(FIELD_SHORT_CODE, DISMISSED_SHORT_CODE)
                .add(FIELD_LEVEL, PLACEHOLDER)
                .add(FIELD_USER_GROUPS, PLACEHOLDER)
                .add(FIELD_RANK, PLACEHOLDER)
                .add(FIELD_START_DATE, PLACEHOLDER)
                .add(FIELD_IS_AVAILABLE_FOR_COURT_EXTRACT, PLACEHOLDER)
                .build();

        return createObjectBuilder()
                .add("resultDefinitions", createArrayBuilder()
                        .add(withdrawnResultDefinitionJsonObject)
                        .add(dismissedResultDefinitionJsonObject)
                        .build())
                .build();
    }


}
