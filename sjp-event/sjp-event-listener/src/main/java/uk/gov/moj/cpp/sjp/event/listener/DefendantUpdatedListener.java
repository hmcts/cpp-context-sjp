package uk.gov.moj.cpp.sjp.event.listener;


import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DefendantUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseSearchResultService caseSearchResultService;

    @Inject
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Inject
    private AddressToAddressEntity addressToAddressEntity;

    @Handles("sjp.events.defendant-national-insurance-number-updated")
    @Transactional
    public void defendantNationalInsuranceNumberUpdated(final JsonEnvelope envelope) {

        final DefendantsNationalInsuranceNumberUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantsNationalInsuranceNumberUpdated.class);
        final DefendantDetail defendantDetail = caseRepository.findCaseDefendant(event.getCaseId());

        if (!defendantDetail.getId().equals(event.getDefendantId())) {
            throw new IllegalArgumentException("Mismatch of defendant IDs: " + defendantDetail.getId() + " != " + event.getDefendantId() + " for case: " + defendantDetail.getCaseDetail().getId());
        }

        defendantDetail.getPersonalDetails().setNationalInsuranceNumber(event.getNationalInsuranceNumber());
    }

    @Handles("sjp.events.defendant-details-updated")
    @Transactional
    public void defendantDetailsUpdated(final JsonEnvelope envelope) {

        final DefendantDetailsUpdated defendantDetailsUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantDetailsUpdated.class);

        final CaseDetail caseDetail = caseRepository.findBy(defendantDetailsUpdated.getCaseId());

        if (caseDetail == null) {
            throw new IllegalArgumentException("Unable to update defendant's details of a case that does not exist: " + defendantDetailsUpdated.getCaseId());
        }

        updateDefendant(caseDetail.getDefendant(), defendantDetailsUpdated);
        caseRepository.save(caseDetail);

        final LocalDate dateOfBirth = defendantDetailsUpdated.getDateOfBirth();

        caseSearchResultService.onDefendantDetailsUpdated(
                defendantDetailsUpdated.getCaseId(),
                defendantDetailsUpdated.getDefendantId(),
                defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(),
                dateOfBirth,
                defendantDetailsUpdated.getUpdatedDate(),
                defendantDetailsUpdated.getLegalEntityName()
        );
    }

    private void updateDefendant(final DefendantDetail defendantDetailEntity, final DefendantDetailsUpdated newData) {
        if (defendantDetailEntity.getPersonalDetails() == null) {
            defendantDetailEntity.setPersonalDetails(new PersonalDetails());
        }

        if (defendantDetailEntity.getLegalEntityDetails() == null) {
            defendantDetailEntity.setLegalEntityDetails(new LegalEntityDetails());
        }

        final PersonalDetails entity = defendantDetailEntity.getPersonalDetails();
        final LegalEntityDetails legalEntity = defendantDetailEntity.getLegalEntityDetails();
        ofNullable(newData.getFirstName()).ifPresent(entity::setFirstName);
        ofNullable(newData.getLastName()).ifPresent(entity::setLastName);
        ofNullable(newData.getLegalEntityName()).ifPresent(legalEntity::setLegalEntityName);
        ofNullable(newData.getDateOfBirth()).ifPresent(entity::setDateOfBirth);
        final Address address = newData.getAddress();

        ofNullable(newData.getPcqId()).ifPresent(defendantDetailEntity:: setPcqId);
        if (newData.isUpdateByOnlinePlea()) {
            ofNullable(newData.getNationalInsuranceNumber()).ifPresent(entity::setNationalInsuranceNumber);
            ofNullable(newData.getDriverNumber()).ifPresent(entity::setDriverNumber);
            ofNullable(newData.getDriverLicenceDetails()).ifPresent(entity::setDriverLicenceDetails);
        } else {
            ofNullable(newData.getGender()).ifPresent(entity::setGender);
            // Title can be explicitly set to null from sjp, no title comes from online plea
            entity.setTitle(newData.getTitle());
            entity.setNationalInsuranceNumber(newData.getNationalInsuranceNumber());
            entity.setDriverNumber(newData.getDriverNumber());
            entity.setDriverLicenceDetails(newData.getDriverLicenceDetails());
        }

        ofNullable(address).map(addressToAddressEntity::convert).ifPresent(defendantDetailEntity::setAddress);

        ofNullable(newData.getContactDetails()).map(contactDetailsToContactDetailsEntity::convert).ifPresent(defendantDetailEntity::setContactDetails);
        ofNullable(newData.getRegion()).ifPresent(defendantDetailEntity::setRegion);
    }
}
