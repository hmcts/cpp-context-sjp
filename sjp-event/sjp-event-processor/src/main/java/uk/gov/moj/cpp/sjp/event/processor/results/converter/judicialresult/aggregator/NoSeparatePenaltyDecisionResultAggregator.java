package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.Collections.singletonList;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LEN;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.NSP;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class NoSeparatePenaltyDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    private CourtCentreConverter courtCentreConverter;

    @Inject
    public NoSeparatePenaltyDecisionResultAggregator(final JCachedReferenceData jCachedReferenceData) {
        super(jCachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final ZonedDateTime resultedOn) {

        final NoSeparatePenalty noSeparatePenalty = (NoSeparatePenalty) offenceDecision;
        final UUID offenceId = noSeparatePenalty.getOffenceDecisionInformation().getOffenceId();

        final List<JudicialResult> judicialResults = new ArrayList<>();
        decisionAggregate.putResults(offenceId, judicialResults);

        // NSP
        judicialResults.addAll(noSeparateResult(sjpSessionEnvelope, resultedOn));

        // addEndorsementAndDisqualificationResults
        judicialResults.addAll(endorsementResults(noSeparatePenalty, sjpSessionEnvelope, resultedOn));

        // press restriction
        judicialResults.addAll(pressRestriction(noSeparatePenalty.getPressRestriction(), sjpSessionEnvelope, resultedOn));


        setFinalOffence(decisionAggregate, offenceId, judicialResults);

        final Optional<CourtCentre> convictingCourtOptional = courtCentreConverter.convertByOffenceId(offenceId, sjpSessionEnvelope.metadata());

        convictingCourtOptional.ifPresent(convictingCourt ->
                decisionAggregate.putConvictionInfo(offenceId,
                        new ConvictionInfo(offenceId,
                                noSeparatePenalty.getOffenceDecisionInformation().getVerdict(),
                                noSeparatePenalty.getConvictionDate(),
                                convictingCourt))
        );
    }

    private List<JudicialResult> noSeparateResult(final JsonEnvelope sjpSessionEnvelope,
                                                  final ZonedDateTime resultedOn) {
        final UUID resultId = NSP.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        return singletonList(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(new ArrayList<>(), resultDefinition.getString(LABEL)))
                .build());
    }

    private List<JudicialResult> endorsementResults(final NoSeparatePenalty noSeparatePenalty,
                                                    final JsonEnvelope sjpSessionEnvelope,
                                                    final ZonedDateTime resultedOn) {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        if (Boolean.TRUE.equals(noSeparatePenalty.getLicenceEndorsed())) {
            judicialResults.addAll(licenceEndorsementResult(sjpSessionEnvelope, resultedOn));
        }
        return judicialResults;
    }

    private List<JudicialResult> licenceEndorsementResult(final JsonEnvelope sjpSessionEnvelope,
                                                          final ZonedDateTime resultedOn) {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        final UUID resultId = LEN.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);

        judicialResults.add(
                populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                        .withOrderedDate(resultedOn.format(DATE_FORMAT))
                        .withResultText(getResultText(new ArrayList<>(), resultDefinition.getString(LABEL)))
                        .build());

        return judicialResults;
    }

}
