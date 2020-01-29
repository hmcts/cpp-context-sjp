package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantDetailsTest extends CaseAggregateBaseTest {

    private static final String ADDRESS_1_UPDATED = "15 Tottenham Court Road";
    private static final String FIRST_NAME_UPDATED = "Mark";
    private static final LocalDate DATE_OF_BIRTH_UPDATED = LocalDate.of(1980, 6, 15);

    private Person person;
    private UUID userId;

    @Before
    @Override
    public void setUp() {
        userId = randomUUID();
        super.setUp();

        this.person = aCase.getDefendant();

        // ASSUMING that new fields does not match with old fields
        assertThat(person.getAddress().getAddress1(), not(equalTo(ADDRESS_1_UPDATED)));
        assertThat(person.getFirstName(), not(equalTo(FIRST_NAME_UPDATED)));
        assertThat(person.getDateOfBirth(), not(equalTo(DATE_OF_BIRTH_UPDATED)));
    }

    @Test
    public void shouldCreateUpdateEvents() {
        ZonedDateTime updateDate = clock.now();

        List<Object> events = caseAggregate.updateDefendantDetails(userId, caseId, defendantId, person, updateDate)
                .collect(toList());

        assertThat(events, contains(instanceOf(DefendantDetailsUpdated.class)));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getCaseId(), equalTo(caseId));
        assertThat(defendantDetailsUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(defendantDetailsUpdated.getTitle(), equalTo(person.getTitle()));
        assertThat(defendantDetailsUpdated.getFirstName(), equalTo(person.getFirstName()));
        assertThat(defendantDetailsUpdated.getLastName(), equalTo(person.getLastName()));
        assertThat(defendantDetailsUpdated.getDateOfBirth(), equalTo(person.getDateOfBirth()));
        assertThat(defendantDetailsUpdated.getGender(), equalTo(person.getGender()));
        assertThat(defendantDetailsUpdated.getNationalInsuranceNumber(), equalTo(person.getNationalInsuranceNumber()));
        assertThat(defendantDetailsUpdated.getAddress(), equalTo(person.getAddress()));
        assertThat(defendantDetailsUpdated.getContactDetails(), equalTo(person.getContactDetails()));
        assertThat(defendantDetailsUpdated.isUpdateByOnlinePlea(), equalTo(false));
        assertThat(defendantDetailsUpdated.getUpdatedDate(), equalTo(clock.now()));

        final Person updatedPerson = new Person(
                person.getTitle(),
                FIRST_NAME_UPDATED,
                person.getLastName(),
                DATE_OF_BIRTH_UPDATED,
                person.getGender(),
                person.getNationalInsuranceNumber(),
                new Address(
                        ADDRESS_1_UPDATED,
                        person.getAddress().getAddress2(),
                        person.getAddress().getAddress3(),
                        person.getAddress().getAddress4(),
                        person.getAddress().getAddress5(),
                        person.getAddress().getPostcode()
                ),
                person.getContactDetails(),
                person.getRegion());

        events = caseAggregate.updateDefendantDetails(userId, caseId, defendantId, updatedPerson, clock.now())
                .collect(toList());

        assertThat(events, contains(
                instanceOf(DefendantDateOfBirthUpdated.class),
                instanceOf(DefendantAddressUpdated.class),
                instanceOf(DefendantPersonalNameUpdated.class),
                instanceOf(DefendantDetailsUpdated.class)));

        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) events.get(0);
        assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), is(person.getDateOfBirth()));
        assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), is(DATE_OF_BIRTH_UPDATED));
        assertThat(defendantDateOfBirthUpdated.getUpdatedAt(), is(updateDate));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(1);
        assertThat(defendantAddressUpdated.getOldAddress().getAddress1(), is(person.getAddress().getAddress1()));
        assertThat(defendantAddressUpdated.getNewAddress().getAddress1(), is(ADDRESS_1_UPDATED));
        assertThat(defendantAddressUpdated.getUpdatedAt(), is(updateDate));

        final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) events.get(2);
        assertThat(defendantPersonalNameUpdated.getOldPersonalName().getFirstName(), is(person.getFirstName()));
        assertThat(defendantPersonalNameUpdated.getNewPersonalName().getFirstName(), is(FIRST_NAME_UPDATED));
        assertThat(defendantPersonalNameUpdated.getUpdatedAt(), is(updateDate));
    }

    @Test
    public void shouldAllowValidationForEmptyTitle() {
        final Person personWithoutTitle = new Person(
                null,
                person.getFirstName(),
                person.getLastName(),
                person.getDateOfBirth(),
                person.getGender(),
                person.getNationalInsuranceNumber(),
                person.getAddress(),
                person.getContactDetails(),
                person.getRegion());

        final List<Object> events = caseAggregate.updateDefendantDetails(userId, caseId, defendantId, personWithoutTitle, clock.now())
                .collect(toList());

        assertThat("has defendant details updated event", events,
                contains(
                        instanceOf(DefendantPersonalNameUpdated.class),
                        instanceOf(DefendantDetailsUpdated.class)
                )
        );
    }
}
