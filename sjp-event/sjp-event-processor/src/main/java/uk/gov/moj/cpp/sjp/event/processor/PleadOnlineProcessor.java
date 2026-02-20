package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;


import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;

@ServiceComponent(EVENT_PROCESSOR)
public class PleadOnlineProcessor {

    private static final String ADDRESS = "address";
    private static final String PERSONAL_DETAILS = "personalDetails";
    private static final String EMPLOYER = "employer";

    private static final String LEGAL_ENTITY = "legalEntityDefendant";
    private static final String POSTCODE = "postcode";

    private static final Map<String, List<String>> PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS = singletonMap(
            "FinancialMeansRequiredWhenPleadingGuilty",
            singletonList("Financial Means are required when you are pleading GUILTY"));

    private static final Set PROHIBITED_CASE_STATES = new HashSet<>(Arrays.asList(COMPLETED.name(),
            REFERRED_FOR_COURT_HEARING.name()));
    private static final Map<String, List<String>> CASE_HAS_BEEN_REVIEWED = singletonMap(
            "CaseAlreadyReviewed",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> CASE_ADJOURNED_POST_CONVICTION = singletonMap(
            "CaseAdjournedPostConviction",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> PLEA_ALREADY_SUBMITTED = singletonMap(
            "PleaAlreadySubmitted",
            singletonList("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));
    public static final String OFFENCES = "offences";
    public static final String FINANCIAL_MEANS = "financialMeans";

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles("public.prosecutioncasefile.sjp-plead-online")
    public void pleadOnline(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        Map<String, List<String>> validationErrors = validate(payload);
        checkValidationErrors(validationErrors);

        final JsonObject caseDetail = getCaseDetail(envelope);
        validationErrors = validateCase(caseDetail);

        checkValidationErrors(validationErrors);
        final JsonObjectBuilder pleaOnlineObjectBuilder = createObjectBuilderWithFilter(payload, field -> !asList(PERSONAL_DETAILS, EMPLOYER).contains(field));
        if (payload.containsKey(PERSONAL_DETAILS))  {
            pleaOnlineObjectBuilder.add(PERSONAL_DETAILS, replacePostcodeInPayload(payload, PERSONAL_DETAILS));
        }

        if (payload.containsKey(EMPLOYER)) {
            pleaOnlineObjectBuilder.add(EMPLOYER, replacePostcodeInPayload(payload, EMPLOYER));
        }

        if (payload.containsKey(LEGAL_ENTITY)) {
            pleaOnlineObjectBuilder.add(LEGAL_ENTITY, replacePostcodeInPayload(payload, LEGAL_ENTITY));
        }

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.plead-online").build(),
                pleaOnlineObjectBuilder.build()));

    }

    /**
     * Rules: - Financial Means are mandatory when Plea is GUILTY
     */
    @SuppressWarnings("squid:S4274")
    private Map<String, List<String>> validate(final JsonObject pleadOnline) {
        assert isEmpty(pleadOnline.getJsonArray(OFFENCES)) || pleadOnline.getJsonArray(OFFENCES).size() == 1 : "supports just single offence";

        final boolean anyGuiltyPlea = pleadOnline.getJsonArray(OFFENCES).stream().map(o -> (JsonObject) o)
                .map(offence -> offence.getString("plea", null))
                .anyMatch(plea -> plea.equals("GUILTY"));

        if (pleadOnline.containsKey(PERSONAL_DETAILS) && anyGuiltyPlea && hasEmptyFinancialMeans(pleadOnline)) {
            return PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS;
        }

        if (pleadOnline.containsKey(LEGAL_ENTITY) && anyGuiltyPlea && hasEmptyLegalEntityFinancialMeans(pleadOnline)) {
            return PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS;
        }

        return emptyMap();
    }

    public Map<String, List<String>> validateCase(final JsonObject caseDetail) {
        if (caseStatusProhibited(caseDetail).equals(TRUE) ||
                FALSE.equals(checkCaseDetailField(caseDetail, "completed", FALSE)) ||
                FALSE.equals(checkCaseDetailField(caseDetail, "assigned", FALSE)) ||
                TRUE.equals(offenceHasPendingWithdrawal(caseDetail).equals(TRUE))) {
            return CASE_HAS_BEEN_REVIEWED;
        }
        if (checkCaseAdjournedTo(caseDetail) ||
                offenceWithConviction(caseDetail) ||
                TRUE.equals(offenceHasConvictionDate(caseDetail))) {
            return CASE_ADJOURNED_POST_CONVICTION;
        }

        return emptyMap();
    }

    private Boolean caseAlreadyPleaded(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("plea", null) != null);
    }

    private Boolean offenceHasConvictionDate(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("convictionDate", null) != null);
    }


