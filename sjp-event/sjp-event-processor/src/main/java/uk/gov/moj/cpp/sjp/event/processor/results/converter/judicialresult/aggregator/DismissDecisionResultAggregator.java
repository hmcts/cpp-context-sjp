package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.D;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DismissDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public DismissDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {
        final Dismiss dismissOffenceDecision = (Dismiss) offenceDecision;
        final UUID offenceId = dismissOffenceDecision.getOffenceDecisionInformation().getOffenceId();
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        final UUID resultId = D.getResultDefinitionId();

        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .build());


        decisionAggregate.putResults(offenceId, judicialResults);

        setFinalOffence(decisionAggregate, offenceId, judicialResults);

    }

}
