package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;

import java.util.Optional;

public class ContactDetailsToContactDetailsEntity implements Converter<uk.gov.moj.cpp.sjp.domain.ContactDetails, ContactDetails> {

    @Override
    public ContactDetails convert(uk.gov.moj.cpp.sjp.domain.ContactDetails contactDetails) {
        return Optional.ofNullable(contactDetails)
                .map(c -> new ContactDetails(c.getEmail(), c.getHome(), c.getMobile()))
                .orElse(null);
    }

}