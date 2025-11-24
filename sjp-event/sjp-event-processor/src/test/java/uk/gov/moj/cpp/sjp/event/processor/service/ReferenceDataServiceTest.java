package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementAreaNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.DocumentTypeAccess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    private static final String ACCOUNT_DIVISION_CODE = "accountDivisionCode";
    private static final String ENFORCING_COURT_CODE = "enforcingCourtCode";
    private static final String NATIONAL_PAYMENT_PHONE = "nationalPaymentPhone";
    private static final String PAYMENT_QUERIES_ADDRESS1 = "paymentQueriesAddress1";
    private static final String PAYMENT_QUERIES_ADDRESS2 = "paymentQueriesAddress2";
    private static final String PAYMENT_QUERIES_ADDRESS3 = "paymentQueriesAddress3";
    private static final String PAYMENT_QUERIES_ADDRESS4 = "paymentQueriesAddress4";
    private static final String PAYMENT_QUERIES_POSTCODE = "paymentQueriesPostcode";
    private static final String REMITTANCE_ADVICE_EMAIL_ADDRESS = "remittanceAdviceEmailAddress";

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

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

    @Test
    public void shouldReturnEthnicity() {
        final String ethnicityCode = "code";
        final JsonArray ethnicity = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
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


        final JsonObject prosecutor = referenceDataService.getProsecutors(TFL, envelope);
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
        final JsonObject responsePayload = createObjectBuilder().add("referralReasons", createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.referral-reasons"),
                responsePayload);

        when(requestReferralReasons()).thenReturn(queryResponse);

        final JsonObject referralReasons = referenceDataService.getReferralReasons(envelope);
        assertThat(referralReasons, is(responsePayload));
    }

    @Test
    public void shouldReturnDocumentsTypeAccess() {
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final JsonObject responsePayload = createObjectBuilder()
                .add("documentsTypeAccess", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", id1.toString())
                                .add("section", "section1"))
                        .add(createObjectBuilder()
                                .add("id", id2.toString())
                                .add("section", "section2")))
                .build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.get-all-document-type-access"),
                responsePayload);
        when(requestDocumentTypeAccess()).thenReturn(queryResponse);

        final List<DocumentTypeAccess> documentTypeAccess = referenceDataService.getDocumentTypeAccess(LocalDate.now(), envelope);

        assertThat(documentTypeAccess, containsInAnyOrder(
                new DocumentTypeAccess(id1, "section1"),
                new DocumentTypeAccess(id2, "section2")
        ));
    }

    @Test
    public void shouldReturnHearingTypes() {
        final JsonObject responsePayload = createObjectBuilder().add("hearingTypes", createArrayBuilder()).build();
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

        final JsonArray allResultDefinitions = referenceDataService.getAllResultDefinitions(envelope);

        assertThat(allResultDefinitions.getJsonObject(0).getString(FIELD_SHORT_CODE), is(WITHDRAWN_SHORT_CODE));
        assertThat(allResultDefinitions.getJsonObject(0).getString(FIELD_ID), is(WITHDRAWN_RESULT_ID));
    }

    @Test
    public void getLocalJusticeAreaByCodeShouldReturnEmptyWhenNullResponseIsReturnedFromReferenceData() {
        final JsonEnvelope referenceDataResponse = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
        when(requester.requestAsAdmin(any())).thenReturn(referenceDataResponse);

        var e = assertThrows(EnforcementAreaNotFoundException.class, () -> referenceDataService.getLocalJusticeAreaByCode(envelope, "localJusticeAreaNationalCourtCode"));
        assertThat(e.getMessage(), is("Could not find Local Justice Area by code: localJusticeAreaNationalCourtCode"));
    }

    @Test
    public void shouldReturnDvlaPenaltyPointNotificationEmailAddress() {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("seqNum", 10010)
                .add("orgName", "DVLA Penalty Point Notification")
                .add("orgType", "DVLA")
                .add("startDate", "2020-05-01")
                .add("emailAddress", "rehab@dvla.gov.uk");
        final JsonEnvelope referenceDataResponse = envelopeFrom(metadataWithRandomUUIDAndName(), payload);
        when(requester.requestAsAdmin(any())).thenReturn(referenceDataResponse);

        final Optional<String> dvlaEmailAddress = referenceDataService.getDvlaPenaltyPointNotificationEmailAddress(envelope);

        assertThat(dvlaEmailAddress.isPresent(), is(true));
        assertThat(dvlaEmailAddress.get(), equalTo("rehab@dvla.gov.uk"));
    }

    @Test
    public void shouldReturnEmptyWhenDvlaEmailIsNotPresentInReferenceData() {
        final JsonEnvelope referenceDataResponse = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
        when(requester.requestAsAdmin(any())).thenReturn(referenceDataResponse);

        final Optional<String> dvlaEmailAddress = referenceDataService.getDvlaPenaltyPointNotificationEmailAddress(envelope);

        assertThat(dvlaEmailAddress.isPresent(), is(false));
    }


    @Test
    public void shouldReturnEnforcementAreaByPostcode() {
        final String postcode = "CRO 2GE";
        final JsonObject enforcementArea = newEnforcementArea();

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), enforcementArea);

        when(enforcementAreaQueryByPostcode(postcode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByPostcode(postcode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(true));
        assertThat(actualEnforcementArea.get(), equalTo(enforcementArea));
    }

    @Test
    public void shouldReturnEmptyEnforcementAreaIfNotFoundByPostcode() {
        final String postcode = "CR0 2GE";
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);

        when(enforcementAreaQueryByPostcode(postcode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByPostcode(postcode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(false));
    }

    @Test
    public void shouldReturnEnforcementAreaByLocalJusticeAreaNationalCourtCode() {
        final String localJusticeAreaNationalCourtCode = "1000";
        final JsonObject enforcementArea = newEnforcementArea();

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), enforcementArea);

        when(enforcementAreaQueryByNationalCourtCode(localJusticeAreaNationalCourtCode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(true));
        assertThat(actualEnforcementArea.get(), equalTo(enforcementArea));
    }

    @Test
    public void shouldReturnEmptyEnforcementAreaIfNotFoundByLocalJusticeAreaNationalCourtCode() {
        final String localJusticeAreaNationalCourtCode = "1000";
        final JsonEnvelope enforcementArea = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);

        when(enforcementAreaQueryByNationalCourtCode(localJusticeAreaNationalCourtCode)).thenReturn(enforcementArea);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(false));
    }

    @Test
    public void shouldReturnVictimSurcharges() {
        ArgumentCaptor<DefaultEnvelope> envelopeCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);
        final String surchargeType = "Fine";
        final String surchargeLevel = "Adult";
        final JsonObject victimSurchargePayload = createObjectBuilder()
                .add("surchargeAmountMin", BigDecimal.valueOf(0))
                .add("surchargeAmountMax", BigDecimal.valueOf(2000))
                .add("surchargeFinePercentage", BigDecimal.valueOf(40))
                .build();
        final JsonObject responsePayload = createObjectBuilder()
                .add("victimSurcharges", createArrayBuilder().add(victimSurchargePayload))
                .build();
        when(requester.request(any())).thenReturn(envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload));

        final List<JsonObject> victimSurcharge = referenceDataService.getVictimSurcharges(envelope, requester, surchargeType, surchargeLevel);

        verify(requester).request(envelopeCaptor.capture());
        final DefaultEnvelope envelope = envelopeCaptor.getValue();
        final JsonObject expectedRequestPayload = createObjectBuilder()
                .add("surchargeType", surchargeType)
                .add("surchargeLevel", surchargeLevel)
                .build();
        assertThat(envelope.payload(), equalTo(expectedRequestPayload));
        assertThat(envelope.metadata().name(), is("referencedata.query.victim-surcharges"));

        assertThat(victimSurcharge.get(0), is(victimSurchargePayload));
        assertThat(victimSurcharge.size(), is(1));

    }

    private JsonEnvelope enforcementAreaQueryByPostcode(final String postcode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.enforcement-area.v2"),
                payloadIsJson(withJsonPath("$.postcode", equalTo(postcode))))));
    }

    private JsonEnvelope enforcementAreaQueryByNationalCourtCode(final String localJusticeAreaNationalCourtCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.enforcement-area.v2"),
                payloadIsJson(withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode))))));
    }

    private static JsonObject newEnforcementArea() {
        return Json.createObjectBuilder()
                .add(ACCOUNT_DIVISION_CODE, "100")
                .add(ENFORCING_COURT_CODE, "1000")
                .add(NATIONAL_PAYMENT_PHONE, "030 0790 9901")
                .add(PAYMENT_QUERIES_ADDRESS1, "London Collection and Compliance Centre")
                .add(PAYMENT_QUERIES_ADDRESS2, "HMCTS")
                .add(PAYMENT_QUERIES_ADDRESS3, "PO Box 31090")
                .add(PAYMENT_QUERIES_ADDRESS4, "London")
                .add(PAYMENT_QUERIES_POSTCODE, "SW1P 3WQ")
                .add(REMITTANCE_ADVICE_EMAIL_ADDRESS, "lcccBank@hmcts.gsi.gov.uk")
                .build();
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

    private Object requestEthnicity(final String ethnicityCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.ethnicities"),
                payloadIsJson(withJsonPath("$.code", equalTo(ethnicityCode))))));
    }

    private Object requestDocumentTypeAccess() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.get-all-document-type-access"),
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

    private JsonObject getAllResultsDefinitionJsonObject() {

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
