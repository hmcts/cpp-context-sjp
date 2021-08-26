package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.JudicialResult.Builder;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DISCHARGE_FOR_DAY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DISCHARGE_FOR_MONTH;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DISCHARGE_FOR_WEEK;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DISCHARGE_FOR_YEAR;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_BACK_DUTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_COMPENSATION;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DDD_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DDP_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DDP_NOTIONAL_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DRIVING_LICENCE_NUMBER;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.FCOMP_MAJOR_CREDITOR;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LEA_REASON_FOR_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LEP_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.REASON_FOR_NO_COMPENSATION;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.D45;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.DDD;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.DDO;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.DDP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.DNP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.DPR;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.FCOMP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.FVEBD;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LEA;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LEN;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LEP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LPC;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.NCR;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.NONE;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DisqualifyEndorseDecision;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultPromptDurationHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;

public class DecisionResultAggregator {

    public static final Map<String, String> unitsMap = ImmutableMap.of(
            DISCHARGE_FOR_DAY, "Days",
            DISCHARGE_FOR_WEEK, "Weeks",
            DISCHARGE_FOR_MONTH, "Months",
            DISCHARGE_FOR_YEAR, "Years"
    );


    public static final String STARTED_AT = "startedAt";
    public static final String LABEL = "label";
    public static final String SESSION_ID = "sessionId";

    private static final int MAXIMUM_DECIMAL_PLACES = 2;
    public static final String OUTGOING_PROMPT_DATE_FORMAT = "dd/MM/yyyy";
    private static final String INCOMING_PROMPT_DATE_FORMAT = "yyyy-MM-dd";


    protected final JCachedReferenceData jCachedReferenceData;

    public DecisionResultAggregator(JCachedReferenceData jCachedReferenceData) {
        this.jCachedReferenceData = jCachedReferenceData;
    }

    protected Builder populateResultDefinitionAttributes(final UUID resultId,
                                                         final JsonEnvelope sjpSessionEnvelope) {
        final JsonObject sjpSessionPayload = sjpSessionEnvelope.payloadAsJsonObject();
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(sjpSessionPayload.getString(STARTED_AT, null));
        final LocalDate sessionStartDate = LocalDate.of(zonedDateTime.getYear(), zonedDateTime.getMonth(), zonedDateTime.getDayOfMonth());
        final UUID sessionId = fromString(sjpSessionPayload.getString(SESSION_ID));

        final JsonObject resultDefinition = jCachedReferenceData.getResultDefinition(resultId, envelopeFrom(sjpSessionEnvelope.metadata(), null), sessionStartDate);

        return JudicialResultHelper.populateResultDefinitionAttributes(resultId, sessionId, resultDefinition);
    }

    protected List<JudicialResult> pressRestriction(final PressRestriction pressRestriction,
                                                    final JsonEnvelope sjpSessionEnvelope,
                                                    final ZonedDateTime resultedOn) {
        UUID resultId;
        JsonObject resultDefinition;
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        if (pressRestriction != null) {

            if (Boolean.TRUE.equals(pressRestriction.getRequested())) {
                resultId = D45.getResultDefinitionId();
                resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
                judicialResultPrompts.add(getPrompt(JPrompt.D45_PRESS_RESTRICTION_APPLIED, resultDefinition)
                        .withJudicialResultPromptTypeId(JPrompt.D45_PRESS_RESTRICTION_APPLIED.getId())
                        .withValue(pressRestriction.getName())
                        .build());
            } else {
                resultId = DPR.getResultDefinitionId();
                resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
                judicialResultPrompts.add(getPrompt(JPrompt.DPR_PRESS_RESTRICTION_REVOKED, resultDefinition)
                        .withJudicialResultPromptTypeId(JPrompt.DPR_PRESS_RESTRICTION_REVOKED.getId())
                        .withValue(resultedOn.format(DATE_FORMAT))
                        .build());
            }

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .build());
        }

