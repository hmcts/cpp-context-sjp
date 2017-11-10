package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantDetailsTest {

    final String ADDRESS_1 = "14 Tottenham Court Road";
    final String ADDRESS_2 = "London";
    final String ADDRESS_3 = "England";
    final String ADDRESS_4 = "UK";
    final String POSTCODE = "W1T 1JY";
    final UUID defendantId = randomUUID();
    final UUID caseId = randomUUID();
    final UUID personId = randomUUID();
    final String title = "Mr";
    final String firstName = "test";
    final String lastName = "lastName";
    final String email = "email";
    final String gender = "gender";
    final String nationalInsuranceNumber = "nationalInsuranceNumber";
    final String homeNumber = "homeNumber";
    final String mobileNumber = "mobileNumber";
    final Address address = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POSTCODE);
    final LocalDate dateOfBirth = LocalDates.from("1980-07-15");

    private DefendantAggregate defendantAggregate;

    @Before
    public void setUp() {
        defendantAggregate = new DefendantAggregate();
    }

    @Test
    public void shouldCreateUpdateEvent() {
        Address address = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3,
                ADDRESS_4, POSTCODE);
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(personId, title, firstName, lastName,
                dateOfBirth, address);
        defendantAggregate.addPersonInfo(defendantId, caseId, personInfoDetails);

        Stream<Object> eventStream = defendantAggregate.updateDefendantDetails(caseId, defendantId,
                gender, nationalInsuranceNumber, email, homeNumber, mobileNumber, personInfoDetails);

        List<Object> events = asList(eventStream.toArray());

        assertThat("Has defendant Updated event", events,
                hasItem(isA(DefendantDetailsUpdated.class))
        );

        DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getFirstName(), is(firstName));
        assertThat(defendantDetailsUpdated.getLastName(), is(lastName));
        assertThat(defendantDetailsUpdated.getTitle(), is(title));

    }

    @Test
    public void shouldFailValidation() {
        Address address = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3,
                ADDRESS_4, POSTCODE);
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(personId, title, firstName, lastName,
                dateOfBirth, address);

        defendantAggregate.addPersonInfo(defendantId, caseId, personInfoDetails);

        PersonInfoDetails personInfoDetailsWithoutTitle = new PersonInfoDetails(personId, null, firstName, lastName,
                dateOfBirth, address);
        Stream<Object> eventStream = defendantAggregate.updateDefendantDetails(caseId, defendantId,
                gender, nationalInsuranceNumber, email, homeNumber, mobileNumber, personInfoDetailsWithoutTitle);

        List<Object> events = asList(eventStream.toArray());

        assertThat("has no defendant details Updated failed event", events,
                hasItem(isA(DefendantDetailsUpdateFailed.class)));
    }
}
