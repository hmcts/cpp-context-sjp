package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class OffenceUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private CaseSearchResultService caseSearchResultService;

    @Inject
    private OnlinePleaRepository.PleaDetailsRepository onlinePleaRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    @Handles(PleaUpdated.EVENT_NAME)
    @Transactional
    public void updatePlea(final JsonEnvelope envelope) {
        final PleaUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaUpdated.class);

        final ZonedDateTime pleaUpdatedDataTime = Optional.ofNullable(event.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null));
        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());

        offenceDetail.setPlea(event.getPlea());
        offenceDetail.setPleaMethod(event.getPleaMethod());
        offenceDetail.setPleaDate(pleaUpdatedDataTime);
        offenceDetail.setMitigation(event.getMitigation());
        offenceDetail.setNotGuiltyBecause(event.getNotGuiltyBecause());


        updatePleaReceivedDate(event.getCaseId(),
                envelope.metadata().createdAt().map(ZonedDateTime::toLocalDate)
                        .orElse(now()));

        if (PleaMethod.ONLINE.equals(event.getPleaMethod())) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            if (onlinePlea.getSubmittedOn() == null) {
                onlinePlea.setSubmittedOn(pleaUpdatedDataTime);
            }
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
        final CaseDetail caseDetail = caseRepository.findBy(event.getCaseId());

        /* From https://tools.hmcts.net/confluence/display/PLAT/ATCM+Case+Statuses
        When the case has been updated with a plea of Not Guilty via online or Court Admin (post) and not updated with dates to avoid and date plea updated <=10 days

        When the case has a plea of Not Guilty and there are dates to avoid

        When the case has s plea of Not Guilty and the plea updated date > 10 days

        NOTE: If the status of the case is 'Withdrawal request - ready for decision' before a Not guilty plea is received,
        when a Not Guilty plea is received the status remains at 'Withdrawal request - ready for decision' */

        /*  From https://tools.hmcts.net/confluence/display/PLAT/ATCM+Case+Statuses
        Plea received - ready for decision
            When the case has been updated with a plea of Guilty via online or Court Admin (post)

        NOTE: If the status of the case is 'Withdrawal request - ready for decision' before a guilty plea is received,
        when a Guilty plea is received the status remains at 'Withdrawal request - ready for decision'
         */

        caseDetail.setStatus(caseDetail.getStatus().pleaReceived(event.getPlea(), caseDetail.getDatesToAvoid()));
    }

    @Handles("sjp.events.plea-cancelled")
    @Transactional
    public void cancelPlea(final JsonEnvelope envelope) {
        final PleaCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaCancelled.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());

        offenceDetail.setPlea(null);
        offenceDetail.setPleaMethod(null);
        offenceDetail.setPleaDate(null);

        updatePleaReceivedDate(event.getCaseId(), null);

        final CaseDetail caseDetail = caseRepository.findBy(event.getCaseId());

        /* Withdrawal request takes priority, in any case even if the plea is withdrawn.
        From Confluence page
        NOTE: When a plea is cancelled the case status goes back to the relevant status based on the rules above i.e. 'No plea received' if certificate of service date < 28 days
        or 'No plea received - ready for decision' if the certificate of service date >= 28 days with the exception of
        when there is a Withdrawal request - case status stays as Withdrawal request - ready for decision. */

        caseDetail.setStatus(caseDetail.getStatus().cancelPlea(event.getProvedInAbsence()));

    }

    private void updatePleaReceivedDate(final UUID caseId, final LocalDate pleaReceivedDate) {
        caseSearchResultService.updatePleaReceivedDate(caseId, pleaReceivedDate);
    }

}
