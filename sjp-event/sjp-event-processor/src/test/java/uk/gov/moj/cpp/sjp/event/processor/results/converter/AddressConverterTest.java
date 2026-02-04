package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS1_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS2_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS3_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS4_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS5_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_4;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_5;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.POSTCODE_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.POST_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS1_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS2_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS3_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS4_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS5_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_4;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WELSH_ADDRESS_LINE_5;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class AddressConverterTest {
    @InjectMocks
    AddressConverter addressConverter;


    @Test
    public void shouldConvertAddressWithMandatoryField() {
        final AddressConverter addressConverter = new AddressConverter();
        final uk.gov.justice.core.courts.Address coreDomainAddress
                = addressConverter.getAddress(getAddressWithMandatoryField());
        assertThat(coreDomainAddress.getAddress1(), is(ADDRESS_LINE_1));
        assertThat(coreDomainAddress.getAddress2(), is(nullValue()));
        assertThat(coreDomainAddress.getAddress3(), is(nullValue()));
        assertThat(coreDomainAddress.getAddress4(), is(nullValue()));
        assertThat(coreDomainAddress.getAddress5(), is(nullValue()));
        assertThat(coreDomainAddress.getPostcode(), is(nullValue()));
    }

    @Test
    public void shouldConvertAddressFromJsonWithMandatoryField() {
        final AddressConverter addressConverter = new AddressConverter();
        final uk.gov.justice.core.courts.Address coreDomainAddress
                = addressConverter.convert(getJsonAddress());
        assertThat(coreDomainAddress.getAddress1(), is(ADDRESS_LINE_1));
        assertThat(coreDomainAddress.getAddress2(), is(ADDRESS_LINE_2));
        assertThat(coreDomainAddress.getAddress3(), is(ADDRESS_LINE_3));
        assertThat(coreDomainAddress.getAddress4(), is(ADDRESS_LINE_4));
        assertThat(coreDomainAddress.getAddress5(), is(ADDRESS_LINE_5));
        assertThat(coreDomainAddress.getPostcode(), is(POST_CODE));
    }

    @Test
    public void shouldConvertWelshAddressFromJsonWithMandatoryField() {
        final AddressConverter addressConverter = new AddressConverter();
        final uk.gov.justice.core.courts.Address coreDomainAddress
                = addressConverter.convertWelsh(getJsonWelshAddress());
        assertThat(coreDomainAddress.getAddress1(), is(WELSH_ADDRESS_LINE_1));
        assertThat(coreDomainAddress.getAddress2(), is(WELSH_ADDRESS_LINE_2));
        assertThat(coreDomainAddress.getAddress3(), is(WELSH_ADDRESS_LINE_3));
        assertThat(coreDomainAddress.getAddress4(), is(WELSH_ADDRESS_LINE_4));
        assertThat(coreDomainAddress.getAddress5(), is(WELSH_ADDRESS_LINE_5));

    }

    @Test
    public void shouldConvertAddressWithOptionalField() {
        final AddressConverter addressConverter = new AddressConverter();
        final uk.gov.justice.core.courts.Address coreDomainAddress
                = addressConverter.getAddress(getAddressWithOptionalField());
        assertThat(coreDomainAddress.getAddress1(), is(ADDRESS_LINE_1));
        assertThat(coreDomainAddress.getAddress2(), is(ADDRESS_LINE_2));
        assertThat(coreDomainAddress.getAddress3(), is(ADDRESS_LINE_3));
        assertThat(coreDomainAddress.getAddress4(), is(ADDRESS_LINE_4));
        assertThat(coreDomainAddress.getAddress5(), is(ADDRESS_LINE_5));
        assertThat(coreDomainAddress.getPostcode(), is(POST_CODE));
    }


    public static uk.gov.justice.json.schemas.domains.sjp.Address getAddressWithMandatoryField() {
        return new uk.gov.justice.json.schemas.domains.sjp.Address(ADDRESS_LINE_1, null, null,
                null, null, null);
    }

    public static uk.gov.justice.json.schemas.domains.sjp.Address getAddressWithOptionalField() {
        return new uk.gov.justice.json.schemas.domains.sjp.Address(ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                ADDRESS_LINE_4, ADDRESS_LINE_5, POST_CODE);
    }

    public static Optional<JsonObject> getJsonAddress() {
        return Optional.of(createObjectBuilder()
                .add(ADDRESS1_KEY, ADDRESS_LINE_1)
                .add(ADDRESS2_KEY, ADDRESS_LINE_2)
                .add(ADDRESS3_KEY, ADDRESS_LINE_3)
                .add(ADDRESS4_KEY, ADDRESS_LINE_4)
                .add(ADDRESS5_KEY, ADDRESS_LINE_5)
                .add(POSTCODE_KEY, POST_CODE)
                .build());
    }

    public static Optional<JsonObject> getJsonWelshAddress() {
        return Optional.of(createObjectBuilder()
                .add(WELSH_ADDRESS1_KEY, WELSH_ADDRESS_LINE_1)
                .add(WELSH_ADDRESS2_KEY, WELSH_ADDRESS_LINE_2)
                .add(WELSH_ADDRESS3_KEY, WELSH_ADDRESS_LINE_3)
                .add(WELSH_ADDRESS4_KEY, WELSH_ADDRESS_LINE_4)
                .add(WELSH_ADDRESS5_KEY, WELSH_ADDRESS_LINE_5)

                .build());
    }
}
