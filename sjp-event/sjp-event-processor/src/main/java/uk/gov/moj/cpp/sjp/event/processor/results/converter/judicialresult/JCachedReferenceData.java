package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.HashedMap;

public class JCachedReferenceData {

    public static final String FIXED_LIST_COLLECTION = "fixedListCollection";
    public static final String ID = "id";
    public static final String ELEMENTS = "elements";
    public static final String CODE = "code";
    public static final String VALUE = "value";

    private ReferenceDataService referenceDataService;
    private Map<UUID, JsonObject> withdrawalReasonsByIds = new HashedMap();
    private Map<String, JsonObject> resultIds = new HashedMap();
    private Map<UUID, String> referralReasonsByIds = new HashedMap();
    private Map<String, String> paymentTypes = new HashedMap();
    private Map<String, String> deductFromBenefitsReasonMap = new HashedMap();
    private Map<String, String> installmentsFrequencyMap = new HashedMap();
    private Map<String, BailStatus> bailStatusMap = new HashedMap();
    private Map<UUID, JsonObject> resultDefinitionMap = new HashedMap();
    private Map<String, JsonObject> verdictTypesMap = new HashedMap();

    @Inject
    public JCachedReferenceData(final ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public String getWithdrawalReason(final UUID withdrawalReasonId, final JsonEnvelope envelope) {
        if (MapUtils.isEmpty(withdrawalReasonsByIds)) {
            withdrawalReasonsByIds = referenceDataService.getWithdrawalReasons(envelope).stream()
                    .collect(toMap(withdrawalReason -> fromString(withdrawalReason.getString("id")), identity()));
        }
        return ofNullable(withdrawalReasonsByIds.get(withdrawalReasonId))
                .map(withdrawalReason -> withdrawalReason.getString("reasonCodeDescription"))
                .orElseThrow(() -> new NotFoundException(withdrawalReasonId.toString()));
    }

    public UUID getResultId(final String resultCode, final JsonEnvelope envelope) {
        if (MapUtils.isEmpty(resultIds)) {
            resultIds = referenceDataService.getResultIds(envelope).stream()
                    .collect(toMap(result -> result.getString("code"), identity()));
        }
        return ofNullable(resultIds.get(resultCode))
                .map(result -> result.getString("id"))
                .map(UUID::fromString)
                .orElseThrow(() -> new NotFoundException(resultCode));
    }

    public String getReferralReason(final UUID referralReasonId, final JsonEnvelope envelope) {
        if (MapUtils.isEmpty(referralReasonsByIds)) {
            referralReasonsByIds = referenceDataService.getReferralReasons(envelope)
                    .getJsonArray("referralReasons")
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .collect(toMap(referralReason -> UUID.fromString(referralReason.getString("id")), this::extractReferralReason));
        }
        return referralReasonsByIds.get(referralReasonId);
    }

    public String getPaymentType(final String paymentTypeCode, final JsonEnvelope envelope) {
        if (MapUtils.isEmpty(paymentTypes)) {
            paymentTypes = referenceDataService.getFixedList(envelope)
                    .getJsonArray(FIXED_LIST_COLLECTION)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .filter(e -> "d7d75420-aace-11e8-98d0-529269fb1459".equals(e.getString(ID)))
                    .flatMap(e -> e.getJsonArray(ELEMENTS).getValuesAs(JsonObject.class).stream())
                    .collect(toMap(e -> e.getString(CODE), e -> e.getString(VALUE)));
        }
        return paymentTypes.get(paymentTypeCode);
    }

    public Optional<String> getCreditorName(final String prosecutingAuthorityCode, final JsonEnvelope envelope) {
        return referenceDataService
                .getProsecutor(prosecutingAuthorityCode, envelope)
                .map(prosecutor -> prosecutor.getString("fullName"));
    }

    private String extractReferralReason(final JsonObject referralReason) {
        if (referralReason.containsKey("subReason")) {
            return String.format("%s (%s)", referralReason.getString("reason"), referralReason.getString("subReason"));
        } else {
            return referralReason.getString("reason");
        }
    }

    public String getDeductFromFundsReason(final String code, final JsonEnvelope envelope) {
        if (MapUtils.isEmpty(deductFromBenefitsReasonMap)) {
            deductFromBenefitsReasonMap = referenceDataService.getFixedList(envelope)
                    .getJsonArray(FIXED_LIST_COLLECTION)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .filter(e -> "1fa26fa6-da67-48f7-9bb4-9b55d825854e".equals(e.getString(ID)))
                    .flatMap(e -> e.getJsonArray(ELEMENTS).getValuesAs(JsonObject.class).stream())
                    .collect(toMap(e -> e.getString(CODE), e -> e.getString(VALUE)));
        }
        return deductFromBenefitsReasonMap.get(code);
    }

    public String getInstallmentFrequency(final String frequencyCode, final JsonEnvelope envelopeFrom) {

        if (MapUtils.isEmpty(installmentsFrequencyMap)) {
            installmentsFrequencyMap = referenceDataService.getFixedList(envelopeFrom)
                    .getJsonArray(FIXED_LIST_COLLECTION)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .filter(e -> "e555e078-5dd8-11e8-9c2d-fa7ae01bbebc".equals(e.getString(ID)))
                    .flatMap(e -> e.getJsonArray(ELEMENTS).getValuesAs(JsonObject.class).stream())
                    .collect(toMap(e -> e.getString(CODE), e -> e.getString(VALUE)));
        }
        return installmentsFrequencyMap.get(frequencyCode);

    }

    public BailStatus getBailStatus(final String bailStatusCode, final JsonEnvelope envelopeFrom) {
        if (MapUtils.isEmpty(bailStatusMap)) {
            bailStatusMap = referenceDataService
                    .getAllBailStatuses(envelopeFrom)
                    .stream()
                    .collect(toMap(BailStatus::getCode, identity()));
        }
        return bailStatusMap.get(bailStatusCode);
    }

    public JsonObject getResultDefinition(final UUID resultDefinitionId,
                                          final JsonEnvelope envelopeFrom,
                                          final LocalDate localDate) {
        if (resultDefinitionMap.get(resultDefinitionId) == null) {
            referenceDataService
                    .getAllResultDefinitions(envelopeFrom, localDate)
                    .getValuesAs(JsonObject.class)
                    .forEach(e -> resultDefinitionMap.put(UUID.fromString(e.getString("id")), e));
        }
        return resultDefinitionMap.get(resultDefinitionId);
    }

    public Optional<JsonObject> getVerdictForMagistrate(final String verdictType,
                                                        final JsonEnvelope envelopeFrom) {
        if (verdictTypesMap.get(verdictType) == null) {
            referenceDataService
                    .getAllVerdictTypes(envelopeFrom)
                    .forEach(e -> {
                        if (e.getString("jurisdiction").equals(JurisdictionType.MAGISTRATES.name())) {
                            verdictTypesMap.put(e.getString("verdictCode"), e);
                        }
                    });
        }
        return Optional.ofNullable(verdictTypesMap.get(verdictType));
    }

}
