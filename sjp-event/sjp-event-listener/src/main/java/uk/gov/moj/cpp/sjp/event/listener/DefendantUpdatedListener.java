package uk.gov.moj.cpp.sjp.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

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

    @Handles("sjp.events.defendant-national-insurance-number-updated")
    @Transactional
    public void defendantNationalInsuranceNumberUpdated(final JsonEnvelope envelope) {

        DefendantsNationalInsuranceNumberUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantsNationalInsuranceNumberUpdated.class);
        DefendantDetail defendantDetail = caseRepository.findCaseDefendant(event.getCaseId());

        if (!defendantDetail.getId().equals(event.getDefendantId())) {
            throw new IllegalArgumentException("Mismatch of defendant IDs: " + defendantDetail.getId() + " != " + event.getDefendantId() + " for case: " + defendantDetail.getCaseDetail().getId());
        }

        defendantDetail.getPersonalDetails().setNationalInsuranceNumber(event.getNationalInsuranceNumber());
    }

    @Handles("sjp.events.defendant-details-updated")
    @Transactional
    public void defendantDetailsUpdated(final JsonEnvelope envelope) {

        DefendantDetailsUpdated defendantDetailsUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantDetailsUpdated.class);

        CaseDetail caseDetail = caseRepository.findBy(defendantDetailsUpdated.getCaseId());

        if (caseDetail == null) {
            throw new IllegalArgumentException("Unable to update defendant's details of a case that does not exist: " + defendantDetailsUpdated.getCaseId());
        }

        updateDefendant(caseDetail.getDefendant(), defendantDetailsUpdated);
        caseRepository.save(caseDetail);

        caseSearchResultService.onDefendantDetailsUpdated(
                defendantDetailsUpdated.getCaseId(),
                defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(),
                defendantDetailsUpdated.getDateOfBirth()
        );

        //this listener updates two tables for the case where the event is fired via plead-online command
        if (defendantDetailsUpdated.isUpdateByOnlinePlea()) {
            final OnlinePlea onlinePlea = new OnlinePlea(defendantDetailsUpdated);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
    }

    private void updateDefendant(DefendantDetail defendantDetailEntity, DefendantDetailsUpdated newData) {
        if (defendantDetailEntity.getPersonalDetails() == null) {
            defendantDetailEntity.setPersonalDetails(new PersonalDetails());
        }

        PersonalDetails entity = defendantDetailEntity.getPersonalDetails();

        entity.setFirstName(newData.getFirstName());
        entity.setLastName(newData.getLastName());
        if (newData.isUpdateByOnlinePlea()) {
            if (newData.getNationalInsuranceNumber() != null) {
                entity.setNationalInsuranceNumber(newData.getNationalInsuranceNumber());
            }
        } else {
            entity.setGender(newData.getGender());
            entity.setTitle(newData.getTitle());
            entity.setNationalInsuranceNumber(newData.getNationalInsuranceNumber());
        }
        entity.setDateOfBirth(newData.getDateOfBirth());

        entity.setAddress(new Address(
                newData.getAddress().getAddress1(),
                newData.getAddress().getAddress2(),
                newData.getAddress().getAddress3(),
                newData.getAddress().getAddress4(),
                newData.getAddress().getPostcode()
        ));

        entity.setContactDetails(new ContactDetails(
                newData.getContactDetails().getEmail(),
                newData.getContactDetails().getHome(),
                newData.getContactDetails().getMobile()
        ));
    }
}
