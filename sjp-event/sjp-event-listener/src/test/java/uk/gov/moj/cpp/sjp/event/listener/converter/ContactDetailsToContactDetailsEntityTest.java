package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;

import org.junit.Test;

public class ContactDetailsToContactDetailsEntityTest {

    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity = new ContactDetailsToContactDetailsEntity();

    @Test
    public void shouldConvertContactDetailsToContactDetailsEntity() {
        uk.gov.moj.cpp.sjp.domain.ContactDetails inputContactDetails = new uk.gov.moj.cpp.sjp.domain.ContactDetails(
                "home","mobile", "email"
        );

        ContactDetails outputContactDetails = contactDetailsToContactDetailsEntity.convert(inputContactDetails);

        ContactDetails expectedContactDetails = new ContactDetails(
                inputContactDetails.getEmail(),
                inputContactDetails.getHome(),
                inputContactDetails.getMobile()
        );

        assertTrue(reflectionEquals(outputContactDetails, expectedContactDetails));
    }

}