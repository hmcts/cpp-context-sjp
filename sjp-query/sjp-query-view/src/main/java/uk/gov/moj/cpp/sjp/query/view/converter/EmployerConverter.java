package uk.gov.moj.cpp.sjp.query.view.converter;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.Employer;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class EmployerConverter {

    public uk.gov.moj.cpp.sjp.domain.Employer convertToEmployer(final Employer entity) {

        return new uk.gov.moj.cpp.sjp.domain.Employer(entity.getDefendantId(), entity.getName(),
                entity.getEmployeeReference(), entity.getPhone(), getAddress(entity).orElse(null));
    }

    private Optional<Address> getAddress(final Employer entity) {
        final boolean nonEmptyAddress = Stream.of(
                entity.getAddress1(),
                entity.getAddress2(),
                entity.getAddress3(),
                entity.getAddress4(),
                entity.getPostcode()).anyMatch(Objects::nonNull);
        return nonEmptyAddress ?
                Optional.of(new Address(
                        entity.getAddress1(),
                        entity.getAddress2(),
                        entity.getAddress3(),
                        entity.getAddress4(),
                        entity.getPostcode()))
                : Optional.empty();
    }
}
