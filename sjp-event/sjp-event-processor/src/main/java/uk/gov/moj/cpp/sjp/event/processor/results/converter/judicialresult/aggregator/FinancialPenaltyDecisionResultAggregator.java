package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_EXCISE_PENALTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.AMOUNT_OF_FINE;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class FinancialPenaltyDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public FinancialPenaltyDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn,
                          final String driverNumber,
                          final String prosecutingAuthority) {

        final FinancialPenalty financialPenaltyDecision = (FinancialPenalty) offenceDecision;
        final UUID offenceId = financialPenaltyDecision.getOffenceDecisionInformation().getOffenceId();

        final List<JudicialResult> judicialResults = new ArrayList<>();
        decisionAggregate.putResults(offenceId, judicialResults);

        // fine result
        judicialResults.addAll(fineResult(financialPenaltyDecision, sjpSessionEnvelope, resultedOn));

        // backDutyResult
        judicialResults.addAll(backDutyResult(financialPenaltyDecision.getBackDuty(), sjpSessionEnvelope, resultedOn));

        // excise penalty
        judicialResults.addAll(excisePenaltyResult(financialPenaltyDecision, sjpSessionEnvelope, resultedOn));

        // compensationResult
        judicialResults.addAll(compensationResult(financialPenaltyDecision.getCompensation(), sjpSessionEnvelope, resultedOn, prosecutingAuthority));

        // noCompensationReasonResult
        judicialResults.addAll(noCompensationReasonResult(financialPenaltyDecision.getNoCompensationReason(), sjpSessionEnvelope, resultedOn));

        // addEndorsementAndDisqualificationResults
        judicialResults.addAll(endorsementAndDisqualificationResults(financialPenaltyDecision, sjpSessionEnvelope, resultedOn, driverNumber));

        // press restriction
        judicialResults.addAll(pressRestriction(financialPenaltyDecision.getPressRestriction(), sjpSessionEnvelope, resultedOn));

        setFinalOffence(decisionAggregate, offenceId, judicialResults);

        decisionAggregate.putConvictionInfo(offenceId,
                new ConvictionInfo(offenceId,
                        financialPenaltyDecision.getOffenceDecisionInformation().getVerdict(),
                        financialPenaltyDecision.getConvictionDate()));
    }

    private List<JudicialResult> excisePenaltyResult(final FinancialPenalty financialPenalty,
                                                     final JsonEnvelope sjpSessionEnvelope,
                                                     final ZonedDateTime resultedOn) {
        final UUID resultId = JResultCode.EXPEN.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        final BigDecimal excisePenalty = financialPenalty.getExcisePenalty();
        if (nonNull(excisePenalty) && excisePenalty.doubleValue() > 0) {
            final JudicialResultPrompt excisePenaltyPrompt = getPrompt(AMOUNT_OF_EXCISE_PENALTY, resultDefinition)
                    .withJudicialResultPromptTypeId(AMOUNT_OF_EXCISE_PENALTY.getId())
                    .withValue(getCurrencyAmount(excisePenalty.toString()))
                    .build();
            judicialResultPrompts.add(excisePenaltyPrompt);

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .build());
        }

        return judicialResults;
    }

    private List<JudicialResult> fineResult(final FinancialPenalty financialPenalty,
                                            final JsonEnvelope sjpSessionEnvelope,
                                            final ZonedDateTime resultedOn) {
        final UUID resultId = JResultCode.FO.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        final BigDecimal fine = financialPenalty.getFine();
        if (nonNull(fine) && fine.doubleValue() > 0) {
            final JudicialResultPrompt amountOfFine = getPrompt(AMOUNT_OF_FINE, resultDefinition)
                    .withJudicialResultPromptTypeId(AMOUNT_OF_FINE.getId())
                    .withValue(getCurrencyAmount(fine.toString()))
                    .build();
            judicialResultPrompts.add(amountOfFine);

            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .build());
        }
        return judicialResults;
    }
}
