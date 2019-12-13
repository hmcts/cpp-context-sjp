package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import static java.util.Arrays.asList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DecisionName.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.CODE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.COSTS_AND_SURCHARGE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.CREATED;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.DECISION_ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.FINANCIAL_IMPOSITION;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.NAME;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.OFFENCES;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.OFFENCE_DECISIONS;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.PAYMENT;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.RESULTS;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.SAVED_AT;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.SESSION_ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.SJP_SESSION_ID;
import static uk.gov.moj.cpp.sjp.event.CaseCompleted.EVENT_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.util.UUIDGenerator;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import java.util.*;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;

public class ResultingDecisionsConverter {

    private static final String ADJOURNED_AT = "adjournedAt";
    private final SjpViewStoreService sjpViewStoreService;
    private final AdjournConverter adjournConverter;
    private static ResultingDecisionsConverter instance;

    private static final Map<String, Converter> INSTANCE_MAP = new HashMap<>();
    private static final Set<String> FINANCIAL_PENALTY_CODES = new HashSet<>();

    private static final String RSJP = "RSJP";

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    static {
        INSTANCE_MAP.put("DISCHARGE", DischargeConverter.INSTANCE);
        INSTANCE_MAP.put("WITHDRAW", WithDrawConverter.INSTANCE);
        INSTANCE_MAP.put("DISMISS", DismissConverter.INSTANCE);
        INSTANCE_MAP.put("FINANCIAL_PENALTY", FinancialPenaltyConverter.INSTANCE);
        INSTANCE_MAP.put("REFERRED_TO_OPEN_COURT", ReferredToOpenCourtConverter.INSTANCE);
        INSTANCE_MAP.put("REFERRED_FOR_FUTURE_SJP_SESSION", ReferredForFutureSJPSessionConverter.INSTANCE);

        FINANCIAL_PENALTY_CODES.add("FCOMP");
        FINANCIAL_PENALTY_CODES.add("FVS");
        FINANCIAL_PENALTY_CODES.add("FCOST");
    }

    private ResultingDecisionsConverter() {
        this(SjpViewStoreService.getInstance(),
                ReferForCourtHearingConverter.getInstance(),
                AdjournConverter.getInstance());
    }

    public static ResultingDecisionsConverter getInstance() {
        if (instance == null) {
            instance = new ResultingDecisionsConverter();
        }
        return instance;
    }

    @VisibleForTesting
    ResultingDecisionsConverter(final SjpViewStoreService sjpViewStoreService,
                                final ReferForCourtHearingConverter referForCourtHearingConverter,
                                final AdjournConverter adjournConverter) {
        this.sjpViewStoreService = sjpViewStoreService;
        this.adjournConverter = adjournConverter;
        INSTANCE_MAP.put("REFER_FOR_COURT_HEARING", referForCourtHearingConverter);
    }

