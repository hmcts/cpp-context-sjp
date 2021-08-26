package uk.gov.moj.cpp.sjp.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.ZonedDateTime;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

@ServiceComponent(EVENT_LISTENER)
public class OffenceUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private OnlinePleaRepository.PleaDetailsRepository onlinePleaRepository;

    @Inject
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    /**
     * @deprecated
     * TODO: REMOVE THE LISTENER AND DECOMMISSION THE PleaUpdated event after the event transformation
     */
    @Deprecated
    @Handles(PleaUpdated.EVENT_NAME)
    @Transactional
    public void updatePlea(final JsonEnvelope envelope) {
        final PleaUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaUpdated.class);

        final ZonedDateTime pleaUpdatedDateTime = event.getUpdatedDate();
        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());

        offenceDetail.setPlea(event.getPlea());
        offenceDetail.setPleaMethod(event.getPleaMethod());
        offenceDetail.setPleaDate(pleaUpdatedDateTime);

        if (PleaMethod.ONLINE.equals(event.getPleaMethod())) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            if (onlinePlea.getSubmittedOn() == null) {
                onlinePlea.setSubmittedOn(pleaUpdatedDateTime);
            }
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
            onlinePleaDetailRepository.save(new OnlinePleaDetail(event.getOffenceId(), event.getCaseId(),
                    offenceDetail.getDefendantDetail().getId(), event.getPlea(),
                    event.getMitigation(), event.getNotGuiltyBecause()));
        }
    }

    @Handles(PleadedGuilty.EVENT_NAME)
    @Transactional
    public void updateOffenceDetailsWithPleadedGuilty(final JsonEnvelope envelope) {
        final PleadedGuilty event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleadedGuilty.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());
        offenceDetail.setPlea(GUILTY);
        offenceDetail.setPleaMethod(event.getMethod());
        offenceDetail.setPleaDate(event.getPleadDate());

        if (PleaMethod.ONLINE == event.getMethod()) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
            onlinePleaDetailRepository.save(new OnlinePleaDetail(event.getOffenceId(), event.getCaseId(),
                    offenceDetail.getDefendantDetail().getId(), GUILTY,
                    event.getMitigation(), null));
        }
    }

    @Handles(PleadedGuiltyCourtHearingRequested.EVENT_NAME)
    @Transactional
    public void updateOffenceDetailsWithPleadedGuiltyCourtHearingRequested(final JsonEnvelope envelope) {
        final PleadedGuiltyCourtHearingRequested event =
                jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleadedGuiltyCourtHearingRequested.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());
        offenceDetail.setPlea(GUILTY_REQUEST_HEARING);
        offenceDetail.setPleaMethod(event.getMethod());
        offenceDetail.setPleaDate(event.getPleadDate());

        if (PleaMethod.ONLINE.equals(event.getMethod())) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
            onlinePleaDetailRepository.save(new OnlinePleaDetail(event.getOffenceId(), event.getCaseId(),
                    offenceDetail.getDefendantDetail().getId(), GUILTY_REQUEST_HEARING,
                    event.getMitigation(), null));
        }
    }

    @Handles(PleadedNotGuilty.EVENT_NAME)
    @Transactional
    public void updateOffenceDetailsWithPleadedNotGuilty(final JsonEnvelope envelope) {
        final PleadedNotGuilty event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleadedNotGuilty.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());
        offenceDetail.setPlea(NOT_GUILTY);
        offenceDetail.setPleaMethod(event.getMethod());
        offenceDetail.setPleaDate(event.getPleadDate());

        if (PleaMethod.ONLINE.equals(event.getMethod())) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
            onlinePleaDetailRepository.save(new OnlinePleaDetail(event.getOffenceId(), event.getCaseId(),
                    offenceDetail.getDefendantDetail().getId(), NOT_GUILTY,
                    null, event.getNotGuiltyBecause()));
        }
    }

    @Handles(PleaCancelled.EVENT_NAME)
    @Transactional
    public void cancelPlea(final JsonEnvelope envelope) {
        final PleaCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaCancelled.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());

        offenceDetail.setPlea(null);
        offenceDetail.setPleaMethod(null);
        offenceDetail.setPleaDate(null);
    }

    @Handles(VerdictCancelled.EVENT_NAME)
    @Transactional
    public void cancelVerdict(final JsonEnvelope envelope) {
        final VerdictCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), VerdictCancelled.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());
        offenceDetail.setConviction(null);
        offenceDetail.setConvictionDate(null);
    }

    @Handles(OffenceWithdrawalRequested.EVENT_NAME)
    public void requestOffenceWithdrawal(final JsonEnvelope envelope) {
        final OffenceWithdrawalRequested offenceWithdrawalRequested = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), OffenceWithdrawalRequested.class);
        final OffenceDetail offenceDetail = offenceRepository.findBy(offenceWithdrawalRequested.getOffenceId());
        offenceDetail.setWithdrawalRequestReasonId(offenceWithdrawalRequested.getWithdrawalRequestReasonId());
    }

    @Handles(OffenceWithdrawalRequestCancelled.EVENT_NAME)
    public void cancelOffenceWithdrawal(final JsonEnvelope envelope) {
        final OffenceWithdrawalRequestCancelled offenceWithdrawalRequestCancelled = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), OffenceWithdrawalRequestCancelled.class);
        final OffenceDetail offenceDetail = offenceRepository.findBy(offenceWithdrawalRequestCancelled.getOffenceId());
        offenceDetail.setWithdrawalRequestReasonId(null);
    }

    @Handles(OffenceWithdrawalRequestReasonChanged.EVENT_NAME)
    public void offenceWithdrawalReasonChange(final JsonEnvelope envelope) {
        final OffenceWithdrawalRequestReasonChanged offenceWithdrawalRequestReasonChanged = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), OffenceWithdrawalRequestReasonChanged.class);
        final OffenceDetail offenceDetail = offenceRepository.findBy(offenceWithdrawalRequestReasonChanged.getOffenceId());
        offenceDetail.setWithdrawalRequestReasonId(offenceWithdrawalRequestReasonChanged.getNewWithdrawalRequestReasonId());
    }

}
