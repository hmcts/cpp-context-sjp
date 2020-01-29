package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import javax.inject.Inject;

public class PersonToPersonalDetailsEntity<P extends Person> implements Converter<P, PersonalDetails> {

    @Inject
    private AddressToAddressEntity addressToAddressEntityConverter;

    @Inject
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Override
    public PersonalDetails convert(P person) {
        return new PersonalDetails(
                person.getTitle(),
                person.getFirstName(),
                person.getLastName(),
                person.getDateOfBirth(),
                person.getGender(),
                person.getNationalInsuranceNumber(),
                addressToAddressEntityConverter.convert(person.getAddress()),
                contactDetailsToContactDetailsEntity.convert(person.getContactDetails()),
                person.getRegion());
    }

}