package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;

import org.junit.Test;

public class ContactDetailsToContactDetailsEntityTest {

    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity = new ContactDetailsToContactDetailsEntity();

    @Test
    public void shouldConvertContactDetailsToContactDetailsEntity() {
        final uk.gov.moj.cpp.sjp.domain.ContactDetails inputContactDetails = new uk.gov.moj.cpp.sjp.domain.ContactDetails(
                "home","mobile", "business", "email", "email2"
        );

        final ContactDetails outputContactDetails = contactDetailsToContactDetailsEntity.convert(inputContactDetails);

        final ContactDetails expectedContactDetails = new ContactDetails(
                inputContactDetails.getEmail(),
                inputContactDetails.getHome(),
                inputContactDetails.getMobile()
        );

        assertTrue(reflectionEquals(outputContactDetails, expectedContactDetails));
    }

}