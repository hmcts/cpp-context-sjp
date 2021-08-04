package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.SUMRCC;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferForCourtHearingDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    public ReferForCourtHearingDecisionResultAggregator(final JCachedReferenceData cachedReferenceData) {
        super(cachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {
        final ReferForCourtHearing referForCourtHearing = (ReferForCourtHearing) offenceDecision;
        final List<JudicialResult> judicialResults = new ArrayList<>(); // single instance should be fine

        // refer for court hearing
        judicialResults.addAll(referForCourtHearing(referForCourtHearing, sjpSessionEnvelope, resultedOn));

        // press restriction
        judicialResults.addAll(pressRestriction(referForCourtHearing.getPressRestriction(), sjpSessionEnvelope, resultedOn));

        referForCourtHearing
                .getOffenceDecisionInformation()
                .forEach(o -> {
                    decisionAggregate.putResults(o.getOffenceId(), judicialResults);
                    setFinalOffence(decisionAggregate, o.getOffenceId(), judicialResults);
                });

        // conviction information
        referForCourtHearing
                .getOffenceDecisionInformation()
                .forEach(oi -> decisionAggregate.putConvictionInfo(oi.getOffenceId(),
                        new ConvictionInfo(oi.getOffenceId(),
                                oi.getVerdict(),
                                referForCourtHearing.getConvictionDate())));
    }

    private List<JudicialResult> referForCourtHearing(final ReferForCourtHearing referForCourtHearing,
                                                      final JsonEnvelope sjpSessionEnvelope,
                                                      final ZonedDateTime resultedOn) {
        final UUID resultId = SUMRCC.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();

        final JudicialResultPrompt referralReason = getPrompt(SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT.getId())
                .withValue(jCachedReferenceData.getReferralReason(referForCourtHearing.getReferralReasonId(), envelopeFrom(sjpSessionEnvelope.metadata(), null)))
                .build();
        judicialResultPrompts.add(referralReason);

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                        .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                        .build());

        return judicialResults;
    }
}
