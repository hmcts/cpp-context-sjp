package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.HOUR_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRTO_DATE_OF_HEARING;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRTO_MAGISTRATES_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRTO_REASONS_FOR_REFERRING;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRTO_TIME_OF_HEARING;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.SUMRTO;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferredToOpenCourtDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public ReferredToOpenCourtDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {
        final ReferredToOpenCourt referredToOpenCourt = (ReferredToOpenCourt) offenceDecision;

        final UUID resultId = SUMRTO.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        judicialResultPrompts.add(getPrompt(SUMRTO_DATE_OF_HEARING, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRTO_DATE_OF_HEARING.getId())
                .withValue(referredToOpenCourt.getReferredToDateTime().format(DATE_FORMAT))
                .build());

        judicialResultPrompts.add(getPrompt(SUMRTO_MAGISTRATES_COURT, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRTO_MAGISTRATES_COURT.getId())
                .withValue(referredToOpenCourt.getMagistratesCourt())
                .build());

        judicialResultPrompts.add(getPrompt(SUMRTO_TIME_OF_HEARING, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRTO_TIME_OF_HEARING.getId())
                .withValue(referredToOpenCourt.getReferredToDateTime().format(HOUR_FORMAT))
                .build());

        judicialResultPrompts.add(getPrompt(SUMRTO_REASONS_FOR_REFERRING, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRTO_REASONS_FOR_REFERRING.getId())
                .withValue(referredToOpenCourt.getReason()).build());

        judicialResults.add(populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                .withOrderedDate(resultedOn.format(DATE_FORMAT))
                .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                .build());

        referredToOpenCourt
                .getOffenceDecisionInformation()
                .forEach(offenceDecisionInformation -> {
                    decisionAggregate.putResults(offenceDecisionInformation.getOffenceId(), judicialResults);
                    setFinalOffence(decisionAggregate, offenceDecisionInformation.getOffenceId(), judicialResults);
                });

    }

}
