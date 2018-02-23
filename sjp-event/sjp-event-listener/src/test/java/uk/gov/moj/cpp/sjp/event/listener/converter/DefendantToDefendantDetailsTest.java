package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantToDefendantDetailsTest {

    private static final String NATIONAL_INSURANCE_NUMBER = UUID.randomUUID().toString();

    private AddressToAddressEntity addressToAddressEntityConverter = new AddressToAddressEntity();

    private OffenceToOffenceDetail offenceToOffenceDetailConverter = new OffenceToOffenceDetail();

    private DefendantToDefendantDetails converterUnderTest = new DefendantToDefendantDetails(NATIONAL_INSURANCE_NUMBER);

    @Test
    public void shouldConvertDefendantToDefendantDetails() {
        Defendant inputDefendant = new Defendant(UUID.randomUUID(), "title", "firstName", "lastName",
                LocalDate.of(1980, 1,1), "M",
                new uk.gov.moj.cpp.sjp.domain.Address("l1", "l2", "l3", "l4", "p"),
        3, asList(buildOffence(), buildOffence()));

        DefendantDetail outputDefendant = converterUnderTest.convert(inputDefendant);

        DefendantDetail expectedDefendant = buildExpectedDefendant(inputDefendant);
        assertTrue(reflectionEquals(outputDefendant, expectedDefendant, "offences", "personalDetails"));
        assertTrue(reflectionEquals(outputDefendant.getOffences(), expectedDefendant.getOffences()));

        assertTrue(reflectionEquals(outputDefendant.getPersonalDetails(), expectedDefendant.getPersonalDetails(), "address", "contactDetails"));
        assertTrue(reflectionEquals(outputDefendant.getPersonalDetails().getAddress(), expectedDefendant.getPersonalDetails().getAddress()));
        assertTrue(reflectionEquals(outputDefendant.getPersonalDetails().getContactDetails(), expectedDefendant.getPersonalDetails().getContactDetails()));

        outputDefendant.getOffences().forEach(o -> assertThat(o.getDefendantDetail(), equalTo(outputDefendant)));
    }

    private DefendantDetail buildExpectedDefendant(Defendant defendant) {
        PersonalDetails personalDetails = new PersonalDetails(
                defendant.getTitle(),
                defendant.getFirstName(),
                defendant.getLastName(),
                defendant.getDateOfBirth(),
                defendant.getGender(),
                NATIONAL_INSURANCE_NUMBER,
                addressToAddressEntityConverter.convert(defendant.getAddress()),
                new ContactDetails()
        );

        return new DefendantDetail(
                defendant.getId(),
                personalDetails,
                defendant.getOffences().stream().map(offenceToOffenceDetailConverter::convert).collect(toSet()),
                defendant.getNumPreviousConvictions());
    }

    private static Offence buildOffence() {
        return new Offence(UUID.randomUUID(),1, "libraOffenceCode", LocalDate.of(2011, 1, 1),
                2, LocalDate.of(2009, 1, 1), "offenceWording",
                "prosecutionFacts", "witnessStatement", BigDecimal.ONE);
    }

}