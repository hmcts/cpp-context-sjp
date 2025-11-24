package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.SETASIDE;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SetAsideDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public SetAsideDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {
        final SetAside setAside = (SetAside) offenceDecision;
        final List<JudicialResult> judicialResults = new ArrayList<>();

        final UUID resultId = SETASIDE.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(new ArrayList<>(), resultDefinition.getString(LABEL)))
                .build());

        setAside
            .getOffenceDecisionInformation()
            .forEach(offenceDecisionInformation -> {
                decisionAggregate.putResults(offenceDecisionInformation.getOffenceId(), judicialResults);
                setFinalOffence(decisionAggregate, offenceDecisionInformation.getOffenceId(), judicialResults);
            });
    }

}
