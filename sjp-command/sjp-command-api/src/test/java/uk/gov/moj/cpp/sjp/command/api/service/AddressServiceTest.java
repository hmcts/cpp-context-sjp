package uk.gov.moj.cpp.sjp.command.api.service;


import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static liquibase.util.StringUtils.isNotEmpty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;

import java.util.Collection;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AddressServiceTest {

    @Parameterized.Parameter
    public List<String> postcodeReceivedList;

    @Parameterized.Parameter(1)
    public String normalizedPostcode;

    @Parameterized.Parameters(name = "Postcode {0} is normalized to {1}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {asList("W1T 1JY", "w1t 1jy", "W1T1JY"), "W1T 1JY"},
                {asList("EC1A 1BB", "EC1A1BB", "eC1a 1Bb", "eC1a1Bb", " eC1a1Bb "), "EC1A 1BB"},
                {asList(""), null},
        });
    }

    @Test
    public void test() {
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
}
