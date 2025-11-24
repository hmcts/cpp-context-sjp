package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class CaseAdjournmentListener {

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.case-adjourned-to-later-sjp-hearing-recorded")
    public void handleCaseAdjournedToLaterSjpHearingRecorded(final Envelope<CaseAdjournedToLaterSjpHearingRecorded> eventEnvelope) {
        final CaseAdjournedToLaterSjpHearingRecorded caseAdjournment = eventEnvelope.payload();

        caseRepository.findBy(caseAdjournment.getCaseId())
                .setAdjournedTo(caseAdjournment.getAdjournedTo());
    }

    @Handles("sjp.events.case-adjournment-to-later-sjp-hearing-elapsed")
    public void handleCaseAdjournmentToLaterSjpHearingElapsed(final Envelope<CaseAdjournmentToLaterSjpHearingElapsed> eventEnvelope) {
        final CaseAdjournmentToLaterSjpHearingElapsed caseAdjournment = eventEnvelope.payload();

        caseRepository.findBy(caseAdjournment.getCaseId())
                .setAdjournedTo(null);
    }
}
