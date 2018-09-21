package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;

import java.util.Optional;

public class AddressToAddressEntity implements Converter<uk.gov.moj.cpp.sjp.domain.Address, Address> {

    @Override
    public Address convert(final uk.gov.moj.cpp.sjp.domain.Address address) {
        return Optional.ofNullable(address)
                .map(a -> new Address(a.getAddress1(), a.getAddress2(), a.getAddress3(), a.getAddress4(), a.getPostcode()))
                .orElse(null);
    }

}