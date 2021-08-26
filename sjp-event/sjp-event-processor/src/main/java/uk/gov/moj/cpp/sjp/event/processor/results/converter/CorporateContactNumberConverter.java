package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.results.CorporateDefendant;

public class CorporateContactNumberConverter {
    //No Mandatory Field
    public ContactNumber getContact(final CorporateDefendant corporateDefendant) {
        return ContactNumber.contactNumber()
                .withWork(corporateDefendant.getTelephoneNumberBusiness1())
                .withMobile(corporateDefendant.getTelephoneNumberBusiness2())
                .withPrimaryEmail(corporateDefendant.getEmailAddress1())
                .withSecondaryEmail(corporateDefendant.getEmailAddress2())
                .build();
    }
}
