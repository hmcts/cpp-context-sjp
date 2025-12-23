package uk.gov.moj.cpp.sjp.command.api.service;


import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;

import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AddressServiceTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(asList("W1T 1JY", "w1t 1jy", "W1T1JY"), "W1T 1JY"),
                Arguments.of(asList("EC1A 1BB", "EC1A1BB", "eC1a 1Bb", "eC1a1Bb", " eC1a1Bb "), "EC1A 1BB"),
                Arguments.of(asList(""), null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(List<String> postcodeReceivedList, String normalizedPostcode) {
        postcodeReceivedList.forEach(postcodeReceived -> {
            final JsonObjectBuilder addressBuilder = createObjectBuilder()
                    .add("address1", "line1")
                    .add("address2", "line2")
                    .add("address3", "line3")
                    .add("address4", "line4")
                    .add("address5", "line5");

            if (isNotEmpty(postcodeReceived)) {
                addressBuilder.add("postcode", postcodeReceived);
            }

            final JsonObject addressObject = addressBuilder.build();
            final JsonObject addressObjectWithNormalizedPostcode = normalizePostcodeInAddress(addressObject);
            assertThat(addressObjectWithNormalizedPostcode.getString("address1"), is((addressObject.getString("address1"))));
            assertThat(addressObjectWithNormalizedPostcode.getString("address2"), is((addressObject.getString("address2"))));
            assertThat(addressObjectWithNormalizedPostcode.getString("address3"), is((addressObject.getString("address3"))));
            assertThat(addressObjectWithNormalizedPostcode.getString("address4"), is((addressObject.getString("address4"))));
            assertThat(addressObjectWithNormalizedPostcode.getString("address5"), is((addressObject.getString("address5"))));

            if (nonNull(normalizedPostcode)) {
                assertThat(addressObjectWithNormalizedPostcode.getString("postcode"), is(normalizedPostcode));
            } else {
                assertThat(addressObjectWithNormalizedPostcode.containsKey("postcode"), is(FALSE));
            }
        });
    }

    @Test
    public void testNullAddress() {
        final JsonObject addressObjectWithNormalized = normalizePostcodeInAddress(null);
        assertThat(addressObjectWithNormalized, is(nullValue()));
    }
}
