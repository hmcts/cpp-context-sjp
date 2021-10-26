package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.util.Objects.nonNull;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.AdjournDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.DischargeDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.DismissDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.FinancialImpositionDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.FinancialPenaltyDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.NoSeparatePenaltyDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.ReferForCourtHearingDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.ReferredForFutureSjpSessionDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.ReferredToOpenCourtDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.SetAsideDecisionResultAggregator;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.WithDrawDecisionResultAggregator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class DecisionSavedToJudicialResultsConverter {

    @Inject
    private WithDrawDecisionResultAggregator withDrawDecisionResultAggregator;

    @Inject
    private DismissDecisionResultAggregator dismissDecisionResultAggregator;

    @Inject
    private AdjournDecisionResultAggregator adjournDecisionResultAggregator;

    @Inject
    private DischargeDecisionResultAggregator dischargeDecisionResultAggregator;

    @Inject
    private FinancialPenaltyDecisionResultAggregator financialPenaltyDecisionResultAggregator;

    @Inject
    private ReferredToOpenCourtDecisionResultAggregator referredToOpenCourtResultAggregator;

    @Inject
    private ReferredForFutureSjpSessionDecisionResultAggregator referredForFutureSjpSessionResultAggregator;

    @Inject
    private ReferForCourtHearingDecisionResultAggregator referForCourtHearingResultAggregator;

    @Inject
    private NoSeparatePenaltyDecisionResultAggregator noSeparatePenaltyDecisionResultAggregator;

    @Inject
    private SetAsideDecisionResultAggregator setAsideDecisionResultAggregator;

    @Inject
    private FinancialImpositionDecisionResultAggregator financialImpositionDecisionResultAggregator;

    public DecisionAggregate convertOffenceDecisions(final DecisionSaved decisionSaved,
                                                     final JsonEnvelope sjpSessionEnvelope,
                                                     final UUID defendantId,
                                                     final ZonedDateTime resultOn,
                                                     final String driverNumber,
                                                     final String prosecutingAuthority) {
        final DecisionAggregate resultsAggregate = new DecisionAggregate();
        final List<OffenceDecision> offenceDecisions = decisionSaved.getOffenceDecisions();
        final FinancialImposition financialImposition = decisionSaved.getFinancialImposition();
        resultsAggregate.putResults(decisionSaved.getCaseId(), new ArrayList<>());

        offenceDecisions
                .forEach(offenceDecision -> convertOffenceDecision(offenceDecision,
                        decisionSaved,
                        sjpSessionEnvelope,
                        resultsAggregate,
                        resultOn,
                        driverNumber,
                        prosecutingAuthority));

        if (nonNull(financialImposition)) {
            financialImpositionDecisionResultAggregator.aggregate(decisionSaved,
                    sjpSessionEnvelope,
                    resultsAggregate,
                    defendantId,
                    decisionSaved.getCaseId(),
                    resultOn,
                    prosecutingAuthority);
        }

        return resultsAggregate;
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private void convertOffenceDecision(final OffenceDecision offenceDecision,
                                        final DecisionSaved decisionSaved,
                                        final JsonEnvelope sjpSessionPayloadObject,
                                        final DecisionAggregate resultsAggregate,
                                        final ZonedDateTime resultOn,
                                        final String driverNumber,
                                        final String prosecutingAuthority) {
        final DecisionType decisionType = offenceDecision.getType();
        final ZonedDateTime resultedOn = decisionSaved.getSavedAt();

        switch (decisionType) {
            case WITHDRAW:
                withDrawDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultOn);
                break;
            case DISMISS:
                dismissDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultedOn);
                break;
            case ADJOURN:
                adjournDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultOn);
                break;
            case DISCHARGE:
                dischargeDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultedOn, driverNumber, prosecutingAuthority);
                break;
            case FINANCIAL_PENALTY:
                financialPenaltyDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultedOn, driverNumber, prosecutingAuthority);
                break;
            case REFERRED_TO_OPEN_COURT:
                referredToOpenCourtResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultOn);
                break;
            case REFERRED_FOR_FUTURE_SJP_SESSION:
                referredForFutureSjpSessionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultOn);
                break;
            case REFER_FOR_COURT_HEARING:
                referForCourtHearingResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultedOn);
                break;
            case NO_SEPARATE_PENALTY:
                noSeparatePenaltyDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultedOn);
                break;
            case SET_ASIDE:
                setAsideDecisionResultAggregator.aggregate(offenceDecision, sjpSessionPayloadObject, resultsAggregate, resultOn);
                break;
            default:
                break;
        }
    }
}