    public JsonEnvelope convert(final JsonEnvelope currentEvent,
                                final JsonObject decisionPayload) {
        final String caseId = currentEvent.asJsonObject().getString("caseId");

        final JsonObjectBuilder metadataJsonObjectBuilder =
                createObjectBuilderWithFilter(currentEvent.metadata().asJsonObject(),
                        field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, DecisionSaved.EVENT_NAME);

        List<JsonObject> allOffenceDecisions;
        if(EVENT_NAME.equals(currentEvent.metadata().name())) {// for completed events
            allOffenceDecisions = buildOffenceDecisions(decisionPayload);
        } else { //  adjourn events
            allOffenceDecisions = buildOffenceDecisionsForAdjourn(decisionPayload);
        }

        final JsonObjectBuilder payloadJsonObjectBuilder;
        String sessionId;
        if (!decisionPayload.containsKey(SJP_SESSION_ID) && hasRSJP(decisionPayload)) {
            sessionId = generateRandomUUID();
        } else {
            sessionId = decisionPayload.getString(SJP_SESSION_ID);
        }

        payloadJsonObjectBuilder = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(SESSION_ID, sessionId)
                .add(DECISION_ID, EVENT_NAME.equals(currentEvent.metadata().name()) ?
                        decisionPayload.getString(ID) : decisionPayload.getString(DECISION_ID))
                .add(SAVED_AT, EVENT_NAME.equals(currentEvent.metadata().name()) ?
                        decisionPayload.getString(CREATED) : decisionPayload.getString(ADJOURNED_AT))
                .add(OFFENCE_DECISIONS, createArrayBuilder().add(allOffenceDecisions.get(0)));

        if (allOffenceDecisions.size() == 2
                && null != allOffenceDecisions.get(1)
                && (allOffenceDecisions.get(1).containsKey(COSTS_AND_SURCHARGE) ||
                allOffenceDecisions.get(1).containsKey(PAYMENT))) {
            payloadJsonObjectBuilder.add(FINANCIAL_IMPOSITION, allOffenceDecisions.get(1));
        }

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), payloadJsonObjectBuilder);
    }

    private List<JsonObject> buildOffenceDecisionsForAdjourn(final JsonObject decisionPayload) {
        return asList(
                adjournConverter
                        .convert(decisionPayload,
                                null,
                                decisionPayload.getString("verdict", null)));
    }

    private List<JsonObject> buildOffenceDecisions(final JsonObject decisionPayload) {
        return decisionPayload
                .getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class)
                .stream()
                .map(e -> hasIdAttribute(e) ?
                        buildOffenceLevelDecision(e, decisionPayload) : buildCaseLevelDecision(e))
                .collect(Collectors.toList());
    }

    private JsonObject buildOffenceLevelDecision(final JsonObject offenceLevelObject, final JsonObject decisionPayload) {
        // loop through the result code and identity what the decision is
        final List<String> validDecisionCodes = getResultDecisions(offenceLevelObject);

        if (validDecisionCodes.isEmpty()
                && isFinancialPenaltySpecialCase(decisionPayload)) {
            validDecisionCodes.add(FINANCIAL_PENALTY);
        }

        if (validDecisionCodes.size() != 1) {
            throw new TransformationException("Had multiple decisions codes or empty");
        }

        final String decision = validDecisionCodes.get(0);

        return INSTANCE_MAP
                .get(decision)
                .convert(offenceLevelObject,
                        sjpViewStoreService.getPlea(getOffenceId(decisionPayload)),
                        decisionPayload.getString("verdict", null));

    }

    private JsonObject buildCaseLevelDecision(final JsonObject caseLevelObject) {
        return FinancialImpositionConverter.INSTANCE.convert(caseLevelObject, null, null);
    }

    private List<String> getResultDecisions(final JsonObject offenceLevelObject) {
        return offenceLevelObject
                .getJsonArray("results")
                .getValuesAs(JsonObject.class)
                .stream()
                .reduce(new ArrayList<>(), (aggregate, element) -> {
                    DecisionTypeCode
                            .getResultDecision(element.getString(CODE))
                            .ifPresent(aggregate::add);
                    return aggregate;
                }, (aggregate1, aggregate2) -> {
                    aggregate1.addAll(aggregate2);
                    return aggregate1;
                });
    }

    // only called when it does'nt map the typically decision codes
    private boolean isFinancialPenaltySpecialCase(final JsonObject decisionPayload) {
        return decisionPayload
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(e -> e.containsKey(RESULTS))
                .flatMap(e -> e.getJsonArray(RESULTS).getValuesAs(JsonObject.class).stream())
                .filter(e -> FINANCIAL_PENALTY_CODES.contains(e.getString(CODE)))
                .count() == 3;
    }

    private boolean hasIdAttribute(final JsonObject jsonObject) {
        return jsonObject.containsKey(ID);
    }

    private String getOffenceId(final JsonObject decisionPayload) {
        return decisionPayload
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(this::hasIdAttribute)
                .map(e -> e.getString("id"))
                .findFirst()
                .orElseThrow(() -> new TransformationException("Offence id cannot be null"));
    }


    @SuppressWarnings("squid:S134")
    private boolean hasRSJP(JsonObject decisionPayload) {
        if (!decisionPayload.containsKey(OFFENCES)) {return false;}

        List<JsonObject> offences = decisionPayload.getJsonArray(OFFENCES).getValuesAs(JsonObject.class);

        for (final JsonObject offence : offences) {
            if (offence.containsKey(RESULTS)) {
                List<JsonObject> results = offence.getJsonArray(RESULTS).getValuesAs(JsonObject.class);

                for (final JsonObject result : results) {
                    if (result.containsKey(CODE) && RSJP.equals(result.getString(CODE))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String generateRandomUUID() {
        return uuidGenerator.generateRandomUUID();
    }

    public void setUUIDGenerator(UUIDGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }


}
