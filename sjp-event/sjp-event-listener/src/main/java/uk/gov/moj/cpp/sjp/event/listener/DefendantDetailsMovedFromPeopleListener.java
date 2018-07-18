package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsMovedFromPeople;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DefendantDetailsMovedFromPeopleListener {

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

    @Handles("sjp.events.defendant-details-moved-from-people")
    @Transactional
    public void defendantDetailsUpdated(final JsonEnvelope envelope) {


        DefendantDetailsMovedFromPeople defendantDetailsMovedFromPeople = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantDetailsMovedFromPeople.class);

        CaseDetail caseDetail = caseRepository.findBy(defendantDetailsMovedFromPeople.getCaseId());

        PersonalDetails personalDetails = createPersonalDetails(defendantDetailsMovedFromPeople);
        caseDetail.getDefendant().setPersonalDetails(personalDetails);
        caseRepository.save(caseDetail);

        caseSearchResultService.onDefendantDetailsUpdated(
                caseDetail.getId(),
                caseDetail.getDefendant().getPersonalDetails().getFirstName(),
                caseDetail.getDefendant().getPersonalDetails().getLastName(),
                caseDetail.getDefendant().getPersonalDetails().getDateOfBirth(),
                getEventCreationDateTime(envelope, caseDetail));
    }

    private ZonedDateTime getEventCreationDateTime(final JsonEnvelope envelope, final CaseDetail caseDetail) {
        return envelope.metadata().createdAt().orElse(
                caseDetail.getDateTimeCreated()
        );
    }

    private PersonalDetails createPersonalDetails(DefendantDetailsMovedFromPeople event) {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName(event.getFirstName());
        personalDetails.setLastName(event.getLastName());
        personalDetails.setGender(event.getGender());
        personalDetails.setTitle(event.getTitle());
        personalDetails.setNationalInsuranceNumber(event.getNationalInsuranceNumber());
        personalDetails.setDateOfBirth(event.getDateOfBirth());
        personalDetails.setAddress(addressToAddressEntity.convert(event.getAddress()));
        personalDetails.setContactDetails(contactDetailsToContactDetailsEntity.convert(event.getContactNumber()));

        return personalDetails;
    }
}
