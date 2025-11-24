package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.WDRNNOT_REASONS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.WDRNNOT;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class WithDrawDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public WithDrawDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {

        final Withdraw withdrawOffenceDecision = (Withdraw) offenceDecision;
        final UUID offenceId = withdrawOffenceDecision.getOffenceDecisionInformation().getOffenceId();

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        final UUID resultId = WDRNNOT.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        judicialResultPrompts.add(getPrompt(WDRNNOT_REASONS, resultDefinition)
                .withJudicialResultPromptTypeId(WDRNNOT_REASONS.getId())
                .withValue(jCachedReferenceData.getWithdrawalReason(withdrawOffenceDecision.getWithdrawalReasonId(), envelopeFrom(sjpSessionEnvelope.metadata(), null)))
                .build());

        judicialResults.add(populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                .withOrderedDate(resultedOn.format(DATE_FORMAT))
                .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                .build());

        decisionAggregate.putResults(offenceId, judicialResults);

        setFinalOffence(decisionAggregate, offenceId, judicialResults);
    }

}
