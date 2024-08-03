package uk.gov.moj.cpp.sjp.event.listener;

import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantDetailUpdateRequestRepository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.transaction.Transactional;

@SuppressWarnings("squid:S1133")
@ServiceComponent(EVENT_LISTENER)
public class DefendantPersonalDetailsChangesListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DefendantDetailUpdateRequestRepository defendantDetailUpdateRequestRepository;

    @Handles("sjp.events.defendant-detail-update-requested")
    @Transactional
    public void defendantDetailUpdateRequested(final JsonEnvelope envelope) {
        final DefendantDetailUpdateRequested defendantDetailUpdateRequested = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantDetailUpdateRequested.class);

        final DefendantDetailUpdateRequest request = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(defendantDetailUpdateRequested.getCaseId())
                .withNameUpdated(defendantDetailUpdateRequested.getNameUpdated())
                .withAddressUpdated(defendantDetailUpdateRequested.getAddressUpdated())
                .withDobUpdated(defendantDetailUpdateRequested.getDobUpdated())
                .build();

        final DefendantDetailUpdateRequest updateRequest = defendantDetailUpdateRequestRepository.findBy(defendantDetailUpdateRequested.getCaseId());
        if (nonNull(updateRequest)) {
            updateRequest.setNameUpdated(defendantDetailUpdateRequested.getNameUpdated());
            updateRequest.setAddressUpdated(defendantDetailUpdateRequested.getAddressUpdated());
            updateRequest.setDobUpdated(defendantDetailUpdateRequested.getDobUpdated());
            defendantDetailUpdateRequestRepository.save(updateRequest);
        } else {
            defendantDetailUpdateRequestRepository.save(request);
        }
    }

    @Handles("sjp.events.defendant-name-update-requested")
    @Transactional
    public void defendantNameUpdateRequested(final JsonEnvelope envelope) {
        final DefendantNameUpdateRequested defendantNameUpdateRequested = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantNameUpdateRequested.class);

        final ZonedDateTime updateDate = getUpdateDate(envelope, defendantNameUpdateRequested.getUpdatedAt());

        final DefendantDetailUpdateRequest.Builder defendantDetailUpdateRequestBuilder = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(defendantNameUpdateRequested.getCaseId())
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withUpdatedAt(updateDate);
        final PersonalName newPersonalName = defendantNameUpdateRequested.getNewPersonalName();
        if (nonNull(newPersonalName)) {
            defendantDetailUpdateRequestBuilder.withFirstName(newPersonalName.getFirstName());
            defendantDetailUpdateRequestBuilder.withLastName(newPersonalName.getLastName());
        } else {
            defendantDetailUpdateRequestBuilder.withLegalEntityName(defendantNameUpdateRequested.getNewLegalEntityName());
        }
        final DefendantDetailUpdateRequest updateRequest = defendantDetailUpdateRequestRepository.findBy(defendantNameUpdateRequested.getCaseId());
        if (nonNull(updateRequest)) {
            updateRequest.setStatus(DefendantDetailUpdateRequest.Status.PENDING);
            updateRequest.setUpdatedAt(updateDate);
            if (nonNull(newPersonalName)) {
                updateRequest.setFirstName(newPersonalName.getFirstName());
                updateRequest.setLastName(newPersonalName.getLastName());
            } else {
                updateRequest.setLegalEntityName(defendantNameUpdateRequested.getNewLegalEntityName());
            }
            defendantDetailUpdateRequestRepository.save(updateRequest);
        } else {
            defendantDetailUpdateRequestRepository.save(defendantDetailUpdateRequestBuilder.build());
        }
    }

    private static ZonedDateTime getUpdateDate(final JsonEnvelope envelope, final ZonedDateTime dateTime) {
        return Optional.ofNullable(dateTime)
                .orElseGet(() -> envelope.metadata().createdAt()
                        .orElseThrow(() -> new IllegalStateException(
                                format("Event {0} has no creation date.", envelope.metadata().id()))));
    }

    @Handles("sjp.events.defendant-name-updated")
    @Transactional
    public void defendantNameUpdated(final JsonEnvelope envelope) {
        final DefendantNameUpdated defendantNameUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantNameUpdated.class);

        recordDefendantDetailsUpdate(
                defendantNameUpdated.getCaseId(),
                defendantNameUpdated.getUpdatedAt(),
                envelope.metadata(),
                CaseDetail::markDefendantNameUpdated);
    }

    /**
     * @deprecated defendant name updated is the new one
     */
    @Deprecated
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


    @Handles("sjp.events.defendant-address-update-requested")
    @Transactional
    public void defendantAddressUpdateRequested(final JsonEnvelope envelope) {
        final DefendantAddressUpdateRequested defendantAddressUpdatedRequested = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantAddressUpdateRequested.class);

        final ZonedDateTime updateDate = getUpdateDate(envelope, defendantAddressUpdatedRequested.getUpdatedAt());
        final DefendantDetailUpdateRequest.Status updateRequestStatus = defendantAddressUpdatedRequested.isAddressUpdateFromApplication() ?
                                                                        DefendantDetailUpdateRequest.Status.UPDATED : DefendantDetailUpdateRequest.Status.PENDING;
        final DefendantDetailUpdateRequest request = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(defendantAddressUpdatedRequested.getCaseId())
                .withStatus(updateRequestStatus)
                .withUpdatedAt(defendantAddressUpdatedRequested.getUpdatedAt())
                .withAddress1(defendantAddressUpdatedRequested.getNewAddress().getAddress1())
                .withAddress2(defendantAddressUpdatedRequested.getNewAddress().getAddress2())
                .withAddress3(defendantAddressUpdatedRequested.getNewAddress().getAddress3())
                .withAddress4(defendantAddressUpdatedRequested.getNewAddress().getAddress4())
                .withAddress5(defendantAddressUpdatedRequested.getNewAddress().getAddress5())
                .withPostcode(defendantAddressUpdatedRequested.getNewAddress().getPostcode())
                .withUpdatedAt(updateDate)
                .build();

        final DefendantDetailUpdateRequest detailUpdateRequest = defendantDetailUpdateRequestRepository.findBy(defendantAddressUpdatedRequested.getCaseId());
        if (nonNull(detailUpdateRequest)) {
            detailUpdateRequest.setStatus(updateRequestStatus);
            detailUpdateRequest.setAddress1(defendantAddressUpdatedRequested.getNewAddress().getAddress1());
            detailUpdateRequest.setAddress2(defendantAddressUpdatedRequested.getNewAddress().getAddress2());
            detailUpdateRequest.setAddress3(defendantAddressUpdatedRequested.getNewAddress().getAddress3());
            detailUpdateRequest.setAddress4(defendantAddressUpdatedRequested.getNewAddress().getAddress4());
            detailUpdateRequest.setAddress5(defendantAddressUpdatedRequested.getNewAddress().getAddress5());
            detailUpdateRequest.setPostcode(defendantAddressUpdatedRequested.getNewAddress().getPostcode());
            detailUpdateRequest.setUpdatedAt(updateDate);
            defendantDetailUpdateRequestRepository.save(detailUpdateRequest);
        } else {
            defendantDetailUpdateRequestRepository.save(request);
        }
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

    @Handles("sjp.events.defendant-date-of-birth-update-requested")
    @Transactional
    public void defendantDateOfBirthUpdateRequested(final JsonEnvelope envelope) {
        final DefendantDateOfBirthUpdateRequested defendantDateOfBirthUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantDateOfBirthUpdateRequested.class);

        final ZonedDateTime updateDate = getUpdateDate(envelope, defendantDateOfBirthUpdated.getUpdatedAt());

        final DefendantDetailUpdateRequest request = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(defendantDateOfBirthUpdated.getCaseId())
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withDateOfBirth(defendantDateOfBirthUpdated.getNewDateOfBirth())
                .withUpdatedAt(updateDate)
                .build();

        final DefendantDetailUpdateRequest updateRequest = defendantDetailUpdateRequestRepository.findBy(defendantDateOfBirthUpdated.getCaseId());
        if (nonNull(updateRequest)) {
            updateRequest.setStatus(DefendantDetailUpdateRequest.Status.PENDING);
            updateRequest.setDateOfBirth(defendantDateOfBirthUpdated.getNewDateOfBirth());
            updateRequest.setUpdatedAt(updateDate);
            defendantDetailUpdateRequestRepository.save(updateRequest);
        } else {
            defendantDetailUpdateRequestRepository.save(request);
        }
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