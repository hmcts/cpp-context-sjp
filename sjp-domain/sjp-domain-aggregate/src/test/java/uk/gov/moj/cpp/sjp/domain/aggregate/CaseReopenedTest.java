package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
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
        super.setUp();
        caseReopenDetails = new CaseReopenDetails(
                aCase.getId(),
                LocalDates.from(REOPENED_DATE),
                LIBRA_CASE_NUMBER,
                REASON
        );
    }

    @Test
    public void shouldMarkCaseReopened() {
        final List<Object> events = reopenCase();
        assertThat(events, hasSize(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(CaseReopened.class));
        assertCaseReopenedDetails(((CaseReopened) event).getCaseReopenDetails());
    }

    @Test
    public void shouldNotMarkCaseReopenedWhenCaseNotCreatedBefore() {
        final CaseAggregate aggregateWithNoHistory = new CaseAggregate();

        final Stream<Object> eventStream = aggregateWithNoHistory.markCaseReopened(caseReopenDetails);

        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseNotFound.class));
    }

    @Test
    public void shouldNotMarkCaseReopenedWhenCaseAlreadyReopened() {
        reopenCase();

        final List<Object> events = caseAggregate.markCaseReopened(caseReopenDetails).collect(Collectors.toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseAlreadyReopened.class));
    }

    @Test
    public void shouldUpdateReopenedCase() {
        reopenCase();

        final List<Object> events = caseAggregate.updateCaseReopened(caseReopenDetails).collect(Collectors.toList());
        assertThat(events, hasSize(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(CaseReopenedUpdated.class));
        assertCaseReopenedDetails(((CaseReopenedUpdated) event).getCaseReopenDetails());
    }

    @Test
    public void shouldNotUpdateCaseReopenedWhenCaseNotCreatedBefore() {
        final CaseAggregate aggregateWithNoHistory = new CaseAggregate();

        final Stream<Object> eventStream = aggregateWithNoHistory.updateCaseReopened(caseReopenDetails);

        final List<Object> events = asList(eventStream.toArray());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseNotFound.class));
    }

    @Test
    public void shouldNotUpdateCaseReopenedWhenCaseNotReopenedBefore() {
        final List<Object> events = caseAggregate.updateCaseReopened(caseReopenDetails).collect(Collectors.toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseNotReopened.class));
    }

    @Test
    public void shouldNotUpdateCaseWhenCaseIdNotValid() {
        final List<Object> events = caseAggregate.updateCaseReopened(new CaseReopenDetails(null, null, null, null)).collect(Collectors.toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseNotFound.class));
    }

    private List<Object> reopenCase() {
        return caseAggregate.markCaseReopened(caseReopenDetails).collect(Collectors.toList());
    }

    private void assertCaseReopenedDetails(CaseReopenDetails caseReopenDetails) {
        assertEquals(aCase.getId(), caseReopenDetails.getCaseId());
        assertEquals(LIBRA_CASE_NUMBER, caseReopenDetails.getLibraCaseNumber());
        assertEquals(REOPENED_DATE, caseReopenDetails.getReopenedDate().toString());
        assertEquals(REASON, caseReopenDetails.getReason());
    }
}

