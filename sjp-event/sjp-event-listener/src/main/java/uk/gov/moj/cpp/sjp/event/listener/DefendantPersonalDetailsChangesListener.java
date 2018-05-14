package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

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
        final CaseDetail caseDetail = caseRepository.findBy(defendantPersonalNameUpdated.getCaseId());
        caseDetail.getDefendant().getPersonalDetails().setNameChanged(Boolean.TRUE);
        caseRepository.save(caseDetail);
    }

    @Handles("sjp.events.defendant-address-updated")
    @Transactional
    public void defendantAddressUpdated(final JsonEnvelope envelope) {
        final DefendantAddressUpdated defendantAddressUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantAddressUpdated.class);
        final CaseDetail caseDetail = caseRepository.findBy(defendantAddressUpdated.getCaseId());
        caseDetail.getDefendant().getPersonalDetails().setAddressChanged(Boolean.TRUE);
        caseRepository.save(caseDetail);
    }

    @Handles("sjp.events.defendant-date-of-birth-updated")
    @Transactional
    public void defendantDateOfBirthUpdated(final JsonEnvelope envelope) {
        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DefendantDateOfBirthUpdated.class);
        final CaseDetail caseDetail = caseRepository.findBy(defendantDateOfBirthUpdated.getCaseId());
        caseDetail.getDefendant().getPersonalDetails().setDobChanged(Boolean.TRUE);
        caseRepository.save(caseDetail);
    }
}