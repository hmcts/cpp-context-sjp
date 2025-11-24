package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseCourtReferralStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class CourtReferralListener {

    @Inject
    private CaseCourtReferralStatusRepository caseCourtReferralStatusRepository;

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.case-referral-for-court-hearing-rejection-recorded")
    public void handleCaseReferredForCourtHearingRejectionRecorded(final Envelope<CaseReferralForCourtHearingRejectionRecorded> eventEnvelope) {
        final CaseReferralForCourtHearingRejectionRecorded caseReferralForCourtHearingRejectionRecorded = eventEnvelope.payload();

        final UUID caseId = caseReferralForCourtHearingRejectionRecorded.getCaseId();

        final CaseCourtReferralStatus caseCourtReferralStatus = caseCourtReferralStatusRepository.findBy(caseId);

        caseCourtReferralStatus.markRejected(
                caseReferralForCourtHearingRejectionRecorded.getRejectedAt(),
                caseReferralForCourtHearingRejectionRecorded.getRejectionReason());

        final CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setManagedByAtcm(true);
        caseRepository.save(caseDetail);
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void handleCaseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> envelope) {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = envelope.payload();

        final CaseDetail caseDetail = caseRepository.findBy(caseReferredForCourtHearing.getCaseId());

        final CaseCourtReferralStatus caseCourtReferralStatus = new CaseCourtReferralStatus(
                caseDetail.getId(),
                caseDetail.getUrn(),
                caseReferredForCourtHearing.getReferredAt());

        caseCourtReferralStatusRepository.save(caseCourtReferralStatus);

        caseDetail.setReferredForCourtHearing(true);
        caseDetail.setManagedByAtcm(false);
        updateDisabilityNeeds(caseReferredForCourtHearing.getDefendantCourtOptions(), caseDetail);
    }

    @Handles("sjp.events.case-referred-for-court-hearing-v2")
    public void handleCaseReferredForCourtHearingV2(final Envelope<CaseReferredForCourtHearingV2> envelope) {
        final CaseReferredForCourtHearingV2 caseReferredForCourtHearing = envelope.payload();

        final CaseDetail caseDetail = caseRepository.findBy(caseReferredForCourtHearing.getCaseId());

        final CaseCourtReferralStatus caseCourtReferralStatus = new CaseCourtReferralStatus(
                caseDetail.getId(),
                caseDetail.getUrn(),
                caseReferredForCourtHearing.getReferredAt());

        caseCourtReferralStatusRepository.save(caseCourtReferralStatus);

        caseDetail.setReferredForCourtHearing(true);
        caseDetail.setManagedByAtcm(false);
        updateDisabilityNeeds(caseReferredForCourtHearing.getDefendantCourtOptions(), caseDetail);
    }

    private void updateDisabilityNeeds(final DefendantCourtOptions defendantCourtOptions, final CaseDetail caseDetail) {
        ofNullable(defendantCourtOptions)
                .map(DefendantCourtOptions::getDisabilityNeeds)
                .map(DisabilityNeeds::getDisabilityNeeds)
                .ifPresent(disabilityNeeds -> {
                    caseDetail.getDefendant().setDisabilityNeeds(disabilityNeeds);
                    caseRepository.save(caseDetail);
                });
    }
}
