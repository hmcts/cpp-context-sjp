package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.moj.cpp.sjp.query.view.converter.EmployerConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class EmployerService {

    @Inject
    private EmployerRepository employerRepository;

    @Inject
    private EmployerConverter employerConverter;

    public Optional<uk.gov.moj.cpp.sjp.domain.Employer> getEmployer(final UUID defendantId) {

        final Employer employer = employerRepository.findBy(defendantId);
        return Optional.ofNullable(employer).map(employerConverter::convertToEmployer);

    }
}