        return judicialResults;
    }

    protected List<JudicialResult> backDutyResult(final BigDecimal backDuty,
                                                  final JsonEnvelope sjpSessionEnvelope,
                                                  final ZonedDateTime resultedOn) {
        final UUID resultId = FVEBD.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        final List<JudicialResult> judicialResults = new ArrayList<>();

        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();
        if (backDuty != null && backDuty.doubleValue() > 0) {
            final JudicialResultPrompt backDutyAmount = getPrompt(JPrompt.AMOUNT_OF_BACK_DUTY, resultDefinition)
                    .withJudicialResultPromptTypeId(AMOUNT_OF_BACK_DUTY.getId())
                    .withValue(getCurrencyAmount(backDuty.toString()))
                    .build();
            resultPrompts.add(backDutyAmount);

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                            .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }
        return judicialResults;
    }

    protected List<JudicialResult> endorsementAndDisqualificationResults(final DisqualifyEndorseDecision endorsementDecision,
                                                                         final JsonEnvelope sjpSessionEnvelope,
                                                                         final ZonedDateTime resultedOn,
                                                                         final String driverNumber) {
        final List<JudicialResult> judicialResults = new ArrayList<>();

        final boolean endorsed = Boolean.TRUE.equals(endorsementDecision.getLicenceEndorsed());
        final boolean disqualification = Boolean.TRUE.equals(endorsementDecision.getDisqualification());

        if (endorsed) {
            judicialResults.addAll(licenceEndorsementResult(endorsementDecision, sjpSessionEnvelope, resultedOn, driverNumber));
        }

        if (disqualification) {
            judicialResults.addAll(disqualificationResult(endorsementDecision, sjpSessionEnvelope, resultedOn, driverNumber));
        }

        if(endorsed || disqualification) {
            addDriversLicenseResults(sjpSessionEnvelope, resultedOn, driverNumber, judicialResults);
        }

        return judicialResults;
    }

    private void addDriversLicenseResults(final JsonEnvelope sjpSessionEnvelope, final ZonedDateTime resultedOn, final String driverNumber, final List<JudicialResult> judicialResults) {
        final UUID lpcResultId = LPC.getResultDefinitionId();
        final JsonObject lpcResultDefinition = getResultDefinition(sjpSessionEnvelope, lpcResultId);
        judicialResults.add(populateResultDefinitionAttributes(lpcResultId, sjpSessionEnvelope)
                .withOrderedDate(resultedOn.format(DATE_FORMAT))
                .withResultText(lpcResultDefinition.getString(LABEL))
                .build());

        if (isNotBlank(driverNumber)) {
            final UUID dnpResultId = DNP.getResultDefinitionId();
            final JsonObject dnpResultDefinition = getResultDefinition(sjpSessionEnvelope, dnpResultId);
            judicialResults.add(populateResultDefinitionAttributes(dnpResultId, sjpSessionEnvelope)
                    .withOrderedDate(resultedOn.format(DATE_FORMAT))
                    .withResultText(dnpResultDefinition.getString(LABEL))
                    .build());
        } else {
            final UUID noneResultId = NONE.getResultDefinitionId();
            final JsonObject noneResultDefinition = getResultDefinition(sjpSessionEnvelope, noneResultId);
            judicialResults.add(populateResultDefinitionAttributes(noneResultId, sjpSessionEnvelope)
                    .withOrderedDate(resultedOn.format(DATE_FORMAT))
                    .withResultText(noneResultDefinition.getString(LABEL))
                    .build());
        }
    }

    protected List<JudicialResult> licenceEndorsementResult(final DisqualifyEndorseDecision disqualifyEndorseDecision,
                                                            final JsonEnvelope sjpSessionEnvelope,
                                                            final ZonedDateTime resultedOn,
                                                            final String driverNumber) {
        UUID resultId;
        JsonObject resultDefinition;
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        if (disqualifyEndorseDecision.getPenaltyPointsImposed() != null
                && disqualifyEndorseDecision.getPenaltyPointsReason() == null) {
            resultId = LEP.getResultDefinitionId();
            resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
            judicialResultPrompts.add(getPrompt(LEP_PENALTY_POINTS, resultDefinition)
                    .withJudicialResultPromptTypeId(LEP_PENALTY_POINTS.getId())
                    .withTotalPenaltyPoints(new BigDecimal(disqualifyEndorseDecision.getPenaltyPointsImposed().toString()))
                    .withValue(disqualifyEndorseDecision.getPenaltyPointsImposed().toString())
                    .build());

            judicialResultPrompts.add(getPrompt(DRIVING_LICENCE_NUMBER, resultDefinition)
                    .withJudicialResultPromptTypeId(DRIVING_LICENCE_NUMBER.getId())
                    .withValue(driverNumber)
                    .build());
        } else if (disqualifyEndorseDecision.getPenaltyPointsImposed() != null
                && disqualifyEndorseDecision.getPenaltyPointsReason() != null) {
            resultId = LEA.getResultDefinitionId();
            resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
            judicialResultPrompts.add(getPrompt(LEP_PENALTY_POINTS, resultDefinition)
                    .withJudicialResultPromptTypeId(LEP_PENALTY_POINTS.getId())
                    .withTotalPenaltyPoints(new BigDecimal(disqualifyEndorseDecision.getPenaltyPointsImposed().toString()))
                    .withValue(disqualifyEndorseDecision.getPenaltyPointsImposed().toString())
                    .build());

            final PenaltyPointsReason penaltyPointsReason = disqualifyEndorseDecision.getPenaltyPointsReason();
            judicialResultPrompts.add(getPrompt(LEA_REASON_FOR_PENALTY_POINTS, resultDefinition)
                    .withJudicialResultPromptTypeId(LEA_REASON_FOR_PENALTY_POINTS.getId())
                    .withValue(penaltyPointsReason.equals(PenaltyPointsReason.DIFFERENT_OCCASIONS) ? penaltyPointsReason.name() : disqualifyEndorseDecision.getAdditionalPointsReason())
                    .build());

            judicialResultPrompts.add(getPrompt(DRIVING_LICENCE_NUMBER, resultDefinition)
                    .withJudicialResultPromptTypeId(DRIVING_LICENCE_NUMBER.getId())
                    .withValue(driverNumber)
                    .build());
        } else { // disqualifyEndorseDecision.getPenaltyPointsImposed() == null
            resultId = LEN.getResultDefinitionId();
            resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        }

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

    @SuppressWarnings("squid:S1151")
    protected List<JudicialResult> disqualificationResult(final DisqualifyEndorseDecision disqualifyEndorseDecision,
                                                          final JsonEnvelope sjpSessionEnvelope,
                                                          final ZonedDateTime resultedOn,
                                                          final String driverNumber) {
        UUID resultId;
        JsonObject resultDefinition;
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        final DisqualificationType disqualificationType = disqualifyEndorseDecision.getDisqualificationType();
        final Optional<DisqualificationPeriod> disqualificationPeriod = ofNullable(disqualifyEndorseDecision.getDisqualificationPeriod());

        switch (disqualificationType) {
            default:
            case DISCRETIONARY:
                resultId = DDD.getResultDefinitionId();
                resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
                disqualificationPeriod.ifPresent(e -> judicialResultPrompts.add(getPromptByDuration(DDP_DISQUALIFICATION_PERIOD, resultDefinition, unitsMap.get(e.getUnit().name()))
                        .withJudicialResultPromptTypeId(DDD_DISQUALIFICATION_PERIOD.getId())
                        .withValue(joinUnit(e)) // though the duration is there on the prompt we just concatenate the unit as CC does that.
                        .build()));
                break;
            case POINTS:
                resultId = DDP.getResultDefinitionId();
                resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
                disqualificationPeriod.ifPresent(e -> judicialResultPrompts.add(getPromptByDuration(DDP_DISQUALIFICATION_PERIOD, resultDefinition, unitsMap.get(e.getUnit().name()))
                        .withJudicialResultPromptTypeId(DDP_DISQUALIFICATION_PERIOD.getId())
                        .withValue(joinUnit(e))
                        .build()));

                if (disqualifyEndorseDecision.getNotionalPenaltyPoints() != null) {
                    judicialResultPrompts.add(getPrompt(DDP_NOTIONAL_PENALTY_POINTS, resultDefinition)
                            .withJudicialResultPromptTypeId(DDP_NOTIONAL_PENALTY_POINTS.getId())
                            .withValue(disqualifyEndorseDecision.getNotionalPenaltyPoints().toString())
                            .build());
                }
                break;
            case OBLIGATORY:
                resultId = DDO.getResultDefinitionId();
                resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
                disqualificationPeriod.ifPresent(e -> judicialResultPrompts.add(getPromptByDuration(DDP_DISQUALIFICATION_PERIOD, resultDefinition, unitsMap.get(e.getUnit().name()))
                        .withJudicialResultPromptTypeId(DDP_DISQUALIFICATION_PERIOD.getId())
                        .withValue(joinUnit(e))
                        .build()));
                break;
        }

        ofNullable(driverNumber)
                .ifPresent(dn -> judicialResultPrompts.add(getPrompt(DRIVING_LICENCE_NUMBER, resultDefinition)
                        .withJudicialResultPromptTypeId(DRIVING_LICENCE_NUMBER.getId())
                        .withValue(dn)
                        .build()));

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .withDurationElement(new JudicialResultPromptDurationHelper().populate(judicialResultPrompts,
                                getSessionDateTime(sjpSessionEnvelope),
                                resultDefinition).orElse(null))
                        .build());

        return judicialResults;
    }

    private String joinUnit(final DisqualificationPeriod e) {
        return String.format("%s %s", e.getValue().toString(), unitsMap.get(e.getUnit().name()));
    }


    protected String joinUnit(final DischargePeriod dischargedFor) {
        return String.format("%s %s", String.valueOf(dischargedFor.getValue()), unitsMap.get(dischargedFor.getUnit().name()));
    }

    protected JudicialResultPrompt.Builder getPrompt(final JPrompt jsPrompt,
                                                     final JsonObject resultDefinition) {
        return JudicialResultHelper.populatePromptDefinitionAttributes(jsPrompt, resultDefinition);
    }

    protected JudicialResultPrompt.Builder getPromptByDuration(final JPrompt jsPrompt,
                                                               final JsonObject resultDefinition,
                                                               final String duration) {
        return JudicialResultHelper.populatePromptDefinitionAttributesBasedOnDuration(jsPrompt, resultDefinition, duration);
    }

    @SuppressWarnings("squid:S1151")
    protected List<JudicialResult> compensationResult(final BigDecimal compensation,
                                                      final JsonEnvelope sjpSessionEnvelope,
                                                      final ZonedDateTime resultedOn,
                                                      final String prosecutingAuthority) {
        final UUID resultId = FCOMP.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        if (compensation != null && compensation.doubleValue() > 0) {
            judicialResultPrompts.add(getPrompt(AMOUNT_OF_COMPENSATION, resultDefinition)
                    .withJudicialResultPromptTypeId(AMOUNT_OF_COMPENSATION.getId())
                    .withValue(getCurrencyAmount(compensation.toString()))
                    .build());

            jCachedReferenceData.getCreditorName(prosecutingAuthority, sjpSessionEnvelope)
                    .ifPresent(value -> judicialResultPrompts.add(getPrompt(FCOMP_MAJOR_CREDITOR, resultDefinition)
                            .withJudicialResultPromptTypeId(FCOMP_MAJOR_CREDITOR.getId())
                            .withValue(value)
                            .build()));

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }

        return judicialResults;
    }

    @SuppressWarnings("squid:S1151")
    protected List<JudicialResult> noCompensationReasonResult(final String noCompensationReason,
                                                              final JsonEnvelope sjpSessionEnvelope,
                                                              final ZonedDateTime resultedOn) {
        final UUID resultId = NCR.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        if (noCompensationReason != null) {
            judicialResultPrompts.add(getPrompt(REASON_FOR_NO_COMPENSATION, resultDefinition)
                    .withJudicialResultPromptTypeId(REASON_FOR_NO_COMPENSATION.getId())
                    .withValue(noCompensationReason)
                    .build());

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .build());
        }

        return judicialResults;
    }

    protected String getResultText(final List<JudicialResultPrompt> judicialResultPrompts,
                                   final String resultLabel) {
        return JudicialResultHelper.getResultText(judicialResultPrompts, resultLabel);
    }

    protected LocalDate getSessionDate(final JsonEnvelope sjpSessionEnvelope) {
        final JsonObject sjpSessionPayload = sjpSessionEnvelope.payloadAsJsonObject();
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(sjpSessionPayload.getString(STARTED_AT, null));
        return LocalDate.of(zonedDateTime.getYear(), zonedDateTime.getMonth(), zonedDateTime.getDayOfMonth());
    }

    protected ZonedDateTime getSessionDateTime(final JsonEnvelope sjpSessionEnvelope) {
        final JsonObject sjpSessionPayload = sjpSessionEnvelope.payloadAsJsonObject();
        return ZonedDateTime.parse(sjpSessionPayload.getString(STARTED_AT, null));
    }


    protected JsonObject getResultDefinition(final JsonEnvelope sjpSessionEnvelope, final UUID resultId) {
        final LocalDate sessionStartDate = getSessionDate(sjpSessionEnvelope);
        return jCachedReferenceData.getResultDefinition(resultId, envelopeFrom(sjpSessionEnvelope.metadata(), null), sessionStartDate);
    }

    @SuppressWarnings("squid:S1151")
    protected List<JudicialResultPrompt> getCheckJudicialPromptsEmpty(final List<JudicialResultPrompt> resultPrompts) {
        return !resultPrompts.isEmpty() ? resultPrompts : null;
    }

    protected String getCurrencyAmount(final String amountValue){
        if(isNotBlank(amountValue)) {
            final BigDecimal amount = new BigDecimal(amountValue).setScale(MAXIMUM_DECIMAL_PLACES, ROUND_DOWN);
            if(amount.doubleValue() > 0) {
                return "£" + amount.toString();
            }
        }
        return amountValue;
    }

    @SuppressWarnings("squid:S00112")
    protected String restructureDate(final String value) {
        try {
            final LocalDate dateValue = LocalDate.parse(value, DateTimeFormatter.ofPattern(INCOMING_PROMPT_DATE_FORMAT));
            return dateValue.format(DateTimeFormatter.ofPattern(OUTGOING_PROMPT_DATE_FORMAT));
        } catch (DateTimeParseException parseException) {
            throw new RuntimeException(String.format("invalid format for incoming date prompt value: %s", value), parseException);
        }
    }

    protected void setFinalOffence(final DecisionAggregate decisionAggregate, final UUID offenceId, final List<JudicialResult> judicialResults) {
        judicialResults.stream()
                .map(JudicialResult::getCategory)
                .filter(FINAL::equals)
                .findFirst()
                .ifPresent(e-> decisionAggregate.putFinalOffence(offenceId, true));
    }

}
