package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "England";
    private static final String ADDRESS_4 = "UK";
    private static final String POSTCODE = "W1T 1JY";
    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final UUID personId = randomUUID();
    private static final String title = "Mr";
    private static final String firstName = "test";
    private static final String lastName = "lastName";
    private static final String email = "email";
    private static final String gender = "gender";
    private static final String nationalInsuranceNumber = "nationalInsuranceNumber";
    private static final String homeNumber = "homeNumber";
    private static final String mobileNumber = "mobileNumber";
    private static final Address address = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POSTCODE);
    private static final LocalDate dateOfBirth = LocalDate.of(1980, 7, 15);

    private CaseAggregate caseAggregate;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateUpdateEvent() {
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(personId, title, firstName, lastName,
                dateOfBirth, address);
        caseAggregate.addPersonInfo(defendantId, caseId, personInfoDetails);

        Stream<Object> eventStream = caseAggregate.updateDefendantDetails(caseId, defendantId,
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
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(personId, title, firstName, lastName,
                dateOfBirth, address);

        caseAggregate.addPersonInfo(defendantId, caseId, personInfoDetails);

        PersonInfoDetails personInfoDetailsWithoutTitle = new PersonInfoDetails(personId, null, firstName, lastName,
                dateOfBirth, address);
        Stream<Object> eventStream = caseAggregate.updateDefendantDetails(caseId, defendantId,
                gender, nationalInsuranceNumber, email, homeNumber, mobileNumber, personInfoDetailsWithoutTitle);

        List<Object> events = asList(eventStream.toArray());

        assertThat("has no defendant details Updated failed event", events,
                hasItem(isA(DefendantDetailsUpdateFailed.class)));
    }
}
