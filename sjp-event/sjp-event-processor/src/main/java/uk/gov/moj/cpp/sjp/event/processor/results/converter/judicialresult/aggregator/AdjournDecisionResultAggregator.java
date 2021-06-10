package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.ADJOURN_TO_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.ADJOURNSJP;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class AdjournDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public AdjournDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {
        final Adjourn adjournOffenceDecision = (Adjourn) offenceDecision;
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final UUID resultId = ADJOURNSJP.getResultDefinitionId();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        judicialResultPrompts.add(getPrompt(ADJOURN_TO_DATE, resultDefinition)
                .withJudicialResultPromptTypeId(ADJOURN_TO_DATE.getId())
                .withValue(adjournOffenceDecision.getAdjournTo().toString())
                .build());

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withJudicialResultPrompts(judicialResultPrompts)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .build());

        adjournOffenceDecision
                .getOffenceDecisionInformation()
                .forEach(offenceDecisionInformation -> decisionAggregate.putResults(offenceDecisionInformation.getOffenceId(), judicialResults));

        // conviction information
        adjournOffenceDecision
                .getOffenceDecisionInformation()
                .forEach(oi -> decisionAggregate.putConvictionInfo(oi.getOffenceId(),
                                new ConvictionInfo(oi.getOffenceId(),
                                        oi.getVerdict(),
                                        adjournOffenceDecision.getConvictionDate())));
    }

}
