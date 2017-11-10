package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNotReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CaseReopenedTest extends CaseAggregateBaseTest {

    private final static String REOPENED_DATE = "2017-01-01";
    private final static String LIBRA_CASE_NUMBER = "LIBRA12345";
    private final static String REASON = "Reason";

    private CaseReopenDetails caseReopenDetails;

    @Before
    public void setupCaseReopenDetails() {
        caseReopenDetails = new CaseReopenDetails(
                sjpCaseAggregate.getCaseId().toString(),
                LocalDates.from(REOPENED_DATE),
                LIBRA_CASE_NUMBER,
                REASON
        );
    }

    @Test
    public void shouldMarkCaseReopened() {
        final List<Object> events = reopenCase();

        assertEquals(1, events.size());

        final Object event = events.get(0);
        assertEquals(CaseReopened.class, event.getClass());
        assertCaseReopenedDetails(((CaseReopened) event).getCaseReopenDetails());
    }

    @Test
    public void shouldNotMarkCaseReopenedWhenCaseNotCreatedBefore() {
        final CaseAggregate aggregateWithNoHistory = new CaseAggregate();

        final Stream<Object> eventStream = aggregateWithNoHistory.markCaseReopened(caseReopenDetails);

        final List<Object> events = asList(eventStream.toArray());
        assertEquals(1, events.size());
        assertEquals(CaseNotFound.class, events.get(0).getClass());
    }

    @Test
    public void shouldNotMarkCaseReopenedWhenCaseAlreadyReopened() {
        reopenCase();

        final List<Object> events = sjpCaseAggregate.markCaseReopened(caseReopenDetails).collect(Collectors.toList());

        assertEquals(1, events.size());
        assertEquals(CaseAlreadyReopened.class, events.get(0).getClass());
    }

    @Test
    public void shouldUpdateReopenedCase() {
        reopenCase();

        final List<Object> events = sjpCaseAggregate.updateCaseReopened(caseReopenDetails).collect(Collectors.toList());
        assertEquals(1, events.size());

        final Object event = events.get(0);
        assertEquals(CaseReopenedUpdated.class, event.getClass());
        assertCaseReopenedDetails(((CaseReopenedUpdated) event).getCaseReopenDetails());
    }

    @Test
    public void shouldNotUpdateCaseReopenedWhenCaseNotCreatedBefore() {
        final CaseAggregate aggregateWithNoHistory = new CaseAggregate();

        final Stream<Object> eventStream = aggregateWithNoHistory.updateCaseReopened(caseReopenDetails);

        final List<Object> events = asList(eventStream.toArray());

        assertEquals(1, events.size());
        assertEquals(CaseNotFound.class, events.get(0).getClass());
    }

    @Test
    public void shouldNotUpdateCaseReopenedWhenCaseNotReopenedBefore() {
        final List<Object> events = sjpCaseAggregate.updateCaseReopened(caseReopenDetails).collect(Collectors.toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0), is(instanceOf(CaseNotReopened.class)));
    }

    private List<Object> reopenCase() {
        return sjpCaseAggregate.markCaseReopened(caseReopenDetails).collect(Collectors.toList());
    }

    private void assertCaseReopenedDetails(CaseReopenDetails caseReopenDetails) {
        assertEquals(sjpCase.getId().toString(), caseReopenDetails.getCaseId());
        assertEquals(LIBRA_CASE_NUMBER, caseReopenDetails.getLibraCaseNumber());
        assertEquals(REOPENED_DATE, caseReopenDetails.getReopenedDate().toString());
        assertEquals(REASON, caseReopenDetails.getReason());
    }
}

