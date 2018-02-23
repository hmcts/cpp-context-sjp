package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.stream.Collectors.toSet;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

public class DefendantToDefendantDetails implements Converter<Defendant, DefendantDetail> {

    private AddressToAddressEntity addressToAddressEntityConverter = new AddressToAddressEntity();
    private OffenceToOffenceDetail offenceToOffenceDetailConverter = new OffenceToOffenceDetail();

    private final String nationalInsuranceNumber;

    public DefendantToDefendantDetails(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    @Override
    public DefendantDetail convert(Defendant defendant) {
        PersonalDetails personalDetails = new PersonalDetails(
                defendant.getTitle(),
                defendant.getFirstName(),
                defendant.getLastName(),
                defendant.getDateOfBirth(),
                defendant.getGender(),
                nationalInsuranceNumber,
                addressToAddressEntityConverter.convert(defendant.getAddress()),
                new ContactDetails()
        );

        return new DefendantDetail(
                defendant.getId(),
                personalDetails,
                defendant.getOffences().stream().map(offenceToOffenceDetailConverter::convert).collect(toSet()),
                defendant.getNumPreviousConvictions());
    }

}