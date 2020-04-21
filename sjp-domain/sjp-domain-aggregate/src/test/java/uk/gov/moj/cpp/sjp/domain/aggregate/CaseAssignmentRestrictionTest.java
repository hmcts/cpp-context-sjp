package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAssignmentRestrictionTest {

    private static final String PROSECUTING_AUTHORITY = "TFL";
    private static final List<String> EXCLUDE = singletonList("1234");
    private static final List<String> INCLUDE_ONLY = singletonList("9876");
    private static final ZonedDateTime DATE_TIME_CREATED = now();

    @Test
    public void shouldUpdateCaseAssignmentRestriction() {
        final CaseAssignmentRestrictionAggregate aggregate = new CaseAssignmentRestrictionAggregate();
        final List<Object> events = aggregate
                .updateCaseAssignmentRestriction(PROSECUTING_AUTHORITY, INCLUDE_ONLY, EXCLUDE, DATE_TIME_CREATED.toString())
                .collect(toList());
        assertThat(events.size(), equalTo(1));
        final CaseAssignmentRestrictionAdded caseAssignmentRestrictionAdded = events
                .stream()
                .map(event -> (CaseAssignmentRestrictionAdded)event)
                .collect(toList())
                .get(0);
        assertThat(caseAssignmentRestrictionAdded.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY));
        assertThat(caseAssignmentRestrictionAdded.getExclude(), equalTo(EXCLUDE));
        assertThat(caseAssignmentRestrictionAdded.getIncludeOnly(), equalTo(INCLUDE_ONLY));
        assertThat(caseAssignmentRestrictionAdded.getDateTimeCreated(), equalTo(DATE_TIME_CREATED.toString()));
    }
}
