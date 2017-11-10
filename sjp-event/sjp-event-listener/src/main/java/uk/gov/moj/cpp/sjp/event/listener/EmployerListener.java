package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class EmployerListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private EmployerRepository employerRepository;

    @Transactional
    @Handles("structure.events.employer-updated")
    public void updateEmployer(final JsonEnvelope event) {
        final EmployerUpdated employerUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), EmployerUpdated.class);
        final Employer employer = new Employer(employerUpdated.getDefendantId());
        employer.setName(employerUpdated.getName());
        employer.setEmployeeReference(employerUpdated.getEmployeeReference());
        employer.setPhone(employerUpdated.getPhone());
        if (employerUpdated.getAddress() != null) {
            employer.setAddress1(employerUpdated.getAddress().getAddress1());
            employer.setAddress2(employerUpdated.getAddress().getAddress2());
            employer.setAddress3(employerUpdated.getAddress().getAddress3());
            employer.setAddress4(employerUpdated.getAddress().getAddress4());
            employer.setPostCode(employerUpdated.getAddress().getPostCode());
        }

        employerRepository.save(employer);
    }
}