    private Boolean offenceWithConviction(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("conviction", null) != null);
    }

    private Boolean offenceHasPendingWithdrawal(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .map(offence -> offence.getBoolean("pendingWithdrawal", FALSE))
                .anyMatch(TRUE::equals);
    }

    private Boolean checkCaseDetailField(final JsonObject caseDetail, final String fieldName, final Boolean fieldValue) {
        return Optional.of(caseDetail.getBoolean(fieldName, FALSE))
                .filter(fieldValue::equals)
                .isPresent();
    }

    private Boolean caseStatusProhibited(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("status", null))
                .filter(PROHIBITED_CASE_STATES::contains)
                .isPresent();
    }

    private Boolean checkCaseAdjournedTo(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("adjournedTo", null))
                .isPresent();
    }

    private static boolean hasEmptyFinancialMeans(final JsonObject pleadOnline) {
        return ! pleadOnline.containsKey(FINANCIAL_MEANS) ||
                ! pleadOnline.getJsonObject(FINANCIAL_MEANS).containsKey("benefits") ||
                ! pleadOnline.getJsonObject(FINANCIAL_MEANS).containsKey("income") ||
                StringUtils.isEmpty(pleadOnline.getJsonObject(FINANCIAL_MEANS).getString("employmentStatus", null));
    }

    private static boolean hasEmptyLegalEntityFinancialMeans(final JsonObject pleadOnline) {
        return ! pleadOnline.containsKey("legalEntityFinancialMeans") ||
                ! pleadOnline.getJsonObject("legalEntityFinancialMeans").containsKey("netTurnover");
    }

    private void checkValidationErrors(Map<String, List<String>> validationErrors) {
        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }
    }

    private JsonObject getCaseDetail(final JsonEnvelope envelope) {
        final JsonObject queryCasePayload = Json.createObjectBuilder()
                .add("caseId", envelope.payloadAsJsonObject().getString("caseId"))
                .build();

        final JsonEnvelope queryCaseEnvelope = JsonEnvelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.query.case").build(),
                queryCasePayload);

        return requester.requestAsAdmin(queryCaseEnvelope).payloadAsJsonObject();
    }

    private Stream<JsonObject> getOffences(final JsonObject caseDetail) {
        return caseDetail.getJsonObject("defendant")
                .getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class)
                .stream();
    }

    private JsonObjectBuilder replacePostcodeInPayload(final JsonObject payload, final String objectToUpdate) {
        final JsonObjectBuilder objectToUpdateBuilder = createObjectBuilderWithFilter(payload.getJsonObject(objectToUpdate),
                field -> !field.contains(ADDRESS));

        objectToUpdateBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(objectToUpdate).getJsonObject(ADDRESS)));

        return objectToUpdateBuilder;
    }

    public static JsonObject normalizePostcodeInAddress(final JsonObject addressJsonObject) {
        if (isNull(addressJsonObject)) {
            return null;
        }

        if (!addressJsonObject.containsKey(POSTCODE)) {
            return addressJsonObject;
        }

        final JsonObjectBuilder addressObjectBuilder = createObjectBuilderWithFilter(addressJsonObject, field -> !field.equals(POSTCODE));
        addressObjectBuilder.add(POSTCODE, normalizePostcode(addressJsonObject
                .getString(POSTCODE)));

        return addressObjectBuilder.build();
    }

    public static String normalizePostcode(final String postcode) {
        final String uppercaseTrimmed = postcode.toUpperCase().replaceAll("\\s", EMPTY);

        return new StringBuilder(uppercaseTrimmed)
                .insert(uppercaseTrimmed.length() - 3, SPACE)
                .toString();
    }
}
