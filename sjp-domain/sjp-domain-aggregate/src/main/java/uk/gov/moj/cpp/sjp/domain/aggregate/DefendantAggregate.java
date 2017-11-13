package uk.gov.moj.cpp.sjp.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactNumber;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.PersonInfoAdded;
import uk.gov.moj.cpp.sjp.event.PersonInfoUpdated;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefendantAggregate implements Aggregate {

    private String title;
    private LocalDate dateOfBirth;
    private Address address;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantAggregate.class);

    private void validate(String title, LocalDate dateOfBirth, Address  address) {
        validateTitle(title);

        validateDateOfBirth(dateOfBirth);

        validateAddress(address);
    }

    private void validateAddress(final Address address) {
        if(this.address != null) {
            ensureFieldIsNotBlankIfWasDefined(this.address.getAddress1(), address.getAddress1(),
                    "street (address1) can not be blank as previous value is: " + this.address.getAddress1());

            ensureFieldIsNotBlankIfWasDefined(this.address.getAddress4(), address.getAddress4(),
                    "town (address4) can not be blank as previous value is: " + this.address.getAddress4());

            ensureFieldIsNotBlankIfWasDefined(this.address.getPostCode(), address.getPostCode(),
                    "postCode can not be blank as previous value is: " + this.address.getPostCode());
        }
    }

    private void ensureFieldIsNotBlankIfWasDefined(String oldValue, String newValue, String errorMessage) {
        if (StringUtils.isNotBlank(oldValue) && StringUtils.isBlank(newValue)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateDateOfBirth(final LocalDate dateOfBirth) {
        if (this.dateOfBirth != null && dateOfBirth == null) {
            throw new IllegalArgumentException(
                    "dob parameter can not be null");
        }
    }

    private void validateTitle(final String title) {
        if (StringUtils.isBlank(title) && StringUtils.isNotBlank(this.title)) {
            throw new IllegalArgumentException(String.format("title parameter can not be null as previous value is : %s", this.title));
        }
    }

    @SuppressWarnings("squid:S00107") //Proper fix requires proper remodelling / guidance
    public Stream<Object> updateDefendantDetails(UUID caseId, UUID defendantId, String gender,
                                                 String nationalInsuranceNumber,String email,
                                                 String homeNumber, String mobileNumber,
                                                 PersonInfoDetails personInfoDetails) {
        try {
            validate(personInfoDetails.getTitle(), personInfoDetails.getDateOfBirth(), personInfoDetails.getAddress());
        } catch(IllegalArgumentException | IllegalStateException e){
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId ,e);
            return apply(Stream.of(new DefendantDetailsUpdateFailed(caseId.toString(), defendantId.toString(), e.getMessage())));
        }

        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withPersonId(personInfoDetails.getPersonId())
                .withTitle(personInfoDetails.getTitle())
                .withFirstName(personInfoDetails.getFirstName())
                .withLastName(personInfoDetails.getLastName())
                .withDateOfBirth(personInfoDetails.getDateOfBirth())
                .withGender(gender)
                .withEmail(email)
                .withNationalInsuranceNumber(nationalInsuranceNumber)
                .withContactNumber(new ContactNumber(homeNumber, mobileNumber))
                .withAddress(personInfoDetails.getAddress()).build();
        return apply(Stream.of(defendantDetailsUpdated));
    }

    public Stream<Object> addPersonInfo(UUID id, UUID caseId, PersonInfoDetails personInfoDetails) {
        return apply(Stream.of(
                new PersonInfoAdded(id, caseId, personInfoDetails)
        ));
    }

    public Stream<Object> updatePersonInfo(final PersonInfoDetails personInfoDetails) {
        return apply(Stream.of(
                new PersonInfoUpdated(personInfoDetails)
        ));

    }

    @Override
    public Object apply(Object event) {
        return match(event).with(

                when(DefendantDetailsUpdated.class).apply(e -> {
                    dateOfBirth = e.getDateOfBirth();
                    address = e.getAddress();
                    title = e.getTitle();
                }),
                when(PersonInfoAdded.class).apply(e -> {
                    title = e.getPersonInfoDetails().getTitle();
                    dateOfBirth = e.getPersonInfoDetails().getDateOfBirth();
                    address = e.getPersonInfoDetails().getAddress();
                }),
                when(DefendantDetailsUpdateFailed.class).apply(e -> {
                    // no change in aggregate state
                }),
                when(PersonInfoUpdated.class).apply(e -> {
                    // no change in aggregate state
                })
        );
    }
}
