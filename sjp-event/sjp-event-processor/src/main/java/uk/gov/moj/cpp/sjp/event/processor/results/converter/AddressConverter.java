package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.Address;

import java.util.Optional;

import javax.json.JsonObject;

public class AddressConverter {
    private static final String ADDRESS1_KEY = "address1";
    private static final String ADDRESS2_KEY = "address2";
    private static final String ADDRESS3_KEY = "address3";
    private static final String ADDRESS4_KEY = "address4";
    private static final String ADDRESS5_KEY = "address5";
    private static final String POSTCODE_KEY = "postcode";

    private static final String WELSH_ADDRESS1_KEY = "welshAddress1";
    private static final String WELSH_ADDRESS2_KEY = "welshAddress2";
    private static final String WELSH_ADDRESS3_KEY = "welshAddress3";
    private static final String WELSH_ADDRESS4_KEY = "welshAddress4";
    private static final String WELSH_ADDRESS5_KEY = "welshAddress5";

    public Address convert(final Optional<JsonObject> addressOpt) {
        final Address.Builder addressBuilder = Address.address();
        if (addressOpt.isPresent()) {
            final JsonObject court = addressOpt.get();
            addressBuilder
                    .withAddress1(court.getString(ADDRESS1_KEY, null))//Mandatory
                    .withAddress2(court.getString(ADDRESS2_KEY, null))
                    .withAddress3(court.getString(ADDRESS3_KEY, null))
                    .withAddress4(court.getString(ADDRESS4_KEY, null))
                    .withAddress5(court.getString(ADDRESS5_KEY, null))
                    .withPostcode(court.getString(POSTCODE_KEY, null));

        }
        return addressBuilder.build();
    }

    public Address convertWelsh(final Optional<JsonObject> addressOpt) {
        final Address.Builder addressBuilder = Address.address();
        if (addressOpt.isPresent()) {
            final JsonObject court = addressOpt.get();
            if (court.containsKey(WELSH_ADDRESS1_KEY)) {
                return getWelshAddress(addressBuilder, court);
            }
        }
        return null;
    }

    private Address getWelshAddress(final Address.Builder addressBuilder, final JsonObject court) {
        addressBuilder
                .withAddress1(court.getString(WELSH_ADDRESS1_KEY, null))//Mandatory
                .withAddress2(court.getString(WELSH_ADDRESS2_KEY, null))
                .withAddress3(court.getString(WELSH_ADDRESS3_KEY, null))
                .withAddress4(court.getString(WELSH_ADDRESS4_KEY, null))
                .withAddress5(court.getString(WELSH_ADDRESS5_KEY, null))
                .withPostcode(court.getString(POSTCODE_KEY, null));
        return addressBuilder.build();
    }

    public Address getAddress(uk.gov.justice.json.schemas.domains.sjp.Address address) {
        final Address.Builder addressBuilder = Address.address();
        if (address != null) {
            return addressBuilder
                    .withAddress1(address.getAddress1())
                    .withAddress2(address.getAddress2())
                    .withAddress3(address.getAddress3())
                    .withAddress4(address.getAddress4())
                    .withAddress5(address.getAddress5())
                    .withPostcode(address.getPostcode())
                    .build();
        }
        return addressBuilder.build();
    }
}
