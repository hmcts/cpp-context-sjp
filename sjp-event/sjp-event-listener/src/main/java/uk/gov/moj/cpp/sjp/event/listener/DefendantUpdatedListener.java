package uk.gov.moj.cpp.sjp.event.listener;


import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DefendantUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

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

        LocalDate dateOfBirth = defendantDetailsUpdated.getDateOfBirth();

        //this listener updates two tables for the case where the event is fired via plead-online command
        if (defendantDetailsUpdated.isUpdateByOnlinePlea()) {
            final OnlinePlea onlinePlea = buildOnlinePlea(caseDetail.getDefendant(), defendantDetailsUpdated);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
            if (isNull(dateOfBirth)) { // not changed by online plea
                dateOfBirth = caseDetail.getDefendant().getPersonalDetails().getDateOfBirth();
            }
        }

        caseSearchResultService.onDefendantDetailsUpdated(
                defendantDetailsUpdated.getCaseId(),
                defendantDetailsUpdated.getDefendantId(),
                defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(),
                dateOfBirth,
                defendantDetailsUpdated.getUpdatedDate()
        );


    }

    private OnlinePlea buildOnlinePlea(final DefendantDetail defendantDetail, final DefendantDetailsUpdated newData) {
        final OnlinePlea newOnlinePlea = new OnlinePlea(defendantDetail, newData.getUpdatedDate());
        final OnlinePleaPersonalDetails personalDetails = newOnlinePlea.getPersonalDetails();
        ofNullable(newData.getFirstName()).ifPresent(personalDetails::setFirstName);
        ofNullable(newData.getLastName()).ifPresent(personalDetails::setLastName);
        ofNullable(newData.getNationalInsuranceNumber()).ifPresent(personalDetails::setNationalInsuranceNumber);
        ofNullable(newData.getDateOfBirth()).ifPresent(personalDetails::setDateOfBirth);
        ofNullable(newData.getAddress()).map(addressToAddressEntity::convert).ifPresent(personalDetails::setAddress);
        ofNullable(newData.getContactDetails()).ifPresent(contactDetails -> {
            personalDetails.setHomeTelephone(contactDetails.getHome());
            personalDetails.setMobile(contactDetails.getMobile());
            personalDetails.setEmail(contactDetails.getEmail());
        });
        return newOnlinePlea;
    }

    private void updateDefendant(final DefendantDetail defendantDetailEntity, final DefendantDetailsUpdated newData) {
        if (defendantDetailEntity.getPersonalDetails() == null) {
            defendantDetailEntity.setPersonalDetails(new PersonalDetails());
        }

        final PersonalDetails entity = defendantDetailEntity.getPersonalDetails();
        ofNullable(newData.getFirstName()).ifPresent(entity::setFirstName);
        ofNullable(newData.getLastName()).ifPresent(entity::setLastName);
        if (newData.isUpdateByOnlinePlea()) {
            ofNullable(newData.getNationalInsuranceNumber()).ifPresent(entity::setNationalInsuranceNumber);
            ofNullable(newData.getDateOfBirth()).ifPresent(entity::setDateOfBirth);
        } else {
            ofNullable(newData.getGender()).ifPresent(entity::setGender);
            // Title can be explicitly set to null from sjp, no title comes from online plea
            entity.setTitle(newData.getTitle());
            entity.setDateOfBirth(newData.getDateOfBirth());
            entity.setNationalInsuranceNumber(newData.getNationalInsuranceNumber());
        }
        ofNullable(newData.getAddress()).map(addressToAddressEntity::convert).ifPresent(entity::setAddress);
        ofNullable(newData.getContactDetails()).map(contactDetailsToContactDetailsEntity::convert).ifPresent(entity::setContactDetails);
        ofNullable(newData.getRegion()).ifPresent(entity::setRegion);
    }
}
