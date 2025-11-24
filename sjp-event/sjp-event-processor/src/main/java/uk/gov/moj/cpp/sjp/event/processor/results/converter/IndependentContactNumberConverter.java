package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;

public class IndependentContactNumberConverter {
    public ContactNumber getContact(final ContactDetails contactDetails) {
        return ContactNumber.contactNumber()
                .withWork(contactDetails.getBusiness())
                .withMobile(contactDetails.getMobile())
                .withHome(contactDetails.getHome())
                .withPrimaryEmail(contactDetails.getEmail())
                .withSecondaryEmail(contactDetails.getEmail2())
                .build();
    }
}
