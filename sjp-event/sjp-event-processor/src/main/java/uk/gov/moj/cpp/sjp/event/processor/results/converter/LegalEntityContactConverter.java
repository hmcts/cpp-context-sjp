package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;

public class LegalEntityContactConverter {
    public ContactNumber getContact(final ContactDetails contactDetails) {
        if (isNull(contactDetails)) {
            return null;
        }
        return ContactNumber.contactNumber()
                .withWork(contactDetails.getBusiness())
                .withMobile(contactDetails.getMobile())
                .withPrimaryEmail(contactDetails.getEmail())
                .withSecondaryEmail(contactDetails.getEmail2())
                .withHome(contactDetails.getHome())
                .build();
    }
}
