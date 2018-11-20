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
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
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
    private Clock clock;

    @Handles(PleaUpdated.EVENT_NAME)
    @Transactional
    public void updatePlea(final JsonEnvelope envelope) {
        final PleaUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaUpdated.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(event.getOffenceId());

        offenceDetail.setPlea(event.getPlea());
        offenceDetail.setPleaMethod(event.getPleaMethod());
        offenceDetail.setPleaDate(Optional.ofNullable(event.getUpdatedDate()).orElse(null));
        offenceDetail.setMitigation(event.getMitigation());
        offenceDetail.setNotGuiltyBecause(event.getNotGuiltyBecause());

        updatePleaReceivedDate(event.getCaseId(),
                envelope.metadata().createdAt().map(ZonedDateTime::toLocalDate)
                        .orElse(now()));

        if (PleaMethod.ONLINE.equals(event.getPleaMethod())) {
            final OnlinePlea onlinePlea = new OnlinePlea(event);
            if (onlinePlea.getSubmittedOn() == null) {
                onlinePlea.setSubmittedOn(envelope.metadata().createdAt()
                        .orElse(clock.now()));
            }
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
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

    }

    private void updatePleaReceivedDate(final UUID caseId, final LocalDate pleaReceivedDate) {
        caseSearchResultService.updatePleaReceivedDate(caseId, pleaReceivedDate);
    }

}
