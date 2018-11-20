package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourts;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class CaseListedInCriminalCourtsTest extends CaseAggregateBaseTest {

    @Test
    public void shouldCreateCaseListedInCriminalCourtsEvent() {

        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        assertThat(events.get(0), instanceOf(CaseListedInCriminalCourts.class));
        final CaseListedInCriminalCourts caseListedInCriminalCourts = (CaseListedInCriminalCourts) events.get(0);
        assertThat(caseListedInCriminalCourts.getCaseId(), equalTo(caseId));
    }
}
