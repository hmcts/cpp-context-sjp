package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CaseListedInCriminalCourtsTest extends CaseAggregateBaseTest {
    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateCaseListingInCriminalCourtsEvent() {
        caseReceivedEvent = collectFirstEvent(caseAggregate.receiveCase(buildCaseReceived(), clock.now()), CaseReceived.class);

        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final ZonedDateTime hearingTime = now();
        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId, hearingCourtName, hearingTime);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        assertThat(events.get(0), instanceOf(CaseListedInCriminalCourts.class));
        final CaseListedInCriminalCourts caseListedInCriminalCourts = (CaseListedInCriminalCourts) events.get(0);
        assertThat(caseListedInCriminalCourts.getCaseId(), equalTo(caseId));
        assertThat(caseListedInCriminalCourts.getHearingCourtName(), equalTo(hearingCourtName));
        assertThat(caseListedInCriminalCourts.getHearingTime(), equalTo(hearingTime));
    }

    @Test
    public void shouldNotCreateCaseListingInCriminalCourtsEvent() {
        caseAggregate = new CaseAggregate();

        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final ZonedDateTime hearingTime = now();

        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId, hearingCourtName, hearingTime);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(0));
    }
}
