package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantDetailsTest {

    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_1_UPDATED = "15 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "Surrey";
    private static final String ADDRESS_4 = "England";
    private static final String ADDRESS_5 = "United Kingdom";
    private static final String POSTCODE = "W1T 1JY";
    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final String title = "Mr";
    private static final String firstName = "test";
    private static final String firstNameUpdated = "tester";
    private static final String lastName = "lastName";
    private static final String email = "email1@aaa.bbb";
    private static final String email2 = "email2@aaa.bbb";
    private static final String gender = "gender";
    private static final String nationalInsuranceNumber = "nationalInsuranceNumber";
    private static final String homeNumber = "123123777";
    private static final String mobileNumber = "456456888";
    private static final String businessNumber = "789789999";
    private static final Address address = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, ADDRESS_5, POSTCODE);
    private static final Address addressUpdated = new Address(ADDRESS_1_UPDATED, ADDRESS_2, ADDRESS_3, ADDRESS_4, ADDRESS_5, POSTCODE);
    private static final ContactDetails contactDetails = new ContactDetails(homeNumber, mobileNumber, businessNumber, email, email2);
    private static final LocalDate dateOfBirth = LocalDate.of(1980, 7, 15);
    private static final LocalDate dateOfBirthUpdated = LocalDate.of(1980, 6, 15);
    private static final Clock clock = new UtcClock();

    private CaseAggregate caseAggregate;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateUpdateEvents() {
        final Person person = new Person(title, firstName, lastName,
                dateOfBirth, gender, nationalInsuranceNumber, address, contactDetails);
        caseAggregate.receiveCase(
                CaseBuilder.aDefaultSjpCase().withDefendant(new Defendant(
                        defendantId,
                        person.getTitle(),
                        person.getFirstName(),
                        person.getLastName(),
                        person.getDateOfBirth(),
                        person.getGender(),
                        person.getNationalInsuranceNumber(),
                        person.getAddress(),
                        person.getContactDetails(),
                        0,
                        emptyList()
                )).build(),
                clock.now()
        );

        Stream<Object> eventStream = caseAggregate.updateDefendantDetails(caseId, defendantId, person, clock.now());

        List<Object> events = asList(eventStream.toArray());

        assertThat("Has defendant Updated event", events,
                hasItem(isA(DefendantDetailsUpdated.class))
        );

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getFirstName(), is(firstName));
        assertThat(defendantDetailsUpdated.getLastName(), is(lastName));
        assertThat(defendantDetailsUpdated.getTitle(), is(title));

        final Person updatedPerson = new Person(title, firstNameUpdated, lastName,
                dateOfBirthUpdated, gender, nationalInsuranceNumber, addressUpdated, contactDetails);

        eventStream = caseAggregate.updateDefendantDetails(caseId, defendantId, updatedPerson, clock.now());

        events = asList(eventStream.toArray());

        assertThat("Has defendant details updated events", events.size(),
                is(4)
        );

        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) events.get(0);
        assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), is(dateOfBirth));
        assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), is(dateOfBirthUpdated));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(1);
        assertThat(defendantAddressUpdated.getOldAddress().getAddress1(), is(ADDRESS_1));
        assertThat(defendantAddressUpdated.getNewAddress().getAddress1(), is(ADDRESS_1_UPDATED));

        final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) events.get(2);
        assertThat(defendantPersonalNameUpdated.getOldPersonalName().getFirstName(), is(firstName));
        assertThat(defendantPersonalNameUpdated.getNewPersonalName().getFirstName(), is(firstNameUpdated));
    }

    @Test
    public void shouldFailValidation() {
        final Person personInfoDetails = new Person(title, firstName, lastName, dateOfBirth, gender,
                nationalInsuranceNumber, address, contactDetails);

        caseAggregate.receiveCase(
                new Case(
                        UUID.randomUUID(),
                        "URN",
                        "enterpriseId",
                        ProsecutingAuthority.TFL,
                        BigDecimal.TEN,
                        LocalDate.now(),
                        new Defendant(
                                defendantId,
                                personInfoDetails.getTitle(),
                                personInfoDetails.getFirstName(),
                                personInfoDetails.getLastName(),
                                personInfoDetails.getDateOfBirth(),
                                personInfoDetails.getGender(),
                                personInfoDetails.getNationalInsuranceNumber(),
                                personInfoDetails.getAddress(),
                                personInfoDetails.getContactDetails(),
                                0,
                                Lists.newArrayList()
                        )
                ),
                clock.now()
        );
        final Person personInfoDetailsWithoutTitle = new Person(null, firstName, lastName,
                dateOfBirth, gender, nationalInsuranceNumber, address, contactDetails);
        final Stream<Object> eventStream = caseAggregate.updateDefendantDetails(caseId, defendantId, personInfoDetailsWithoutTitle, clock.now());

        final List<Object> events = asList(eventStream.toArray());

        assertThat("has no defendant details Updated failed event", events,
                hasItem(isA(DefendantDetailsUpdateFailed.class)));
    }
}
