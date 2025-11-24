package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityFinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaLegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class OnlinePleaReceivedListener {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @Inject
    private OnlinePleaRepository.LegalEntityDetailsOnlinePleaRepository legalEntityDetailsOnlinePleaRepository;

    @Inject
    private AddressToAddressEntity addressToAddressEntity;

    private static final String CASE_ID_PROPERTY = "caseId";

    @Handles("sjp.events.online-plea-received")
    @Transactional
    public void onlinePleaReceived(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final OnlinePleaReceived onlinePleaReceived = jsonObjectToObjectConverter.convert(payload, OnlinePleaReceived.class);
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));

        CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setOnlinePleaReceived(true);
        if(Boolean.TRUE.equals(caseDetail.getDefendantAcceptedAocp())){
            caseDetail.setDefendantAcceptedAocp(false);
        }
        caseRepository.save(caseDetail);

        final ZonedDateTime pleaDateTime = event.metadata().createdAt().orElse(now());
        final OnlinePlea onlinePlea = buildOnlinePlea(caseDetail.getDefendant(), onlinePleaReceived, pleaDateTime);
        if (nonNull(onlinePlea.getLegalEntityDetails()) && nonNull(onlinePlea.getLegalEntityDetails().getLegalEntityName())) {
            legalEntityDetailsOnlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
        else {
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
    }

    private OnlinePlea buildOnlinePlea(final DefendantDetail defendantDetail, final OnlinePleaReceived newData, final ZonedDateTime pleaDateTime) {
        final OnlinePlea newOnlinePlea = new OnlinePlea(defendantDetail, newData.getDisabilityNeeds(), pleaDateTime);
        final LegalEntityDefendant newLegalEntityDefendant = newData.getLegalEntityDefendant();
        if (nonNull(newLegalEntityDefendant) && nonNull(newLegalEntityDefendant.getAddress())) {
            final uk.gov.moj.cpp.sjp.domain.Address legalEntityAddress = newLegalEntityDefendant.getAddress();
            final Address  address = new Address(legalEntityAddress.getAddress1(),
                    legalEntityAddress.getAddress2(),
                    legalEntityAddress.getAddress3(),
                    legalEntityAddress.getAddress4(),
                    legalEntityAddress.getAddress5(),
                    legalEntityAddress.getPostcode());
            final LegalEntityFinancialMeans legalEntityFinancialMeans = new LegalEntityFinancialMeans(newData.getLegalEntityFinancialMeans().getTradingMoreThan12Months(), newData.getLegalEntityFinancialMeans().getNumberOfEmployees(), newData.getLegalEntityFinancialMeans().getGrossTurnover(), newData.getLegalEntityFinancialMeans().getNetTurnover());

            final OnlinePleaLegalEntityDetails legalEntity = new OnlinePleaLegalEntityDetails(newLegalEntityDefendant.getName(), newLegalEntityDefendant.getPosition(), legalEntityFinancialMeans, address, newLegalEntityDefendant.getContactDetails().getHome(), newLegalEntityDefendant.getContactDetails().getMobile(), newLegalEntityDefendant.getContactDetails().getEmail());
            newOnlinePlea.setLegalEntityDetails(legalEntity);
        }

        else {
            final OnlinePleaPersonalDetails personalDetails = newOnlinePlea.getPersonalDetails();
            final uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails newPersonalDetails = newData.getPersonalDetails();
            if (newPersonalDetails !=null) {
                ofNullable(newPersonalDetails.getFirstName()).ifPresent(personalDetails::setFirstName);
                ofNullable(newPersonalDetails.getLastName()).ifPresent(personalDetails::setLastName);
                ofNullable(newPersonalDetails.getNationalInsuranceNumber()).ifPresent(personalDetails::setNationalInsuranceNumber);
                ofNullable(newPersonalDetails.getDateOfBirth()).ifPresent(personalDetails::setDateOfBirth);
                ofNullable(newPersonalDetails.getAddress()).map(addressToAddressEntity::convert).ifPresent(personalDetails::setAddress);
                ofNullable(newPersonalDetails.getContactDetails()).ifPresent(contactDetails -> {
                    personalDetails.setHomeTelephone(contactDetails.getHome());
                    personalDetails.setMobile(contactDetails.getMobile());
                    personalDetails.setEmail(contactDetails.getEmail());
                });
                ofNullable(newPersonalDetails.getDriverNumber()).ifPresent(personalDetails::setDriverNumber);
                ofNullable(newPersonalDetails.getDriverLicenceDetails()).ifPresent(personalDetails::setDriverLicenceDetails);
            }
        }
        return newOnlinePlea;
    }
}
