package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;

public class AddressToAddressEntity implements Converter<uk.gov.moj.cpp.sjp.domain.Address, Address> {

    @Override
    public Address convert(uk.gov.moj.cpp.sjp.domain.Address address) {
        return new Address(
                address.getAddress1(),
                address.getAddress2(),
                address.getAddress3(),
                address.getAddress4(),
                address.getPostcode());
    }

}