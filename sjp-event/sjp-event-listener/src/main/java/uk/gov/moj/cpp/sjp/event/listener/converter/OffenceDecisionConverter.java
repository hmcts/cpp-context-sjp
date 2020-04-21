package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionVisitor;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredForFutureSJPSessionDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredToOpenCourtDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.SetAsideOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class OffenceDecisionConverter implements OffenceDecisionVisitor {

    private List<OffenceDecision> entities = new LinkedList<>();

    private UUID caseDecisionId;

    private OffenceDecisionConverter(final UUID caseDecisionId) {
        this.caseDecisionId = caseDecisionId;
    }

    public static List<OffenceDecision> convert(UUID caseDecisionId, uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision offenceDecision) {
        final OffenceDecisionConverter converter = new OffenceDecisionConverter(caseDecisionId);
        offenceDecision.accept(converter);
        return converter.entities;
    }

    @Override
    public void visit(final Dismiss dismiss) {
        entities = dismiss.offenceDecisionInformationAsList().stream()
                .map(offenceDecisionInformation -> new DismissOffenceDecision(
                        offenceDecisionInformation.getOffenceId(), caseDecisionId,
                        offenceDecisionInformation.getVerdict()))
                .collect(toList());
    }

    @Override
    public void visit(final Withdraw withdrawDecision) {
        final OffenceDecisionInformation offenceDecisionInformation = withdrawDecision.getOffenceDecisionInformation();
        final WithdrawOffenceDecision withdrawOffenceDecision = new WithdrawOffenceDecision(
                offenceDecisionInformation.getOffenceId(),
                caseDecisionId, withdrawDecision.getWithdrawalReasonId(),
                offenceDecisionInformation.getVerdict());

        entities = singletonList(withdrawOffenceDecision);
    }

    @Override
    public void visit(final Adjourn adjournDecision) {
        entities = adjournDecision.offenceDecisionInformationAsList().stream()
                .map(offenceDecisionInformation -> new AdjournOffenceDecision(
                        offenceDecisionInformation.getOffenceId(),
                        caseDecisionId,
                        adjournDecision.getReason(),
                        adjournDecision.getAdjournTo(),
                        offenceDecisionInformation.getVerdict(),
                        adjournDecision.getConvictionDate()
                ))
                .collect(toList());
    }

    @Override
    public void visit(final Discharge discharge) {
        final OffenceDecisionInformation offenceDecisionInformation = discharge.getOffenceDecisionInformation();
        final DischargeOffenceDecision dischargeOffenceDecision = new DischargeOffenceDecision(
                offenceDecisionInformation.getOffenceId(),
                caseDecisionId,
                offenceDecisionInformation.getVerdict(),
                discharge.getDischargedFor() == null ? null : new DischargePeriod(discharge.getDischargedFor().getUnit()
                        , discharge.getDischargedFor().getValue()),
                discharge.getGuiltyPleaTakenIntoAccount(),
                discharge.getCompensation(),
                discharge.getNoCompensationReason(),
                discharge.getDischargeType(),
                discharge.getBackDuty(),
                discharge.getConvictionDate(),
                discharge.getLicenceEndorsed(),
                discharge.getPenaltyPointsImposed(),
                discharge.getPenaltyPointsReason(),
                discharge.getAdditionalPointsReason(),
                discharge.getDisqualification(),
                discharge.getDisqualificationType(),
                ofNullable(discharge.getDisqualificationPeriod()).map(DisqualificationPeriod::getValue).orElse(null),
                ofNullable(discharge.getDisqualificationPeriod()).map(DisqualificationPeriod::getUnit).orElse(null),
                discharge.getNotionalPenaltyPoints()
        );

        entities = singletonList(dischargeOffenceDecision);
    }

    @Override
    public void visit(final ReferForCourtHearing referForCourtHearing) {
        entities = referForCourtHearing.offenceDecisionInformationAsList().stream()
                .map(offenceDecisionInformation -> new ReferForCourtHearingDecision(
                        offenceDecisionInformation.getOffenceId(),
                        caseDecisionId,
                        referForCourtHearing.getReferralReasonId(),
                        referForCourtHearing.getEstimatedHearingDuration(),
                        referForCourtHearing.getListingNotes(),
                        offenceDecisionInformation.getVerdict(),
                        referForCourtHearing.getConvictionDate()))
                .collect(toList());
    }

    @Override
    public void visit(final FinancialPenalty financialPenalty) {
        final OffenceDecisionInformation offenceDecisionInformation = financialPenalty.getOffenceDecisionInformation();
        final FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = new FinancialPenaltyOffenceDecision(
                offenceDecisionInformation.getOffenceId(),
                caseDecisionId,
                offenceDecisionInformation.getVerdict(),
                financialPenalty.getGuiltyPleaTakenIntoAccount(),
                financialPenalty.getCompensation(),
                financialPenalty.getNoCompensationReason(),
                financialPenalty.getFine(),
                financialPenalty.getBackDuty(),
                financialPenalty.getExcisePenalty(),
                financialPenalty.getConvictionDate(),
                financialPenalty.getLicenceEndorsed(),
                financialPenalty.getPenaltyPointsImposed(),
                financialPenalty.getPenaltyPointsReason(),
                financialPenalty.getAdditionalPointsReason(),
                financialPenalty.getDisqualification(),
                financialPenalty.getDisqualificationType(),
                ofNullable(financialPenalty.getDisqualificationPeriod()).map(DisqualificationPeriod::getValue).orElse(null),
                ofNullable(financialPenalty.getDisqualificationPeriod()).map(DisqualificationPeriod::getUnit).orElse(null),
                financialPenalty.getNotionalPenaltyPoints());

        entities = singletonList(financialPenaltyOffenceDecision);
    }

    @Override
    public void visit(final ReferredToOpenCourt referredToOpenCourt) {
        entities = referredToOpenCourt.offenceDecisionInformationAsList()
                .stream()
                .map(offenceDecisionInformation -> new ReferredToOpenCourtDecision(
                        offenceDecisionInformation.getOffenceId(),
                        caseDecisionId,
                        offenceDecisionInformation.getVerdict(),
                        referredToOpenCourt.getReferredToCourt(),
                        referredToOpenCourt.getReferredToRoom(),
                        referredToOpenCourt.getReferredToDateTime(),
                        referredToOpenCourt.getReason(),
                        referredToOpenCourt.getMagistratesCourt()))
                .collect(toList());
    }

    @Override
    public void visit(final ReferredForFutureSJPSession referredForFutureSJPSession) {
        entities = referredForFutureSJPSession.offenceDecisionInformationAsList()
                .stream()
                .map(offenceDecisionInformation -> new ReferredForFutureSJPSessionDecision(
                        offenceDecisionInformation.getOffenceId(),
                        caseDecisionId,
                        offenceDecisionInformation.getVerdict()))
                .collect(toList());
    }

    @Override
    public void visit(final NoSeparatePenalty noSeparatePenalty) {
        entities = singletonList(new NoSeparatePenaltyOffenceDecision(
                noSeparatePenalty.getOffenceDecisionInformation().getOffenceId(),
                caseDecisionId,
                noSeparatePenalty.getOffenceDecisionInformation().getVerdict(),
                noSeparatePenalty.getConvictionDate(),
                noSeparatePenalty.getGuiltyPleaTakenIntoAccount(),
                noSeparatePenalty.getLicenceEndorsed()
        ));
    }

    @Override
    public void visit(final SetAside setAside) {
        entities = setAside.offenceDecisionInformationAsList()
                .stream()
                .map(offenceDecisionInformation -> new SetAsideOffenceDecision(
                        offenceDecisionInformation.getOffenceId(),
                        caseDecisionId
                )).collect(toList());

    }

}
