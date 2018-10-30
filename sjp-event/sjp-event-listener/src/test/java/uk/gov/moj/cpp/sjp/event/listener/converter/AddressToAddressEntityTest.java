package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;

import org.junit.Test;

public class AddressToAddressEntityTest {

    private AddressToAddressEntity addressToAddressEntity = new AddressToAddressEntity();

    @Test
    public void shouldConvertAddressToAddressEntity() {
        uk.gov.moj.cpp.sjp.domain.Address inputAddress = new uk.gov.moj.cpp.sjp.domain.Address(
                "address1", "address2", "address3",
                "address4", "address5", "postcode");

        Address outputAddress = addressToAddressEntity.convert(inputAddress);

        Address expectedAddress = new Address(
                inputAddress.getAddress1(),
                inputAddress.getAddress2(),
                inputAddress.getAddress3(),
                inputAddress.getAddress4(),
                inputAddress.getAddress5(),
                inputAddress.getPostcode());

        assertTrue(reflectionEquals(outputAddress, expectedAddress));
    }

}