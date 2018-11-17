package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseCourtReferralStatusRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CourtReferralListener {

    @Inject
    private CaseCourtReferralStatusRepository caseCourtReferralStatusRepository;

    @Handles("sjp.events.case-referral-for-court-hearing-rejection-recorded")
    public void handleCaseReferredForCourtHearingRejectionRecorded(final Envelope<CaseReferralForCourtHearingRejectionRecorded> eventEnvelope) {
        final CaseReferralForCourtHearingRejectionRecorded caseReferralForCourtHearingRejectionRecorded = eventEnvelope.payload();

        final UUID caseId = caseReferralForCourtHearingRejectionRecorded.getCaseId();

        final CaseCourtReferralStatus caseCourtReferralStatus = caseCourtReferralStatusRepository.findBy(caseId);

        caseCourtReferralStatus.markRejected(
                caseReferralForCourtHearingRejectionRecorded.getRejectedAt(),
                caseReferralForCourtHearingRejectionRecorded.getRejectionReason());
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void handleCaseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> envelope) {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = envelope.payload();

        final CaseCourtReferralStatus caseCourtReferralStatus = new CaseCourtReferralStatus(
                caseReferredForCourtHearing.getCaseId(),
                caseReferredForCourtHearing.getReferredAt());

        caseCourtReferralStatusRepository.save(caseCourtReferralStatus);
    }
}
