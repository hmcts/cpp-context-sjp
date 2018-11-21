package uk.gov.moj.cpp.sjp.event.listener;

import static java.text.MessageFormat.format;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DefendantPersonalDetailsChangesListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.defendant-personal-name-updated")
    @Transactional
    public void defendantPersonalNameUpdated(final JsonEnvelope envelope) {
        final DefendantPersonalNameUpdated defendantPersonalNameUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantPersonalNameUpdated.class);

        recordDefendantDetailsUpdate(
                defendantPersonalNameUpdated.getCaseId(),
                defendantPersonalNameUpdated.getUpdatedAt(),
                envelope.metadata(),
                CaseDetail::markDefendantNameUpdated);
    }

    @Handles("sjp.events.defendant-address-updated")
    @Transactional
    public void defendantAddressUpdated(final JsonEnvelope envelope) {
        final DefendantAddressUpdated defendantAddressUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantAddressUpdated.class);

        recordDefendantDetailsUpdate(
                defendantAddressUpdated.getCaseId(),
                defendantAddressUpdated.getUpdatedAt(),
                envelope.metadata(),
                CaseDetail::markDefendantAddressUpdated);
    }

    @Handles("sjp.events.defendant-date-of-birth-updated")
    @Transactional
    public void defendantDateOfBirthUpdated(final JsonEnvelope envelope) {
        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantDateOfBirthUpdated.class);

        recordDefendantDetailsUpdate(
                defendantDateOfBirthUpdated.getCaseId(),
                defendantDateOfBirthUpdated.getUpdatedAt(),
                envelope.metadata(),
                CaseDetail::markDefendantDateOfBirthUpdated);
    }

    private void recordDefendantDetailsUpdate(
            final UUID caseId,
            final ZonedDateTime updatedAt,
            final Metadata metadata,
            final BiConsumer<CaseDetail, ZonedDateTime> caseDetailChangeRecorder) {

        // We use the event created date in case the updatedAt field is not present in event payload(for old events)
        final ZonedDateTime updateDate = Optional.ofNullable(updatedAt)
                .orElseGet(() -> metadata.createdAt()
                        .orElseThrow(() -> new IllegalStateException(
                                format("Event {0} has no creation date.", metadata.id()))));

        final CaseDetail caseDetail = caseRepository.findBy(caseId);

        caseDetailChangeRecorder.accept(caseDetail, updateDate);

        caseRepository.save(caseDetail);
    }
}