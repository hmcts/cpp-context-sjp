package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    private CaseSearchResultRepository searchResultRepository;

    @Handles("sjp.events.plea-updated")
    @Transactional
    public void updatePlea(final JsonEnvelope envelope) {
        final PleaUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaUpdated.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(UUID.fromString(event.getOffenceId()));

        offenceDetail.setPlea(event.getPlea());
        offenceDetail.setPleaMethod(event.getPleaMethod());

        updatePleaReceivedDate(UUID.fromString(event.getCaseId()),
                envelope.metadata().createdAt().map(ZonedDateTime::toLocalDate)
                        .orElse(now()));
    }

    @Handles("sjp.events.plea-cancelled")
    @Transactional
    public void cancelPlea(final JsonEnvelope envelope) {
        final PleaCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaCancelled.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(UUID.fromString(event.getOffenceId()));

        offenceDetail.setPlea(null);
        offenceDetail.setPleaMethod(null);

        updatePleaReceivedDate(UUID.fromString(event.getCaseId()), null);

    }

    @Transactional
    void updatePleaReceivedDate(final UUID caseId, final LocalDate pleaReceived) {
        searchResultRepository.findByCaseId(caseId)
                .forEach(searchResult -> searchResult.setPleaDate(pleaReceived));
    }

}
