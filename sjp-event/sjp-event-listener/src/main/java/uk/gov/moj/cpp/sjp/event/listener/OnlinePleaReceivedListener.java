package uk.gov.moj.cpp.sjp.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class OnlinePleaReceivedListener {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @Inject
    private AddressToAddressEntity addressToAddressEntity;

    private static final String CASE_ID_PROPERTY = "caseId";

    @Handles("sjp.events.online-plea-received")
    @Transactional
    public void onlinePleaReceived(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final PleadOnline pleadOnline = jsonObjectToObjectConverter.convert(payload, PleadOnline.class);
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));

        CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setOnlinePleaReceived(true);
        caseRepository.save(caseDetail);

        final ZonedDateTime pleaDateTime = event.metadata().createdAt().orElse(now());
        final OnlinePlea onlinePlea = buildOnlinePlea(caseDetail.getDefendant(), pleadOnline, pleaDateTime);
        onlinePleaRepository.saveOnlinePlea(onlinePlea);
    }

    private OnlinePlea buildOnlinePlea(final DefendantDetail defendantDetail, final PleadOnline newData, final ZonedDateTime pleaDateTime) {
        final OnlinePlea newOnlinePlea = new OnlinePlea(defendantDetail, pleaDateTime);
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
        }
        return newOnlinePlea;
    }
}
