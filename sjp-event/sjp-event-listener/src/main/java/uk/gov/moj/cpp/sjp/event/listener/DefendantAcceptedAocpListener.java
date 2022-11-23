package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpOnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantAcceptedAocpListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @Inject
    private AocpOnlinePleaRepository.PersonDetailsOnlinePleaRepository aocpOnlinePleaRepository;

    @Inject
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private AddressToAddressEntity addressToAddressEntity;

    @Handles("sjp.events.defendant-accepted-aocp")
    public void handleDefendantAcceptedAocp(final JsonEnvelope envelope) {

        final DefendantAcceptedAocp defendantAcceptedAocp =
                jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantAcceptedAocp.class);

        final CaseDetail caseDetail = caseRepository.findBy(defendantAcceptedAocp.getCaseId());

        final AocpOnlinePlea aocpOnlinePlea = buildOnlinePlea(caseDetail.getDefendant(), defendantAcceptedAocp);
        aocpOnlinePleaRepository.save(aocpOnlinePlea);

        defendantAcceptedAocp.getOffences().forEach(offence->
            saveOnlinePleaDetail(offence, defendantAcceptedAocp));

        caseDetail.setDefendantAcceptedAocp(true);
        caseRepository.save(caseDetail);
    }

    private void saveOnlinePleaDetail(final Offence offence, final DefendantAcceptedAocp defendantAcceptedAocp){
        final OnlinePleaDetail onlinePleaDetail = new OnlinePleaDetail(offence.getId(), defendantAcceptedAocp.getCaseId(),
                defendantAcceptedAocp.getDefendantId(), offence.getPlea(), offence.getMitigation(), offence.getNotGuiltyBecause(), true);

        onlinePleaDetailRepository.save(onlinePleaDetail);
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private AocpOnlinePlea buildOnlinePlea(final DefendantDetail defendantDetail, final DefendantAcceptedAocp defendantAcceptedAocp) {
        final AocpOnlinePlea newAocpOnlinePlea = new AocpOnlinePlea(defendantDetail, defendantAcceptedAocp.getPleadDate(), UUID.randomUUID());
        final OnlinePleaPersonalDetails personalDetails = newAocpOnlinePlea.getPersonalDetails();
        final uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails newPersonalDetails = defendantAcceptedAocp.getPersonalDetails();
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
        newAocpOnlinePlea.setAocpAccepted(defendantAcceptedAocp.getAocpAccepted());
        return newAocpOnlinePlea;
    }
}
