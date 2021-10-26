package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DURATION_VALUE_OF_CONDITIONAL_DISCHARGE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.AD;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.CD;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultPromptDurationHelper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DischargeDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    private CourtCentreConverter courtCentreConverter;

    @Inject
    public DischargeDecisionResultAggregator(final JCachedReferenceData cachedReferenceData) {
        super(cachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn,
                          final String driverNumber,
                          final String prosecutingAuthority) {

        final Discharge dischargeOffenceDecision = (Discharge) offenceDecision;
        final UUID offenceId = dischargeOffenceDecision.getOffenceDecisionInformation().getOffenceId();

        final List<JudicialResult> judicialResults = new ArrayList<>();
        decisionAggregate.putResults(offenceId, judicialResults);

        // dischargeResult
        judicialResults.addAll(dischargeResult(dischargeOffenceDecision, sjpSessionEnvelope, resultedOn));

        // backDutyResult
        judicialResults.addAll(backDutyResult(dischargeOffenceDecision.getBackDuty(), sjpSessionEnvelope, resultedOn));

        // addEndorsementAndDisqualificationResults
        judicialResults.addAll(endorsementAndDisqualificationResults(dischargeOffenceDecision, sjpSessionEnvelope, resultedOn, driverNumber));

        // compensationResult
        judicialResults.addAll(compensationResult(dischargeOffenceDecision.getCompensation(), sjpSessionEnvelope, resultedOn, prosecutingAuthority));

        // noCompensationReasonResult
        judicialResults.addAll(noCompensationReasonResult(dischargeOffenceDecision.getNoCompensationReason(), sjpSessionEnvelope, resultedOn));

        // press restriction
        judicialResults.addAll(pressRestriction(dischargeOffenceDecision.getPressRestriction(), sjpSessionEnvelope, resultedOn));


        setFinalOffence(decisionAggregate, offenceId, judicialResults);

        final Optional<CourtCentre> convictingCourtOptional = courtCentreConverter.convertByOffenceId(offenceId, sjpSessionEnvelope.metadata());

        // conviction information
        convictingCourtOptional.ifPresent(convictingCourt ->
                decisionAggregate.putConvictionInfo(offenceId,
                new ConvictionInfo(offenceId,
                        dischargeOffenceDecision.getOffenceDecisionInformation().getVerdict(),
                        dischargeOffenceDecision.getConvictionDate(),
                        convictingCourt))
        );
    }

    private List<JudicialResult> dischargeResult(final Discharge discharge,
                                                 final JsonEnvelope sjpSessionEnvelope,
                                                 final ZonedDateTime resultedOn) {

        final DischargeType dischargeType = discharge.getDischargeType();
        final UUID resultId = DischargeType.ABSOLUTE.equals(dischargeType) ? AD.getResultDefinitionId() : CD.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        final JudicialResult.Builder judicialResultBuilder = populateResultDefinitionAttributes(resultId, sjpSessionEnvelope);

        final List<JudicialResultPrompt> resultPrompts = new ArrayList<>();
        if (discharge.getDischargedFor() != null
                && DischargeType.CONDITIONAL.equals(dischargeType)) {
            final DischargePeriod dischargedFor = discharge.getDischargedFor();
            final JudicialResultPrompt durationFor = getPromptByDuration(DURATION_VALUE_OF_CONDITIONAL_DISCHARGE, resultDefinition, unitsMap.get(dischargedFor.getUnit().name()))
                    .withJudicialResultPromptTypeId(DURATION_VALUE_OF_CONDITIONAL_DISCHARGE.getId())
                    .withValue(joinUnit(dischargedFor))
                    .build();
            resultPrompts.add(durationFor);
        }

        judicialResultBuilder
                .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(resultPrompts))
                .withOrderedDate(resultedOn.format(DATE_FORMAT))
                .withDurationElement(new JudicialResultPromptDurationHelper().populate(resultPrompts,
                        getSessionDateTime(sjpSessionEnvelope),
                        resultDefinition).orElse(null))
                .withResultText(getResultText(resultPrompts, resultDefinition.getString(LABEL)));

        return Arrays.asList(judicialResultBuilder.build());
    }
}
