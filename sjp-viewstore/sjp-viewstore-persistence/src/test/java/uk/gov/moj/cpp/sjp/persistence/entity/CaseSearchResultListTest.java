package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class CaseSearchResultListTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String FIRST_NAME = "Foo";
    private static final String LAST_NAME = "Bar";
    private static final String LEGAL_ENTITY_NAME = "Legal entity name";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.now();
    private static final ZonedDateTime DATE_ADDED = ZonedDateTime.now();

    @Test
    public void hasNameChangedShouldConsiderNullValuesAsNameNotChanged() {
        final CaseSearchResult caseSearchResults = new CaseSearchResult();
        caseSearchResults.setFirstName("Foo");
        caseSearchResults.setLastName("Bar");
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(singletonList(caseSearchResults));

        assertThat(caseSearchResultList.hasNameChanged(null, null,null), is(false));
        assertThat(caseSearchResultList.hasNameChanged(null, "Bar",null), is(false));
        assertThat(caseSearchResultList.hasNameChanged("Foo", null,null), is(false));

        caseSearchResults.setFirstName(null);
        caseSearchResults.setLastName(null);
        assertThat(caseSearchResultList.hasNameChanged(null, null,null), is(false));
    }

    @Test
    public void hasNameChangedShouldReturnTrueWhenNameChanged() {
        final CaseSearchResult caseSearchResults = new CaseSearchResult();
        caseSearchResults.setFirstName("Foo");
        caseSearchResults.setLastName("Bar");
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(singletonList(caseSearchResults));

        assertThat(caseSearchResultList.hasNameChanged("Will", "Smith",null), is(true));
        assertThat(caseSearchResultList.hasNameChanged("Will", caseSearchResults.getLastName(),null), is(true));
        assertThat(caseSearchResultList.hasNameChanged(caseSearchResults.getFirstName(), "Smith",null), is(true));

        caseSearchResults.setFirstName(null);
        caseSearchResults.setLastName(null);
        assertThat(caseSearchResultList.hasNameChanged("Will", "Smith",null), is(true));
    }

    @Test
    public void hasNameChangedShouldIgnoreCase() {
        final CaseSearchResult caseSearchResults = new CaseSearchResult();
        caseSearchResults.setFirstName("Foo");
        caseSearchResults.setLastName("Bar");
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(singletonList(caseSearchResults));

        assertThat(caseSearchResultList.hasNameChanged("fOO", "bAR",null), is(false));
        assertThat(caseSearchResultList.hasNameChanged("fOO", caseSearchResults.getLastName(),null), is(false));
        assertThat(caseSearchResultList.hasNameChanged(caseSearchResults.getFirstName(), "bAR",null), is(false));
    }

    @Test
    public void setNameSetsAllFields() {
        final List<CaseSearchResult> actual = new ArrayList<>();
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(actual);

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, DATE_ADDED, LEGAL_ENTITY_NAME);

        assertThat(actual, hasSize(1));
        assertThat(actual.get(0).getCaseId(), equalTo(CASE_ID));
        assertThat(actual.get(0).getDefendantId(), equalTo(DEFENDANT_ID));
        assertThat(actual.get(0).getWithdrawalRequestedDate(), nullValue());
        assertThat(actual.get(0).isAssigned(), equalTo(false));
        assertThat(actual.get(0).getFirstName(), equalTo(FIRST_NAME));
        assertThat(actual.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(actual.get(0).getCurrentFirstName(), equalTo(FIRST_NAME));
        assertThat(actual.get(0).getCurrentLastName(), equalTo(LAST_NAME));
        assertThat(actual.get(0).getDateOfBirth(), equalTo(DATE_OF_BIRTH));
        assertThat(actual.get(0).getDateAdded(), equalTo(DATE_ADDED));
        assertThat(actual.get(0).getLegalEntityName(), equalTo(LEGAL_ENTITY_NAME));
        assertThat(actual.get(0).isDeprecated(), equalTo(false));
    }

    @Test
    public void setNameShouldUpdateNameWithPreviousRecordWhenNewNameIsNull() {
        final List<CaseSearchResult> actual = new ArrayList<>();
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(actual);

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, DATE_ADDED,null);
        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, "new_first_name", null, DATE_OF_BIRTH, DATE_ADDED,null);
        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, null, "new_last_name", DATE_OF_BIRTH, DATE_ADDED,null);
        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, null, null, DATE_OF_BIRTH, DATE_ADDED,null);

        assertThat(actual.get(0), hasNames(FIRST_NAME, LAST_NAME));
        assertThat(actual.get(0), hasCurrentNames("new_first_name", "new_last_name"));

        assertThat(actual.get(1), hasNames("new_first_name", LAST_NAME));
        assertThat(actual.get(1), hasCurrentNames("new_first_name", "new_last_name"));

        assertThat(actual.get(2), hasNames("new_first_name", "new_last_name"));
        assertThat(actual.get(2), hasCurrentNames("new_first_name", "new_last_name"));

        assertThat(actual.get(3), hasNames("new_first_name", "new_last_name"));
        assertThat(actual.get(3), hasCurrentNames("new_first_name", "new_last_name"));
    }

    @Test
    public void setNameUpdatesCurrentName() {
        final List<CaseSearchResult> actual = new ArrayList<>();
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(actual);

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, null, null, DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0), hasNames(null, null));
        assertThat(actual.get(0), hasCurrentNames(null, null));

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, "new_first_name", "new_last_name", DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0), hasCurrentNames("new_first_name", "new_last_name"));
        assertThat(actual.get(1), hasCurrentNames("new_first_name", "new_last_name"));

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, "Will", "Smith", DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0), hasCurrentNames("Will", "Smith"));
        assertThat(actual.get(1), hasCurrentNames("Will", "Smith"));
        assertThat(actual.get(2), hasCurrentNames("Will", "Smith"));
    }

    @Test
    public void setNameShouldDeprecatePreviousRecords() {
        final List<CaseSearchResult> actual = new ArrayList<>();
        final CaseSearchResultList caseSearchResultList = new CaseSearchResultList(actual);

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0).isDeprecated(), equalTo(false));

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0).isDeprecated(), equalTo(true));
        assertThat(actual.get(1).isDeprecated(), equalTo(false));

        caseSearchResultList.setName(CASE_ID, DEFENDANT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, DATE_ADDED,null);
        assertThat(actual.get(0).isDeprecated(), equalTo(true));
        assertThat(actual.get(1).isDeprecated(), equalTo(true));
        assertThat(actual.get(2).isDeprecated(), equalTo(false));
    }

    private Matcher<CaseSearchResult> hasNames(final String firstName, final String lastName) {
        return allOf(
                hasProperty("firstName", equalTo(firstName)),
                hasProperty("lastName", equalTo(lastName))
        );
    }

    private Matcher<CaseSearchResult> hasCurrentNames(final String currentFirstName, final String currentLastName) {
        return allOf(
                hasProperty("currentFirstName", equalTo(currentFirstName)),
                hasProperty("currentLastName", equalTo(currentLastName))
        );
    }
}
